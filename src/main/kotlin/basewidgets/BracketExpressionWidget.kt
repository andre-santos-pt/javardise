package basewidgets

import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite

class BracketExpressionWidget(
        parent: Composite,
        open: String,
        close: String,
        f: (Composite) -> Expression
) : EditorWidget(parent), Expression {
    private val expression: ExpressionWidget
    private val left: TokenWidget
    private val right: TokenWidget

    init {
        left = TokenWidget(this, open)
        expression = ExpressionWidget(this, f, null)
        right = TokenWidget(this, close)
        val delAction: () -> Unit = {
            val e = expression.copyTo(parent as EditorWidget)
            val p = getParent() as Expression
            if (p.isSubstitutable) (p as Expression.SubstitutableExpression).substitute(this, e)
            p.requestLayoutInternal()
            e.setFocus()
        }
        right.addDeleteListener(delAction)
        val lis = left.addDeleteListener(delAction)
        //expression.addKeyListener(lis)
        expression.setFocus()
    }

    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun addKeyListenerInternal(listener: KeyListener) {
       expression.addKeyListenerInternal(listener)
    }

    override fun requestLayoutInternal() {
        requestLayout()
    }

    override fun copyTo(parent: EditorWidget): Expression {
        TODO()
    }
}