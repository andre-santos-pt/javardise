package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.createSequence

class MethodWidget(parent: Composite, val dec: CallableDeclaration<*>, style: Int = SWT.NONE) :
    MemberWidget<CallableDeclaration<*>>(parent, dec, style = style), SequenceContainer {

    var typeId: Id? = null
    override val name: Id
    override var body: SequenceWidget? = null

    val bodyModel: BlockStmt? =
        if (dec is MethodDeclaration) if(dec.body.isPresent) dec.body.get() else null
        else (dec as ConstructorDeclaration).body

    val paramsWidget: ParamListWidget

    override val closingBracket: TokenWidget

    private val observers = mutableListOf<(Node?, Any?) -> Unit>()

    fun addObserver(action: (Node?, Any?) -> Unit) {
        observers.add(action)
    }

    val focusListener = { event: Event ->
        if ((event.widget as Control).isChild(this@MethodWidget)) {
            val w = (event.widget as Control).findAncestor<NodeWidget<*>>()
            observers.forEach {
                var n = w?.node as? Node
                if (n is ExpressionStmt)
                    n = n.expression
                it(n, event.widget.data)
            }
        }
    }

    fun getChildOnFocus() : Node? {
        val onFocus = Display.getDefault().focusControl
        return if(onFocus.isChild(this)) {
            val w = onFocus.findAncestor<NodeWidget<*>>()
            var n = w?.node as? Node
            if (n is ExpressionStmt)
                n = n.expression
            n
        }
        else null
    }

    init {
        if (node.isMethodDeclaration) {
            typeId = SimpleTypeWidget(firstRow, (node as MethodDeclaration).type) { it.asString() }
            typeId!!.addFocusLostAction(::isValidType) {
                node.modifyCommand(node.typeAsString, it, node::setType)
            }
        }

        name = SimpleNameWidget(firstRow, node.name) { it.asString() }

        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(node.nameAsString, it, node::setName)
        }

        node.observeProperty<Type>(ObservableProperty.TYPE) {
            typeId?.set(it?.asString())
        }
        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it?.asString())
        }

        if (node.isConstructorDeclaration) {
            name.setReadOnly()
            name.setToolTip("Constructor name is not editable. Renaming the class modifies constructors accordingly.")
            // BUG problem with MVC
//            (node.parentNode.get() as TypeDeclaration<*>)
//                .observeProperty<SimpleName>(ObservableProperty.NAME) {
//                    name.set((it as SimpleName).asString())
//                    (node as ConstructorDeclaration).name = it
//                }
        }
        FixedToken(firstRow, "(")
        paramsWidget = ParamListWidget(firstRow, node.parameters)
        FixedToken(firstRow, ")")

        if(bodyModel != null) {
            body = createSequence(column, bodyModel)
            TokenWidget(firstRow, "{").addInsert(null, body!!, true)
            closingBracket = TokenWidget(column, "}")
        }
        else
            closingBracket = TokenWidget(firstRow, ";")

        Display.getDefault().addFilter(SWT.FocusIn, focusListener)
    }

    override fun dispose() {
        Display.getDefault().removeFilter(SWT.FocusIn, focusListener)
        super.dispose()
    }

    inner class ParamListWidget(parent: Composite, val parameters: NodeList<Parameter>) :
        Composite(parent, SWT.NONE) {
        var insert: TextWidget? = null

        init {
            layout = ROW_LAYOUT_H_SHRINK
            font = parent.font

            if(parameters.isEmpty())
                createInsert()

            addParams()

            parameters.register(object : ListAddRemoveObserver<Parameter>() {
                override fun elementAdd(list: NodeList<Parameter>, index: Int, node: Parameter) {
                    val p = ParamWidget(this@ParamListWidget, index, node)
                    if (index == 0 && list.isEmpty()) {
                        //ParamWidget(this@ParamListWidget, index, node)
                    } else if (index == list.size) {
                        val c = FixedToken(this@ParamListWidget, ",")
                        c.moveAbove(p)
                    } else {
                        val n = children.find { it is ParamWidget && it.node == list[index] }
                        n?.let {
                            p.moveAbove(n)
                            val c = FixedToken(this@ParamListWidget, ",")
                            c.moveAbove(n)
                        }
                    }
                    insert?.delete()
                    p.setFocusOnCreation(list.isEmpty())
                    p.requestLayout()
                }

                override fun elementRemove(list: NodeList<Parameter>, index: Int, node: Parameter) {
                    val i = children.indexOfFirst { it is ParamWidget && it.node == node }
                    if (i != -1) {
                        children[i].dispose()

                        // comma
                        if (i == 0 && list.size > 1)
                            children[i].dispose()
                        else if (i != 0)
                            children[i - 1].dispose()
                    }
                    if (parameters.size == 1) {
                        createInsert()
                        insert?.setFocus()
                    }
                    else if(index == 0) {
                        children[0].setFocus()
                    }
                    else {
                        val prev = children.indexOfFirst { it is ParamWidget && it.node == list[index-1] }
                        (children[prev] as ParamWidget).setFocusOnCreation(false)
                    }
                    requestLayout()


                }

                override fun elementReplace(
                    list: NodeList<Parameter>,
                    index: Int,
                    old: Parameter,
                    new: Parameter
                ) {
                    TODO("parameter replacement")
                }
            })
        }


        override fun dispose() {
            Display.getDefault().removeFilter(SWT.FocusIn, focusListener)
            super.dispose()
        }

        private fun createInsert() {
            val newInsert = TextWidget.create(this, " ") { c, s ->
                c.toString().matches(TYPE_CHARS)
            }
            newInsert.addKeyEvent(SWT.SPACE, precondition = { isValidType(it) }) {
                parameters.addCommand(node, Parameter(StaticJavaParser.parseType(newInsert.text), "parameter"))
            }
            if(bodyModel != null)
                newInsert.addKeyEvent(SWT.CR) {
                    this@MethodWidget.body!!.insertBeginning()
                }
            newInsert.addFocusLostAction {
                newInsert.clear(" ")
            }
            insert = newInsert
        }


        private fun addParams() {
            parameters.forEachIndexed { index, parameter ->
                if (index != 0)
                    FixedToken(this, ",")

                ParamWidget(this, index, parameter)
            }
        }

        inner class ParamWidget(parent: Composite, val index: Int, override val node: Parameter) :
        Composite(parent, SWT.NONE), NodeWidget<Parameter> {
            val type: Id
            val name: Id

            init {
                layout = ROW_LAYOUT_H_SHRINK
                font = parent.font
                type = SimpleTypeWidget(this, node.type) { it.asString() }
                type.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    parameters.removeCommand(this@MethodWidget.node, node) // TODO BUG Index -1 out of bounds for length 1
                }
                type.addFocusLostAction(::isValidType) {
                    node.modifyCommand(node.typeAsString, it, node::setType)
                }

                name = SimpleNameWidget(this, node.name) { it.asString() }
                name.addKeyEvent(',') {
                    parameters.addCommand(this@MethodWidget.node, Parameter(StaticJavaParser.parseType("type"), SimpleName("parameter")))
                }
                name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    parameters.removeCommand(this@MethodWidget.node, node)
                }
                if(bodyModel != null)
                    name.addKeyEvent(SWT.CR, precondition= {this@ParamListWidget.children.last() === this}) {
                        this@MethodWidget.body!!.insertBeginning()
                    }
                name.addFocusLostAction(::isValidSimpleName) {
                    node.modifyCommand(node.name, SimpleName(name.text), node::setName)
                }

                node.observeProperty<Type>(ObservableProperty.TYPE) {
                    type.set(it!!.asString())
                }
                node.observeProperty<SimpleName>(ObservableProperty.NAME) {
                    name.set(it!!.asString())
                }
            }

            override fun setFocusOnCreation(firstFlag: Boolean) {
                if (firstFlag)
                    name.setFocus()
                else
                    type.setFocus()
            }
        }
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        name.setFocus()
    }

    fun focusParameters() = paramsWidget.setFocus()
}

