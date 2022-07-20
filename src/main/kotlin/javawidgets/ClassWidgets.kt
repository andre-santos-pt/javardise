package javawidgets

import basewidgets.*
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.PrimitiveType.Primitive
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

abstract class NodeWidget<T>(parent: Composite) : Composite(parent, SWT.NONE) {
    abstract val node: T

    abstract fun setFocusOnCreation()
}



abstract class MemberWidget<T : NodeWithModifiers<*>>(
    parent: Composite,
    override val node: T
) : NodeWidget<NodeWithModifiers<*>>(parent) {
    val modifiers = mutableListOf<TokenWidget>()

    lateinit var column: Composite
    lateinit var firstRow: Composite

    init {
        layout = FillLayout()
        column = column {
            firstRow = row {
                node.modifiers.forEach {
                    val mod = Factory.newTokenWidget(this, it.keyword.asString())
                    mod.addDeleteListener(node, it)
                    modifiers.add(mod)
                }

                node.modifiers.register(object : AstObserverAdapter() {
                    override fun listChange(
                        observedNode: NodeList<*>,
                        type: AstObserver.ListChangeType,
                        index: Int,
                        nodeAddedOrRemoved: Node
                    ) {
                        val mod = nodeAddedOrRemoved as Modifier
                        if (type == AstObserver.ListChangeType.ADDITION) {
                            val w = Factory.newTokenWidget(firstRow, mod.keyword.asString())
                            w.addDeleteListener(node, mod)
                            // BUG  should move to same place
                            w.moveAboveInternal(if (modifiers.isEmpty()) firstRow.children[0] else modifiers.last().widget)
                        } else {
                            val index = modifiers.indexOfFirst { it.text == mod.keyword.asString() }
                            if (index != -1) {
                                modifiers[index].dispose()
                                modifiers.removeAt(index)
                                if (index < modifiers.size)
                                    modifiers[index].setFocus()
                                else
                                    firstRow.children[index]
                            }
                        }
                        requestLayout()
                    }
                })
            }
        }
    }


    private fun TokenWidget.addDeleteListener(node: NodeWithModifiers<*>, modifier: Modifier) {
        addDeleteListener {
            Commands.execute(object : Command {
                override fun run() {
                    node.modifiers.remove(modifier)
                }

                override fun undo() {
                    node.modifiers.add(modifier)
                }
            })
        }
    }
}

class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type) {
    private val keyword: TokenWidget
    private val id: Id
    private var body: SequenceWidget

    init {
        layout = FillLayout()
        keyword = Factory.newTokenWidget(firstRow, "class")
        id = Id(firstRow, type.name.id)
        id.addFocusListenerInternal(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                if (id.text.isNotEmpty())
                    Commands.execute(object : Command {
                        val existingName = type.name
                        override fun run() {
                            type.name = SimpleName(id.text)
                        }

                        override fun undo() {
                            type.name = existingName
                        }
                    })
                else
                    id.text = type.name.id
            }
        })
        FixedToken(firstRow, "{")

        body = SequenceWidget(column, 1) { w, e ->
            TextWidget.create(w)
        }
        type.members.forEach {
            createMember(it)
        }
        FixedToken(column, "}")

        type.observeProperty<SimpleName>(ObservableProperty.NAME) {
            id.text = it?.id ?: ""
        }

        type.members.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node
            ) {
                if (type == AstObserver.ListChangeType.ADDITION) {
                    val tail = index == body.children.size
                    val w = createMember(nodeAddedOrRemoved as BodyDeclaration<*>)
                    if (!tail)
                        w.moveAbove(body.children[index])
                } else {
                    TODO("removal")
                }
                body.requestLayout()
            }
        })

    }

    fun createMember(dec: BodyDeclaration<*>) =
        when (dec) {
            is FieldDeclaration -> FieldWidget(body, dec)
            is MethodDeclaration -> MethodWidget(body, dec)
            is ConstructorDeclaration -> MethodWidget(body, dec)
            else -> TODO("unsupported - $dec")
        }

    override fun setFocusOnCreation() {
        id.setFocus()
    }

}

class FieldWidget(parent: Composite, val dec: FieldDeclaration) :
    MemberWidget<FieldDeclaration>(parent, dec) {

    val typeId: Id

    init {
        typeId = Id(firstRow, dec.elementType.toString())
        Id(firstRow, dec.variables[0].name.asString()) // TODO multi var
        FixedToken(firstRow, ";")
    }

    override fun setFocusOnCreation() {
        typeId.setFocus()
    }

}


class MethodWidget(parent: Composite, dec: CallableDeclaration<*>) :
    MemberWidget<CallableDeclaration<*>>(parent, dec) {

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

        if (node.isConstructorDeclaration) {
            name.setReadOnly()
            (node.parentNode.get() as TypeDeclaration<*>)
                .observeProperty<SimpleName>(ObservableProperty.NAME) {
                    name.set((it as SimpleName).asString())
                    (node as ConstructorDeclaration).name = it
                }
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
                    val param = Parameter(PrimitiveType(Primitive.INT),SimpleName("parameter"))
                    override fun run() {
                        // TODO type in ID
                        parameters.add(0, param)
                    }

                    override fun undo() {
                       parameters.remove(param)
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
                        val index = parameters.indexOf(node)

                        override fun run() {
                            parameters.remove(node)
                        }

                        override fun undo() {
                            parameters.add(index, node)
                        }
                    })
                }

                name = Id(this, node.name.asString())
                name.addKeyEvent(',') {
                    Commands.execute(object : Command {
                        val param = Parameter(PrimitiveType(Primitive.INT), SimpleName("parameter"))
                        override fun run() {
                            parameters.add(index+1, param)
                        }

                        override fun undo() {
                            parameters.remove(param)
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

}

