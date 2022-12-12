package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.nodeTypes.NodeWithArguments
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

class ExpressionListWidget<T : Expression, N : Node>(
    parent: Composite,
    open: String,
    close: String,
    val owner: NodeWidget<N>,
    val expressionList: NodeList<T>
) :
    Composite(parent, SWT.NONE) {
    val openBracket: TokenWidget
    private lateinit var insert: TextWidget
    val closeBracket: TokenWidget

    private data class ArgWidget(
        var comma: FixedToken?,
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
        background = parent.background
        foreground = parent.foreground
        font = parent.font
        openBracket = TokenWidget(this, open)
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
                createArgument(node, index, false).setFocusOnCreation()
            }

            override fun elementReplace(
                list: NodeList<T>,
                index: Int,
                old: T,
                new: T
            ) {
                createArgument(new, index, true).setFocusOnCreation()
            }

            override fun elementRemove(
                list: NodeList<T>,
                index: Int,
                node: T
            ) {
                argumentWidgets[index].dispose()
                if (index == 0 && argumentWidgets.size > 1) {
                    argumentWidgets[1].comma?.dispose()
                    argumentWidgets[1].comma = null
                }
                argumentWidgets.removeAt(index)
                requestLayout()
                if (index != argumentWidgets.size)
                    argumentWidgets[index].arg.setFocus() // Cannot invoke "org.eclipse.swt.internal.cocoa.NSTextField.selectText(org.eclipse.swt.internal.cocoa.id)" because "this.view" is null
                else if (index != 0)
                    argumentWidgets[index - 1].arg.setFocus()
                else {
                    createInsert(this@ExpressionListWidget)
                    insert.setFocus()
                }
            }
        })
    }

    override fun setFocus(): Boolean {
        return if(argumentWidgets.isEmpty()) insert.setFocus() else argumentWidgets.first().arg.setFocus()
    }

    private fun createArgument(exp: T, index: Int, replace: Boolean):
            ExpressionWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            if (it == null)
                owner.commandStack.execute(object : Command {
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
                owner.commandStack.execute(object : Command {
                    override val target = owner.node
                    override val kind = CommandKind.MODIFY
                    override val element = exp

                    var i: Int = -1
                    override fun run() {
                        i =
                            expressionList.indexOfIdentity(exp) // BUG illegal index
                        expressionList[i] = it as T
                    }

                    override fun undo() {
                        expressionList[i] = element
                    }
                })

            }
        }
        arg.tailObservers.add {
            addTailListener(arg)
        }
        addTailListener(arg)



        if (argumentWidgets.isEmpty()) {
            arg.moveBelow(openBracket.widget)
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

    private fun addTailListener(arg: ExpressionWidget<*>) {
        arg.tail.addKeyEvent(',') {
            owner.commandStack.execute(object : Command {
                override val target = owner.node
                override val kind = CommandKind.ADD
                override val element = NameExpr(Configuration.fillInToken)

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
        arg.head.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.isCombinedKey(SWT.ARROW_LEFT)) {
                    val i = expressionList.indexOfIdentity(arg.node as T)
                    if (i > 0) {
                        owner.commandStack.execute(object : Command {
                            override val target: Node =
                                expressionList.parentNode.get()
                            override val kind: CommandKind = CommandKind.MODIFY
                            override val element = expressionList

                            override fun run() {
                                expressionList.swap(i, i - 1)
                            }

                            override fun undo() {
                                expressionList.swap(i, i - 1)
                            }
                        })
                    }
                }
            }
        });
    }

    private fun createInsert(parent: Composite) {
        fun doAddArgummentCommand(
            expression: Expression,
            after: Expression? = null
        ) {
            owner.commandStack.execute(object : Command {
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
            c.toString().matches(TYPE_CHARS) || c == SWT.BS
        }

        insert.moveAboveInternal(closeBracket.widget)

        insert.addFocusLostAction {
            if (tryParse<Expression>(insert.text)) {
                doAddArgummentCommand(StaticJavaParser.parseExpression(insert.text))
                insert.delete()
            } else
                insert.clear()
        }

        val keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (insert.widget.isDisposed)
                    return

                if (insert.isAtBeginning || insert.isEmpty) {

                    val unop = unaryOperators.filter { it.isPrefix }
                        .find { it.asString().startsWith(e.character) }
                    if (unop != null) {
                        if (tryParse<Expression>(insert.text)) {
                            val exp = insert.text
                            insert.delete()
                            doAddArgummentCommand(
                                UnaryExpr(
                                    StaticJavaParser.parseExpression(exp),
                                    unop
                                )
                            )

                        } else if (insert.text.isBlank()) {
                            insert.delete()
                            doAddArgummentCommand(
                                UnaryExpr(
                                    NameExpr(Configuration.fillInToken),
                                    unop
                                )
                            )

                        }
                    }
                }
                else if (insert.isAtEnd) {
                    val biop = binaryOperators.find {
                        it.asString().startsWith(e.character)
                    }
                    if (biop != null  && tryParse<Expression>(insert.text)
                    ) {
                        val exp = insert.text
                        insert.delete()
                        doAddArgummentCommand(
                            BinaryExpr(
                                StaticJavaParser.parseExpression(exp),
                                NameExpr(Configuration.fillInToken), biop
                            )
                        )
                    }
                }

                if(e.character == ',') {
                    if(tryParse<Expression>(insert.text)) {
                        val insertExp =
                            StaticJavaParser.parseExpression<Expression>(insert.text)
                        insert.delete()
                        doAddArgummentCommand(insertExp)
                        doAddArgummentCommand(
                            NameExpr(Configuration.fillInToken),
                            insertExp
                        )
                    }
                }
            }
        }
        insert.addKeyListenerInternal(keyListener)
        val listener = insert.addFocusLostAction {
            insert.text = " "
        }

        insert.widget.addDisposeListener {
            insert.widget.removeFocusListener(listener)
        }
    }
}