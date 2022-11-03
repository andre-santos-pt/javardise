package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.binaryOperators
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.modifyCommand

class BinaryExpressionWidget(
    parent: Composite,
    override val node: BinaryExpr,
    override val editEvent: (Expression?) -> Unit
) :
    ExpressionWidget<BinaryExpr>(parent) {

    var left: ExpressionWidget<*>
    var operator: TokenWidget
    var right: ExpressionWidget<*>
    val leftObserver: AstObserver
    val rightObserver: AstObserver
    val operatorObserver: AstObserver

    init {
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = { binaryOperators.map { it.asString() } }) {
            node.modifyCommand(
                node.operator,
                binaryOperators.find { op -> op.asString() == it },
                node::setOperator
            )
        }
        left = drawLeft(this, node.left)
        right = drawRight(this, node.right)
        leftObserver =
            node.observeProperty<Expression>(ObservableProperty.LEFT) {
                left.dispose()
                drawLeft(this, it!!)
            }
        rightObserver =
            node.observeProperty<Expression>(ObservableProperty.RIGHT) {
                right.dispose()
                drawRight(this, it!!)
            }
        operatorObserver =
            node.observeProperty<BinaryExpr.Operator>(ObservableProperty.OPERATOR) {
                operator.set(it?.asString() ?: "??")
                operator.setFocus()
            }
    }

    override fun dispose() {
        super.dispose()
        node.unregister(leftObserver)
        node.unregister(rightObserver)
        node.unregister(operatorObserver)
    }

    private fun drawLeft(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        left = createExpressionWidget(parent, expression) {
            if (it != null) {
                node.modifyCommand(node.left, it, node::setLeft)
                left.dispose()
                drawLeft(parent, it)
            }
        }
        left.moveAbove(operator.widget)
        left.requestLayout()
        left.setFocusOnCreation()
        return left
    }

    private fun drawRight(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        right = createExpressionWidget(parent, expression) {
            if(it != null) {
                node.modifyCommand(node.right, it, node::setRight)
                right.dispose()
                drawRight(parent, it)
            }
            else
                editEvent(node.left)
        }
        right.moveBelow(operator.widget)
        right.requestLayout()
        right.setFocusOnCreation()
        return right
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        right.setFocus()
    }


    override fun toString(): String {
        return "BiExp $node"
    }

    override val tail: TextWidget
        get() = right.tail
}