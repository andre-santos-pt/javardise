package javawidgets

import basewidgets.*
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import javassist.expr.MethodCall
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

abstract class MemberWidget<T : NodeWithModifiers<*>>(
    parent: Composite,
    val member: T,
    val executor: CommandExecutor
) : Composite(parent, SWT.NONE) {
    val modifiers = mutableListOf<TokenWidget>()

    lateinit var column: Composite
    lateinit var firstRow: Composite

    init {
        layout = FillLayout()
        column = column {
            firstRow = row {
                member.modifiers.forEach {
                    val mod = TokenWidget(this, it.keyword.asString())
                    mod.addDeleteListener(member, it)
                    modifiers.add(mod)
                }

                member.modifiers.register(object : AstObserverAdapter() {
                    override fun listChange(
                        observedNode: NodeList<*>,
                        type: AstObserver.ListChangeType,
                        index: Int,
                        nodeAddedOrRemoved: Node
                    ) {
                        val mod = nodeAddedOrRemoved as Modifier
                        if (type == AstObserver.ListChangeType.ADDITION) {
                            val w = TokenWidget(firstRow, mod.keyword.asString())
                            w.addDeleteListener(
                                nodeAddedOrRemoved as NodeWithModifiers<*>,
                                mod
                            ) // BUG  class com.github.javaparser.ast.Modifier cannot be cast to class com.github.javaparser.ast.nodeTypes.NodeWithModifiers (com.github.javaparser.ast.Modifier and com.github.javaparser.ast.nodeTypes.NodeW
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
            executor.execute(object : Command {
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

class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration, executor: CommandExecutor) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type, executor) {
    private lateinit var id: Id
    private lateinit var body: SequenceWidget

    init {
        layout = FillLayout()

        TokenWidget(firstRow, "class")
        id = Id(firstRow, type.name.id)
        id.addFocusListenerInternal(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                if (id.text.isNotEmpty())
                    executor.execute(object : Command {
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
            is FieldDeclaration -> FieldWidget(body, dec, executor)
            is MethodDeclaration -> MethodWidget(body, dec, executor)
            is ConstructorDeclaration -> MethodWidget(body, dec, executor)
            else -> TODO("unsupported - $dec")
        }

}

class FieldWidget(parent: Composite, val dec: FieldDeclaration, executor: CommandExecutor) :
    MemberWidget<FieldDeclaration>(parent, dec, executor) {

    val typeId: Id

    init {
        typeId = Id(firstRow, dec.elementType.toString())
        Id(firstRow, dec.variables[0].name.asString()) // TODO multi var
        FixedToken(firstRow, ";")
    }

}


class MethodWidget(parent: Composite, dec: CallableDeclaration<*>, executor: CommandExecutor) :
    MemberWidget<CallableDeclaration<*>>(parent, dec, executor) {

    var typeId: Id? = null
    lateinit var body: SequenceWidget
    val bodyModel =
        if (dec is MethodDeclaration) dec.body.get()!!  // TODO watch out for signature only
        else (dec as ConstructorDeclaration).body

    init {
        if (member.isMethodDeclaration)
            typeId = Id(firstRow, (member as MethodDeclaration).type.toString())
        val name = Id(firstRow, member.name.asString())
        if (member.isConstructorDeclaration) {
            name.setReadOnly()
            (member.parentNode.get() as TypeDeclaration<*>)
                .observeProperty<SimpleName>(ObservableProperty.NAME) {
                    name.set((it as SimpleName).asString())
                    (member as ConstructorDeclaration).name = it
                }
        }
        FixedToken(firstRow, "(")
        member.parameters.forEachIndexed { index, parameter ->
            if (index != 0)
                FixedToken(firstRow, ",")

            Id(firstRow, parameter.type.asString())
            Id(firstRow, parameter.name.asString())
        }
        FixedToken(firstRow, ")")
        FixedToken(firstRow, "{")


        body = SequenceWidget(column, 1) { w, e ->
            createInsert(w, executor, bodyModel)
        }
        bodyModel.statements.forEach {
            createWidget(it, body, executor)
        }
        FixedToken(column, "}")


        bodyModel.statements.register(object : ListAddRemoveObserver() {
            override fun elementAdd(index: Int, node: Node) {
                val w = when (node) {
                    is IfStmt -> IfWidget(body, node, executor)
                    is WhileStmt -> WhileWidget(body, node, executor)
                    is ReturnStmt -> ReturnWidget(body, node, executor)
                    is ExpressionStmt ->
                        if(node.expression is AssignExpr) AssignWidget(body, node.expression as AssignExpr, executor)
                        else
                            if (node.expression is MethodCallExpr) CallWidget(body, node.expression as MethodCallExpr, executor)
                            else TODO()

                    else -> TODO("NA")
                }
                w.moveAbove(body.insertWidget)
                w.setFocus()
                body.requestLayout()
            }

            override fun elementRemove(index: Int, node: Node) {
                body.children.find { it is StatementWidget<*> && it.statement == node }
                    ?.let { it.dispose() }
                body.requestLayout()
            }
        })
    }


    inner class ParamWidget(parent: Composite, val param: Parameter) : Composite(parent, SWT.NONE) {
        init {
            layout = RowLayout()
            Id(this, param.type.asString())
            Id(this, param.name.asString())
        }
    }

}

