package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.nodeTypes.NodeWithArguments
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TYPE_CHARS
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ListObserver
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.observeList
import pt.iscte.javardise.external.tryParseExpression

class ArgumentListWidget(parent: Composite, open: String, close: String, val owner: Node, val expressionList: NodeList<Expression>) :
    Composite(parent, SWT.NONE) {
    val openBracket: FixedToken
    private lateinit var insert: TextWidget
    val closeBracket: TokenWidget

    private data class ArgWidget(val comma: FixedToken?, var arg: ExpressionWidget<*>) {
        fun dispose() {
            comma?.dispose()
            arg.dispose()
        }
    }

    private val argumentWidgets = mutableListOf<ArgWidget>()


    init {
        layout = ROW_LAYOUT_H_SHRINK
        openBracket = FixedToken(this, open)
        closeBracket = TokenWidget(this, close)
        if (expressionList.isEmpty())
            createInsert(this)
        expressionList.forEachIndexed { index, expression ->
            createArgument(expression, index, false)
        }

        expressionList.observeList(object : ListObserver<Expression> {
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
                    createInsert(this@ArgumentListWidget)
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
            ExpressionWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            Commands.execute(object : Command {
                override val target = owner
                override val kind = CommandKind.MODIFY
                override val element = exp

                var i : Int = -1
                override fun run() {
                    i = expressionList.indexOfIdentity(element)
                    expressionList[i] = it
                }

                override fun undo() {
                    expressionList[i] = element
                }
            })
        }

        arg.tail.addKeyEvent(',') {
            Commands.execute(object : Command {
                override val target = owner
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                var i : Int = -1
                override fun run() {
                    i = expressionList.indexOfIdentity(arg.node)
                    expressionList.add(i+1, element)
                }

                override fun undo() {
                    expressionList.removeAt(i)
                }
            })
        }

        arg.tail.addKeyEvent(SWT.BS, precondition = {it.isEmpty()}) {
            Commands.execute(object : Command {
                override val target = owner
                override val kind = CommandKind.REMOVE
                override val element = exp

                var i : Int = -1
                override fun run() {
                    i = expressionList.indexOfIdentity(arg.node)
                    expressionList.removeAt(i)
                }

                override fun undo() {
                    expressionList.add(i, element)
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

    private fun createInsert(parent: Composite) {
        fun doAddArgummentCommand(
            expression: Expression,
            after: Expression? = null
        ) {
            Commands.execute(object : Command {
                override val target = owner
                override val kind = CommandKind.ADD
                override val element = expression

                override fun run() {
                    target as NodeWithArguments<*>
                    if (after != null)
                        target.arguments.addAfter(element, after)
                    else
                        target.arguments.add(element)
                }

                override fun undo() {
                    target as NodeWithArguments<*>
                    target.arguments.remove(element)
                }
            })
        }

        insert = TextWidget.create(parent, " ") { c, _ ->
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
            insert.text = " "
        }

        insert.widget.addDisposeListener {
            insert.widget.removeFocusListener(listener)
        }
    }
}