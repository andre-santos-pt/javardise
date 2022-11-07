package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.nodeTypes.NodeWithArguments
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

class ArgumentListWidget<T : Expression, N : Node>(
    parent: Composite,
    open: String,
    close: String,
    val owner: NodeWidget<N>,
    val expressionList: NodeList<T>
) :
    Composite(parent, SWT.NONE) {
    val openBracket: FixedToken
    private lateinit var insert: TextWidget
    val closeBracket: TokenWidget

    private data class ArgWidget(
        val comma: FixedToken?,
        var arg: ExpressionWidget<*>
    ) {
        fun dispose() {
            comma?.dispose()
            arg.dispose()
        }
    }

    private val argumentWidgets = mutableListOf<ArgWidget>()


    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = parent.font
        openBracket = FixedToken(this, open)
        closeBracket = TokenWidget(this, close)

        if (expressionList.isEmpty())
            createInsert(this)
        expressionList.forEachIndexed { index, expression ->
            createArgument(expression, index, false)
        }

        expressionList.observeList(object : ListObserver<T> {
            override fun elementAdd(
                list: NodeList<T>,
                index: Int,
                node: T
            ) {
                createArgument(node, index, false).setFocus()
            }

            override fun elementReplace(
                list: NodeList<T>,
                index: Int,
                old: T,
                new: T
            ) {
                createArgument(new, index, true)
            }

            override fun elementRemove(
                list: NodeList<T>,
                index: Int,
                node: T
            ) {
                argumentWidgets[index].dispose()
                argumentWidgets.removeAt(index)
                requestLayout()
                if (index != argumentWidgets.size)
                    argumentWidgets[index].arg.setFocus()
                else if (index != 0)
                    argumentWidgets[index - 1].arg.setFocus()
                else {
                    createInsert(this@ArgumentListWidget)
                    insert.setFocus()
                }
            }
        })
    }



    private fun createArgument(exp: T, index: Int, replace: Boolean):
            ExpressionWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            if (it == null)
                owner.commands.execute(object : Command {
                    override val target = owner.node
                    override val kind = CommandKind.REMOVE
                    override val element = exp

                    var i: Int = -1
                    override fun run() {
                        i = expressionList.indexOfIdentity(exp)
                        expressionList.removeAt(i)
                    }

                    override fun undo() {
                        expressionList.add(i, element)
                    }
                })
            else {
                owner.commands.execute(object : Command {
                    override val target = owner.node
                    override val kind = CommandKind.MODIFY
                    override val element = exp

                    var i: Int = -1
                    override fun run() {
                        i = expressionList.indexOfIdentity(element)
                        expressionList[i] = it as T
                    }

                    override fun undo() {
                        expressionList[i] = element
                    }
                })
            }
        }

        arg.tail.addKeyEvent(',') {
            owner.commands.execute(object : Command {
                override val target = owner.node
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                var i: Int = -1
                override fun run() {
                    i = expressionList.indexOfIdentity(arg.node as T)
                    expressionList.add(i + 1, element as T)
                }

                override fun undo() {
                    expressionList.removeAt(i)
                }
            })
        }

        if (argumentWidgets.isEmpty()) {
            arg.moveBelow(openBracket.label)
            argumentWidgets.add(index, ArgWidget(null, arg))
        } else {
            if (replace) {
                arg.moveAbove(argumentWidgets[index].arg)
                argumentWidgets[index].arg.dispose()
                argumentWidgets[index].arg = arg
            } else {
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
            owner.commands.execute(object : Command {
                override val target = owner.node
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
            if (tryParse<Expression>(insert.text)) {
                doAddArgummentCommand(StaticJavaParser.parseExpression(insert.text))
                insert.delete()
            } else
                insert.clear()
        }
        insert.addKeyEvent(',', precondition = { tryParse<Expression>(it) }) {
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