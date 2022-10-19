package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.ArrayInitializerExpr
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK

class NewArrayInitExpressionWidget(
    parent: Composite,
    override val node: ArrayInitializerExpr
) : ExpressionWidget<ArrayInitializerExpr>(parent) {

    val args: ArgumentListWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        args = ArgumentListWidget(parent, "{", "}", node, node.values)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        args.setFocus()
    }

    override val tail: TextWidget
        get() = args.closeBracket
}