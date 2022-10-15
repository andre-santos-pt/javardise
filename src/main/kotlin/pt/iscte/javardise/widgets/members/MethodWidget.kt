package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.ExpressionStmt
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
    val name: Id
    override lateinit var body: SequenceWidget

    val bodyModel =
        if (dec is MethodDeclaration) dec.body.get()  // TODO watch out for signature only
        else (dec as ConstructorDeclaration).body

    val paramsWidget: ParamListWidget

    override val closingBracket: TokenWidget


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


    private val observers = mutableListOf<(Node?, Any?) -> Unit>()

    fun addObserver(action: (Node?, Any?) -> Unit) {
        observers.add(action)
    }

    init {
        if (node.isMethodDeclaration)
            typeId = SimpleTypeWidget(firstRow, (node as MethodDeclaration).type) { it.asString() }

        name = SimpleNameWidget(firstRow, node.name) { it.asString() }
        name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
            Commands.execute(object : Command {
                override val target = node.parentNode.get() as ClassOrInterfaceDeclaration // BUG
                override val kind: CommandKind = CommandKind.REMOVE
                override val element: Node = dec
                val index: Int = target.members.indexOf(dec)
                override fun run() {
                    dec.remove()
                }

                override fun undo() {
                    target.members.add(index, dec.clone())
                }

            })
            dec.remove()
        }

        name.addFocusLostAction {
            if (name.isValid() && name.text != node.nameAsString)
                Commands.execute(object : ModifyCommand<SimpleName>(node, node.name) {
                    override fun run() {
                        node.name = SimpleName(name.text)
                    }

                    override fun undo() {
                        node.name = element
                    }
                })
            else {
                name.set(node.name.id)
            }
        }

        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it?.asString())
            //(node.parentNode.get() as ClassOrInterfaceDeclaration).name = it?.clone()
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

        body = createSequence(column, bodyModel)
        TokenWidget(firstRow, "{").addInsert(null, body, true)
        closingBracket = TokenWidget(column, "}")

        Display.getDefault().addFilter(SWT.FocusIn, focusListener)
    }

    override fun dispose() {
        Display.getDefault().removeFilter(SWT.FocusIn, focusListener)
        super.dispose()
    }

    inner class ParamListWidget(parent: Composite, val parameters: NodeList<Parameter>) :
        Composite(parent, SWT.NONE) {
        lateinit var insert: TextWidget

        init {
            layout = ROW_LAYOUT_H_SHRINK

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
                    if (parameters.size == 1)
                        createInsert()

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
            insert = TextWidget.create(this, " ") { c, s ->
                c.toString().matches(TYPE_CHARS)
            }
            insert.addKeyEvent(SWT.SPACE, precondition = { tryParseType(it) }) {
                Commands.execute(object : Command {
                    override val target = dec
                    override val kind = CommandKind.ADD
                    override val element: Parameter
                        get() = Parameter(StaticJavaParser.parseType(insert.text), SimpleName("parameter"))

                    override fun run() {
                        parameters.add(0, element)
                    }

                    override fun undo() {
                        parameters.remove(element)
                    }
                })
                insert.delete()
            }
            insert.addFocusLostAction {
                insert.widget.text = " "
            }

        }


        private fun addParams() {
            parameters.forEachIndexed { index, parameter ->
                if (index != 0)
                    FixedToken(this, ",")

                ParamWidget(this, index, parameter)
            }
        }

        // TODO name listeners
        inner class ParamWidget(parent: Composite, val index: Int, override val node: Parameter) :
        Composite(parent, SWT.NONE), NodeWidget<Parameter> {
            val type: Id
            val name: Id

            init {
                layout = RowLayout()
                (layout as RowLayout).marginTop = 0
                type = SimpleTypeWidget(this, node.type) { it.asString() }
                type.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    Commands.execute(object : Command {
                        override val target = dec
                        override val kind = CommandKind.ADD
                        override val element = node

                        val index = parameters.indexOf(element)

                        override fun run() {
                            parameters.remove(element)
                        }

                        override fun undo() {
                            parameters.add(index, element)
                        }
                    })
                }

                name = SimpleNameWidget(this, node.name) { it.asString() }
                name.addKeyEvent(',') {
                    Commands.execute(object : Command {
                        override val target = dec
                        override val kind = CommandKind.ADD
                        override val element: Parameter
                            get() = Parameter(StaticJavaParser.parseType("type"), SimpleName("parameter"))

                        override fun run() {
                            parameters.add(index + 1, element)
                        }

                        override fun undo() {
                            parameters.remove(element)
                        }
                    })
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

