package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.Expression
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.TextWidget

// TODO BUG in ExpressionList cast
class NewArrayInitExpressionWidget(
    parent: Composite,
    override val node: ArrayInitializerExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ArrayInitializerExpr>(parent) {

    val args: ExpressionListWidget<Expression, ArrayInitializerExpr> =
        ExpressionListWidget(this, "{", "}", this, node.values)

    init {
        args.openBracket.addDeleteListener {
            editEvent(Configuration.hole())
        }
        args.closeBracket.addDeleteListener {
            editEvent(Configuration.hole())
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        args.setFocus()
    }

    override val head: TextWidget
        get() = args.openBracket

    override val tail: TextWidget
        get() = args.closeBracket
}