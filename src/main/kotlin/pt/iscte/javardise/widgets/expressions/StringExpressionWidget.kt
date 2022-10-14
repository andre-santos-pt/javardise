package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.StringLiteralExpr
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.COMMENT_COLOR
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING

class StringExpressionWidget(parent: Composite, override val node: StringLiteralExpr) : ExpWidget<StringLiteralExpr>(parent) {
    val text: TextWidget
    val close: TokenWidget

    init {
        layout = ROW_LAYOUT_H_STRING
        val open = FixedToken(this, "\"")
        open.label.foreground = COMMENT_COLOR()
        text = TextWidget.create(this, node.value) { _, _ -> true }
        text.widget.foreground = COMMENT_COLOR()
        close = TokenWidget(this, "\"")
        close.widget.foreground = COMMENT_COLOR()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }

    override val tail: TextWidget
        get() = close
}