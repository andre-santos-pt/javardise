package javawidgets.statements

import basewidgets.*
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import javawidgets.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import row

class CallWidget(
    parent: SequenceWidget,
    node: ExpressionStmt, override val block: BlockStmt
) :
    StatementWidget<ExpressionStmt>(parent, node) {
    lateinit var target: Id
    lateinit var methodName: Id
    val row: Composite
    val call: MethodCallExpr
    lateinit var insert: TextWidget
    lateinit var openBracket: FixedToken

    val argumentWidgets = mutableListOf<ExpressionFreeWidget>()

    init {
        require(node.expression is MethodCallExpr)
        call = node.expression as MethodCallExpr

        layout = FillLayout()
        row = row {
            if (call.scope.isPresent) {
                target = SimpleNameWidget(this, call.scope.get()) { it.toString() }
                target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
                FixedToken(this, ".")
            }
            methodName = SimpleNameWidget(this, call.name) { it.asString() }
            openBracket = FixedToken(this, "(")
        }

        call.arguments.forEach {
            createArgument(it)
        }

        if (call.arguments.isEmpty())
            createInsert(row)

        FixedToken(row, ")")
        FixedToken(row, ";")

        methodName.addFocusLostAction {
            if (methodName.text.isEmpty())
                methodName.set(call.name.asString())
            else if (methodName.text != call.name.asString()) {
                Commands.execute(object : Command {
                    override val target = call
                    override val kind = CommandKind.MODIFY
                    override val element = call.name
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

    private fun registerObservers() {
        call.observeProperty<SimpleName>(ObservableProperty.NAME) {
            methodName.set(it?.asString() ?: call.name.asString())
        }

        call.arguments.observeList(object : ListObserver<Expression> {
            override fun elementAdd(list: NodeList<Expression>, index: Int, node: Expression) {
                createArgument(node).setFocus()
            }

            override fun elementRemove(list: NodeList<Expression>, index: Int, node: Expression) {
                val a = argumentWidgets.find { it.expression === node }
                a?.let {
                    it.delete()
                    argumentWidgets.remove(it)
                }
            }
        })
    }

    private fun createArgument(node: Expression): ExpressionFreeWidget {
        val arg = ExpressionFreeWidget(row, node) {
            Commands.execute(object : Command {
                override val target = call
                override val kind = CommandKind.MODIFY
                override val element = node

                override fun run() {
                    val index = call.arguments.indexOf(node) // BUG
                    call.arguments[index] = it
                }

                override fun undo() {
                    val index = call.arguments.indexOf(it)
                    call.arguments[index] = element
                }
            })
        }
        arg.addKeyEvent(',') {
            Commands.execute(object : Command {
                override val target = call
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                override fun run() {
                    call.arguments.addAfter(element, arg.expression)
                }

                override fun undo() {
                    call.arguments.remove(element)
                }
            })
        }
        if (argumentWidgets.isEmpty()) {
            arg.moveBelowInternal(openBracket.label)
        } else {
            val comma = FixedToken(row, ",")
            comma.label.moveBelow(argumentWidgets.last().widget)
            arg.moveBelowInternal(comma.label)
        }
        argumentWidgets.add(arg)
        row.requestLayout()
        return arg
    }

    override fun setFocus(): Boolean {
        return methodName.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        methodName.setFocus()
    }

    private fun createInsert(parent: Composite) {
        insert = TextWidget.create(parent, " ") { c, s ->
            c.toString().matches(TYPE_CHARS)
        }

        insert.addFocusLostAction {
            if (tryParseExpression(insert.text)) {
                doAddArgummentCommand(StaticJavaParser.parseExpression(insert.text))
                insert.delete()
            } else
                insert.clear()
        }
        insert.addKeyEvent(',', precondition = { tryParseExpression(it) }) {
            val insertExp = StaticJavaParser.parseExpression<Expression>(insert.text)
            doAddArgummentCommand(insertExp)
            doAddArgummentCommand(NameExpr("expression"), insertExp)
            insert.delete()
        }
        val listener = insert.addFocusLostAction {
            insert.widget.text = " "
        }

        insert.widget.addDisposeListener {
            insert.widget.removeFocusListener(listener)
        }
    }

    private fun doAddArgummentCommand(expression: Expression, after: Expression? = null) {
        Commands.execute(object : Command {
            override val target = node.expression as MethodCallExpr
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
}