package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
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

class CallExpressionWidget(parent: Composite, override val node: MethodCallExpr) :
    ExpWidget<MethodCallExpr>(parent) {
    lateinit var target: Id
    var methodName: Id
    lateinit var insert: TextWidget
    val openBracket: FixedToken
    val closeBracket: TokenWidget
    val argumentWidgets = mutableListOf<ExpWidget<*>>()

    init {
        layout = ROW_LAYOUT_H_SHRINK
        if (node.scope.isPresent) {
            target = SimpleNameWidget(this, node.scope.get()) { it.toString() }
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {

            }  //action = createDeleteEvent(node, block))
            FixedToken(this, ".")
        }
        methodName = SimpleNameWidget(this, node.name) { it.asString() }
        openBracket = FixedToken(this, "(")

        node.arguments.forEach {
            createArgument(it)
        }
        if (node.arguments.isEmpty())
            createInsert(this)

        closeBracket = TokenWidget(this, ")")

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
            override fun elementAdd(list: NodeList<Expression>, index: Int, node: Expression) {
                createArgument(node).setFocus()
            }

            override fun elementRemove(list: NodeList<Expression>, index: Int, node: Expression) {
                val a = argumentWidgets.find { it.node === node }
                a?.let {
                    it.dispose()
                    argumentWidgets.remove(it)
                }
            }
        })
    }

    private fun createArgument(exp: Expression): ExpWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element = exp

                override fun run() {
                    val index = node.arguments.indexOf(exp) // BUG
                    node.arguments[index] = it
                }

                override fun undo() {
                    val index = node.arguments.indexOf(it)
                    node.arguments[index] = element
                }
            })
        }
        // TODO cast..
        (arg as SimpleExpressionWidget).expression.addKeyEvent(',') {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                override fun run() {
                    node.arguments.addAfter(element, arg.node)
                }

                override fun undo() {
                    node.arguments.remove(element)
                }
            })
        }
        if (argumentWidgets.isEmpty()) {
            arg.expression.moveBelowInternal(openBracket.label)
        } else {
            val comma = FixedToken(this, ",")
            comma.label.moveBelow(argumentWidgets.last())
            arg.expression.moveBelowInternal(comma.label)
        }
        argumentWidgets.add(arg)
        requestLayout()
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
}