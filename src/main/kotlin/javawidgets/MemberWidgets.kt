package javawidgets

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.SWT
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

    init {
        layout = RowLayout()
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
                    val w = TokenWidget(this@MemberWidget, mod.keyword.asString())
                    w.addDeleteListener(nodeAddedOrRemoved as NodeWithModifiers<*>, mod) // BUG  class com.github.javaparser.ast.Modifier cannot be cast to class com.github.javaparser.ast.nodeTypes.NodeWithModifiers (com.github.javaparser.ast.Modifier and com.github.javaparser.ast.nodeTypes.NodeW
                    w.moveAboveInternal(if (modifiers.isEmpty()) this@MemberWidget.children[0] else modifiers.last().widget)
                } else {
                    val index = modifiers.indexOfFirst { it.text == mod.keyword.asString() }
                    if (index != -1) {
                        modifiers[index].dispose()
                        modifiers.removeAt(index)
                        if (index < modifiers.size)
                            modifiers[index].setFocus()
                        else
                            this@MemberWidget.children[index]
                    }
                }
                requestLayout()
            }
        })
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

class FieldWidget(parent: Composite, dec: FieldDeclaration, executor: CommandExecutor) :
    MemberWidget<FieldDeclaration>(parent, dec, executor) {

    val typeId: Id

    init {
        typeId = Id(this, dec.elementType.toString())
        Id(this, dec.variables[0].name.asString()) // TODO multi var
        FixedToken(this, ";")
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
        column {
            row {
                if (member.isMethodDeclaration)
                    typeId = Id(this, (member as MethodDeclaration).type.toString())
                Id(this, member.name.asString()) //TODO fixed if constructor
                FixedToken(this, "(")
                member.parameters.forEachIndexed { index, parameter ->
                    if (index != 0)
                        FixedToken(this, ",")
                    ParamWidget(this, parameter)
                }
                FixedToken(this, ")")
                FixedToken(this, "{")
            }
            body = SequenceWidget(this, 1) { w, e ->
                createInsert(w, executor, bodyModel)
            }
            bodyModel.statements.forEach {
                createWidget(it, body, executor)
            }
            FixedToken(this, "}")
        }

        bodyModel.statements.register(object : ListAddRemoveObserver() {
            override fun elementAdd(index: Int, node: Node) {
                val w = when (node) {
                    is IfStmt -> IfWidget(body, node, executor)
                    is WhileStmt -> WhileWidget(body, node, executor)
                    is ReturnStmt -> ReturnWidget(body, node, executor)
                    is ExpressionStmt -> AssignWidget(body, node.expression as AssignExpr, executor)
                    else -> TODO("NA")
                }
                w.moveAbove(body.insertWidget)
                w.setFocus()
                body.requestLayout()
            }

            override fun elementRemove(index: Int, node: Node) {
                body.children.find { it is ControlWidget<*> && it.stmt == node }
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

