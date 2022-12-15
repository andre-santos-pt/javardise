package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.observeNotNullProperty

class ArrayAccessExpressionWidget(
    parent: Composite,
    override val node: ArrayAccessExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ArrayAccessExpr>(parent) {
    var target: ExpressionWidget<*>
    val open: TokenWidget
    val close: TokenWidget
    var index: ExpressionWidget<*>

    init {
        open = TokenWidget(this, "[")
        close = TokenWidget(this, "]")
        close.addDeleteListener {
            editEvent(node.name.clone())
        }
        target = drawExpression(node.name)
        index = drawIndex(node.index)

        observeNotNullProperty<Expression>(ObservableProperty.NAME) {
            target.dispose()
            drawExpression(it)
        }

        observeNotNullProperty<Expression>(ObservableProperty.INDEX) {
            index.dispose()
            drawIndex(it)
        }
    }

    private fun drawExpression(
        expression: Expression
    ): ExpressionWidget<*> {
        target = createExpressionWidget(this, expression) {
            if (it == null)
                editEvent(null)
            else
                node.modifyCommand(node.name, it, node::setName)
        }
        target.moveAbove(open.widget)
        target.requestLayout()
        target.setFocusOnCreation()
        return target
    }

    private fun drawIndex(
        expression: Expression
    ): ExpressionWidget<*> {
        index = createExpressionWidget(this, expression) {
            if (it == null)
                editEvent(node.name.clone())
            else
                node.modifyCommand(node.index, it, node::setIndex)
        }
        index.moveBelow(open.widget)
        index.requestLayout()
        index.setFocusOnCreation()
        return index
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        index.setFocus()
    }

    override val head: TextWidget
        get() = target.head

    override val tail: TextWidget
        get() = close
}