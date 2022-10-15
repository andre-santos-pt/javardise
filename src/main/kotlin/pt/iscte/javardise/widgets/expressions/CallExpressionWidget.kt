package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.SimpleNameWidget
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*

class CallExpressionWidget(
    parent: Composite,
    override val node: MethodCallExpr
) :
    ExpWidget<MethodCallExpr>(parent) {
    var target: Id? = null
    var methodName: Id
    val openBracket: FixedToken
    lateinit var insert: TextWidget
    val closeBracket: TokenWidget

    data class ArgWidget(val comma: FixedToken?, var arg: ExpWidget<*>) {
        fun dispose() {
            comma?.dispose()
            arg.dispose()
        }
    }

    val argumentWidgets = mutableListOf<ArgWidget>()

    init {
        layout = ROW_LAYOUT_H_SHRINK
        if (node.scope.isPresent) {
            target = SimpleNameWidget(this, node.scope.get()) { it.toString() }
            target!!.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                //action = createDeleteEvent(node, block))
            }
            FixedToken(this, ".")
        }
        methodName = SimpleNameWidget(this, node.name) { it.asString() }
        openBracket = FixedToken(this, "(")
        closeBracket = TokenWidget(this, ")")

        if (node.arguments.isEmpty())
            createInsert(this)

        node.arguments.forEachIndexed { index, expression ->
            createArgument(expression, index, false)
        }
        methodName.addFocusLostAction {
            if (methodName.text.isEmpty())
                methodName.set(node.name.asString())
            else if (methodName.text != node.name.asString()) {
                Commands.execute(object : Command {
                    override val target = node
                    override val kind = CommandKind.MODIFY
                    override val element = node.name
                    override fun run() {
                        target.name = SimpleName(methodName.text)
                    }

                    override fun undo() {
                        target.name = element
                    }
                })
            }
        }
        registerObservers()
    }

    override val tail: TextWidget
        get() = closeBracket

    private fun registerObservers() {
        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            methodName.set(it?.asString() ?: node.name.asString())
        }

        node.arguments.observeList(object : ListObserver<Expression> {
            override fun elementAdd(
                list: NodeList<Expression>,
                index: Int,
                node: Expression
            ) {
                createArgument(node, index, false).setFocus()
            }

            override fun elementReplace(
                list: NodeList<Expression>,
                index: Int,
                old: Expression,
                new: Expression
            ) {
                createArgument(new, index, true)
            }

            override fun elementRemove(
                list: NodeList<Expression>,
                index: Int,
                node: Expression
            ) {
                argumentWidgets[index].dispose()
                argumentWidgets.removeAt(index)
                requestLayout()
                if(index != argumentWidgets.size)
                    argumentWidgets[index].arg.setFocus()
                else if(index != 0)
                    argumentWidgets[index-1].arg.setFocus()
                else {
                    createInsert(this@CallExpressionWidget)
                    insert.setFocus()
                }
            }
        })
    }

    fun <T : Node> NodeList<T>.indexOfIdentity(e: T): Int {
        for(i in 0..lastIndex)
            if(get(i) === e)
                return i
        return -1
    }

    private fun createArgument(exp: Expression, index: Int, replace: Boolean):
            ExpWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.MODIFY
                override val element = exp

                var i : Int = -1
                override fun run() {
                    i = node.arguments.indexOfIdentity(element)
                    node.arguments[i] = it
                }

                override fun undo() {
                    node.arguments[i] = element
                }
            })
        }

        arg.tail.addKeyEvent(',') {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                var i : Int = -1
                override fun run() {
                    i = node.arguments.indexOfIdentity(arg.node)
                    node.arguments.add(i+1, element)
                }

                override fun undo() {
                    node.arguments.removeAt(i)
                }
            })
        }

        arg.tail.addKeyEvent(SWT.BS, precondition = {it.isEmpty()}) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.REMOVE
                override val element = exp

                var i : Int = -1
                override fun run() {
                    i = node.arguments.indexOfIdentity(arg.node)
                    node.arguments.removeAt(i)
                }

                override fun undo() {
                    node.arguments.add(i, element)
                }
            })
        }

        if (argumentWidgets.isEmpty()) {
            arg.moveBelow(openBracket.label)
            argumentWidgets.add(index, ArgWidget(null, arg))
        } else {
            if(replace) {
                arg.moveAbove(argumentWidgets[index].arg)
                argumentWidgets[index].arg.dispose()
                argumentWidgets[index].arg = arg
            }
            else {
                val comma = FixedToken(this, ",")
                if (index == argumentWidgets.size) {
                    comma.label.moveAbove(closeBracket.widget)
                    arg.moveBelow(comma.label)
                } else {
                    comma.label.moveAbove(argumentWidgets[index].arg)
                    arg.moveAbove(comma.label)
                }
                argumentWidgets.add(index, ArgWidget(comma, arg))
            }
        }
        arg.requestLayout()
        return arg
    }

    override fun setFocus(): Boolean {
        return methodName.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        methodName.setFocus()
    }

    private fun createInsert(parent: Composite) {
        fun doAddArgummentCommand(
            expression: Expression,
            after: Expression? = null
        ) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element = expression

                override fun run() {
                    if (after != null)
                        target.arguments.addAfter(element, after)
                    else
                        target.arguments.add(element)
                }

                override fun undo() {
                    target.arguments.remove(element)
                }
            })
        }

        insert = TextWidget.create(parent, " ") { c, s ->
            c.toString().matches(TYPE_CHARS)
        }

        insert.moveAboveInternal(closeBracket.widget)

        insert.addFocusLostAction {
            if (tryParseExpression(insert.text)) {
                doAddArgummentCommand(StaticJavaParser.parseExpression(insert.text))
                insert.delete()
            } else
                insert.clear()
        }
        insert.addKeyEvent(',', precondition = { tryParseExpression(it) }) {
            val insertExp =
                StaticJavaParser.parseExpression<Expression>(insert.text)
            insert.delete()
            doAddArgummentCommand(insertExp)
            doAddArgummentCommand(NameExpr("expression"), insertExp)
        }
        val listener = insert.addFocusLostAction {
            insert.widget.text = " "
        }

        insert.widget.addDisposeListener {
            insert.widget.removeFocusListener(listener)
        }
    }


}