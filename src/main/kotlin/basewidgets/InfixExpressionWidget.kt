package basewidgets

import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

class InfixExpressionWidget(
    parent: Composite,
    element: Any?,
    operator: String,
    left: (EditorWidget) -> Expression,
    right: (EditorWidget) -> Expression
) : EditorWidget(parent, element), Expression.SubstitutableExpression {

    private var left: Expression
    private val operator: TokenWidget
    private var right: Expression

    init {
        this.left = left(this)
        this.operator = TokenWidget(this, operator)
        this.right = right(this)
        layout = Constants.ROW_LAYOUT_H_SHRINK
        val delListener = this.operator.addDeleteListener {
            val p = this.parent as Expression
            val e = this.left.copyTo(this.parent as EditorWidget)
            if (p.isSubstitutable) (p as Expression.SubstitutableExpression).substitute(this, e)
            p.layoutInternal()
            e.setFocus()
        }
        //this.right.addKeyListenerInternal(delListener)
    }

    constructor(parent: Composite, element: Any?, operator: String, left: (EditorWidget) -> Expression) :
            this(parent, element, operator, left, { SimpleExpressionWidget(it, "expression", element) })

    override fun setFocus(): Boolean = left.setFocus()

    fun focusRight() = right.setFocus()


    override fun addKeyListenerInternal(listener: KeyListener) {
        operator.addKeyListenerInternal(listener)
    }

    override fun layoutInternal() {
        layout()
    }

    override fun substitute(current: Expression, newExpression: Expression) {
        if (left === current) {
            (newExpression as Control).moveAbove(left as Control)
            left.dispose()
            left = newExpression
        } else if (right === current) {
            (newExpression as Control).moveBelow(right as Control)
            right.dispose()
            right = newExpression
        }
        focusRight()
    }

    override fun copyTo(parent: EditorWidget): Expression =
        InfixExpressionWidget(parent, programElement, operator.text) { left.copyTo(it) }

    override fun toString() = left.toString() + operator + right

}