package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.unaryOperators

class UnaryExpressionWidget(parent: Composite, override val node: UnaryExpr) : ExpressionWidget<UnaryExpr>(parent) {
    var operator: TokenWidget
    var expressionWidget: ExpressionWidget<*>
    val expressionObserver: AstObserver
    val operatorObserver: AstObserver

    init {
        layout = ROW_LAYOUT_H_SHRINK
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = { unaryOperators.filter { it.isPrefix }.map { it.asString() } }) {
            Commands.execute(object : Command {
                override val target: Node = node
                override val kind = CommandKind.MODIFY
                override val element = node.operator

                override fun run() {
                    node.operator = unaryOperators.find { op -> op.asString() == it }
                }

                override fun undo() {
                    node.operator = element
                }
            })
        }

        expressionWidget = drawExpression(this, node.expression)

        expressionObserver = node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            expressionWidget.dispose()
            drawExpression(this, node.expression)
        }

        operatorObserver = node.observeProperty<BinaryExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "??")
            operator.setFocus()
        }
    }

    private fun drawExpression(parent: Composite, expression: Expression): ExpressionWidget<*> {
        expressionWidget = createExpressionWidget(parent, expression) {
            Commands.execute(object :
                ModifyCommand<Expression>(node, node.expression) {
                override fun run() {
                    node.expression = it
                }

                override fun undo() {
                    node.expression = element
                }
            })
            expressionWidget.dispose()
            drawExpression(parent, it)
        }
        expressionWidget.requestLayout()
        expressionWidget.setFocusOnCreation()
        return expressionWidget
    }

    override fun dispose() {
        super.dispose()
        node.unregister(expressionObserver)
        node.unregister(operatorObserver)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expressionWidget.setFocusOnCreation(firstFlag)
    }

    override val tail: TextWidget
        get() = expressionWidget.tail
}