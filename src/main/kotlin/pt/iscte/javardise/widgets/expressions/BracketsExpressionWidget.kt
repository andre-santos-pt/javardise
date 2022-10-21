package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.moveAbove
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.modifyCommand

class BracketsExpressionWidget(
    parent: Composite,
    override val node: EnclosedExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<EnclosedExpr>(parent) {
    val leftBracket: TokenWidget
    val rightBracket: TokenWidget

    var expressionWidget: ExpressionWidget<*>
    val expressionObserver: AstObserver

    init {
        layout = ROW_LAYOUT_H_SHRINK

        leftBracket = TokenWidget(this, "(")
        rightBracket = TokenWidget(this, ")")
        expressionWidget = drawExpression(this, node.inner)

        expressionObserver = node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            expressionWidget.dispose()
            drawExpression(this, node.inner)
        }

        leftBracket.addDeleteListener {
            editEvent(node.inner.clone())
        }
        rightBracket.addDeleteListener {
            editEvent(node.inner.clone())
        }
    }

    private fun drawExpression(parent: Composite, expression: Expression): ExpressionWidget<*> {
        expressionWidget = createExpressionWidget(parent, expression) {
            node.modifyCommand(node.inner, it, node::setInner)
            expressionWidget.dispose()
            if(it != null)
                drawExpression(parent, it)
        }
        expressionWidget.moveAbove(rightBracket)
        expressionWidget.requestLayout()
        expressionWidget.setFocusOnCreation()
        return expressionWidget
    }

    override fun dispose() {
        super.dispose()
        node.unregister(expressionObserver)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expressionWidget.setFocusOnCreation(firstFlag)
    }

    override val tail: TextWidget
        get() = rightBracket
}