package javawidgets

import basewidgets.*
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display

class MethodWidget(parent: Composite, val dec: CallableDeclaration<*>, style: Int = SWT.NONE) :
    MemberWidget<CallableDeclaration<*>>(parent, dec, style = style) {

    var typeId: Id? = null
    val name: Id
    var body: SequenceWidget
    val bodyModel =
        if (dec is MethodDeclaration) dec.body.get()  // TODO watch out for signature only
        else (dec as ConstructorDeclaration).body

    val paramsWidget: ParamListWidget

    val closingBracket: TokenWidget

    init {
        if (node.isMethodDeclaration)
            typeId = SimpleTypeWidget(firstRow, (node as MethodDeclaration).type) {it.asString()}

        name = SimpleNameWidget(firstRow, node.name) {it.asString()}
        name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
            Commands.execute(object : Command {
                override val target = node.parentNode as ClassOrInterfaceDeclaration // BUG
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
    }

    fun Composite.findClassWidget(): ClassWidget? {
        if (this is ClassWidget)
            return this
        else {
            val parent = this.parent
            if (parent == null)
                return null
            else
                return parent.findClassWidget()
        }
    }

    inner class ParamListWidget(parent: Composite, val parameters: NodeList<Parameter>) :
        Composite(parent, SWT.NONE) {
        lateinit var insert: TextWidget

        init {
            layout = RowLayout()
            (layout as RowLayout).marginTop = 0

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
                    requestLayout()
                }

                override fun elementRemove(list: NodeList<Parameter>, index: Int, node: Parameter) {
                    val index = children.indexOfFirst { it is ParamWidget && it.node == node }
                    if (index != -1) {
                        children[index].dispose()

                        // comma
                        if (index == 0 && list.size > 1)
                            children[index].dispose()
                        else if (index != 0)
                            children[index - 1].dispose()
                    }
                    if (parameters.size == 1)
                        createInsert()

                    requestLayout()
                }
            })
        }

        private fun createInsert() {
            insert = TextWidget.create(this, " ") {
                c,s -> c.toString().matches(TYPE_CHARS)
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
            insert.addFocusListenerInternal(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    insert.widget.text = " "
                }
            })
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
            NodeWidget<Parameter>(parent) {
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

                name = SimpleNameWidget(this, node.name) { it.asString()}
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
                if(firstFlag)
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

    fun getNodeOnFocus(): Node? {
        val control = Display.getDefault().focusControl
        var parent = control.parent
        while (parent != null && parent !is NodeWidget<*>)
            parent = parent.parent

        if (parent is NodeWidget<*>)
            return parent.node as Node
        else
            return null
    }
}