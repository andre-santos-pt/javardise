package javawidgets

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display

class MethodWidget(parent: Composite, val dec: CallableDeclaration<*>, style: Int = SWT.NONE) :
    MemberWidget<CallableDeclaration<*>>(parent, dec, style) {

    var typeId: Id? = null
    val name: Id
    var body: SequenceWidget
    val bodyModel =
        if (dec is MethodDeclaration) dec.body.get()  // TODO watch out for signature only
        else (dec as ConstructorDeclaration).body

    init {
        if (node.isMethodDeclaration)
            typeId = Id(firstRow, (node as MethodDeclaration).type.toString())

        name = Id(firstRow, node.name.asString())
        name.addKeyEvent(SWT.BS, precondition = {it.isEmpty()}) {
            Commands.execute(object : Command {
                override val target: ClassOrInterfaceDeclaration = dec.parentNode.get() as ClassOrInterfaceDeclaration
                override val kind: CommandKind = CommandKind.REMOVE
                override val element: Node = dec
                val index: Int =  target.members.indexOf(dec)
                override fun run() {
                    dec.remove()
                }

                override fun undo() {
                    target.members.add(index, dec.clone())
                }

            })
            dec.remove()
        }

        if (node.isConstructorDeclaration) {
            name.setReadOnly()
            // problem with MVC
//            (node.parentNode.get() as TypeDeclaration<*>)
//                .observeProperty<SimpleName>(ObservableProperty.NAME) {
//                    name.set((it as SimpleName).asString())
//                    (node as ConstructorDeclaration).name = it
//                }
        }
        FixedToken(firstRow, "(")
        ParamListWidget(firstRow, node.parameters)
        FixedToken(firstRow, ")")
        FixedToken(firstRow, "{")
        body = createSequence(column, bodyModel)
        FixedToken(column, "}")
    }

    inner class ParamListWidget(parent: Composite, val parameters: NodeList<Parameter>) : Composite(parent, SWT.NONE) {
        init {
            layout = RowLayout()
            (layout as RowLayout).marginTop = 0

            val insert = Id(this, " ")
            insert.addKeyEvent(SWT.SPACE, precondition = { it.isNotBlank() }) {
                Commands.execute(object : Command {
                    override val target = dec
                    override val kind = CommandKind.ADD
                    override val element: Parameter
                        get() = Parameter(PrimitiveType(PrimitiveType.Primitive.INT), SimpleName("parameter"))

                    override fun run() {
                        // TODO type in ID
                        parameters.add(0, element)
                    }

                    override fun undo() {
                        parameters.remove(element)
                    }
                })
                insert.set(" ")
            }
            addParams()

            parameters.register(object : ListAddRemoveObserver<Parameter>() {
                override fun elementAdd(list: NodeList<Parameter>, index: Int, node: Parameter) {
                    val p = ParamWidget(this@ParamListWidget, index, node)
                    if (index == 0 && list.isEmpty()) {
                        //ParamWidget(this@ParamListWidget, index, node)
                    }
                    else if (index == list.size) {
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
                    p.setFocusOnCreation()
                    requestLayout()
                }

                override fun elementRemove(list: NodeList<Parameter>, index: Int, node: Parameter) {
                    val index = children.indexOfFirst { it is ParamWidget && it.node == node }
                    if (index != -1) {
                        children[index].dispose()

                        // comma
                        if (index == 0 && list.size > 1)
                            children[index].dispose()
                        else if(index != 0)
                            children[index - 1].dispose()
                    }
                    requestLayout()
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
        inner class ParamWidget(parent: Composite, val index: Int, override val node: Parameter) : NodeWidget<Parameter>(parent) {
            val type: Id
            val name: Id

            init {
                layout = RowLayout()
                (layout as RowLayout).marginTop = 0
                type = Id(this, node.type.asString())
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

                name = Id(this, node.name.asString())
                name.addKeyEvent(',') {
                    Commands.execute(object : Command {
                        override val target = dec
                        override val kind = CommandKind.ADD
                        override val element: Parameter
                            get() = Parameter(PrimitiveType(PrimitiveType.Primitive.INT), SimpleName("parameter"))

                        override fun run() {
                            parameters.add(index + 1, element)
                        }

                        override fun undo() {
                            parameters.remove(element)
                        }
                    })
                }
            }

            override fun setFocusOnCreation() {
                type.setFocus()
            }
        }
    }



    override fun setFocusOnCreation() {
        name.setFocus()
    }


    fun addSelectionListener(event: (Node) -> Unit) {
        //TODO("Not yet implemented")
    }

    fun getNodeOnFocus() : Node? {
        val control = Display.getDefault().focusControl
        var parent = control.parent
        while(parent != null && parent !is NodeWidget<*>)
            parent = parent.parent

        if(parent is NodeWidget<*>)
            return parent.node as Node
        else
            return null
    }
}