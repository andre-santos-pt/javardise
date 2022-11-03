package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget

class NewArrayInitExpressionWidget(
    parent: Composite,
    override val node: ArrayInitializerExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ArrayInitializerExpr>(parent) {

    val args: ArgumentListWidget<Expression>

    init {
        args = ArgumentListWidget(this, "{", "}", node, node.values)
        args.closeBracket.addDeleteListener {
            editEvent(NameExpr("expression"))
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        args.setFocus()
    }

    override val tail: TextWidget
        get() = args.closeBracket
}