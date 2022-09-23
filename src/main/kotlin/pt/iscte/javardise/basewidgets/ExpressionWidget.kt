package pt.iscte.javardise.basewidgets

import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite

class ExpressionWidget(
    parent: Composite,
    f: (EditorWidget) -> Expression,
    e: Any?
) : EditorWidget(parent, e), Expression.SubstitutableExpression {

    var expression: Expression
        private set

    init {
        expression = f(this)
    }

    override fun substitute(current: Expression, newExpression: Expression) {
        current.dispose()
        expression = newExpression
        expression.layoutInternal()
        layout()
    }

    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun layoutInternal() {
        expression.layoutInternal()
    }

    override fun addKeyListenerInternal(listener: KeyListener) {
        expression.addKeyListenerInternal(listener)
    }


    override fun copyTo(parent: EditorWidget): Expression {
        return expression.copyTo(parent)
    }

    val isEmpty: Boolean
        get() = expression is SimpleExpressionWidget && (expression as SimpleExpressionWidget).isEmpty

    override fun toString() = expression.toString()

}