package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.MenuItem

class PrefixExpressionWidget(
    parent: Composite,
    element: Any?,
    operator: String,
    f: (EditorWidget) -> Expression
) : EditorWidget(parent, element), Expression.SubstitutableExpression {

    private val operator: TokenWidget
    private var expression: Expression

    init {
        this.operator = TokenWidget(this, operator)
        expression = f(this)
        val delListener = this.operator.addDeleteListener {
            val e = expression.copyTo(parent as EditorWidget)
            val p = getParent() as Expression
            if (p.isSubstitutable)
                    (p as Expression.SubstitutableExpression).substitute(this, e)
            e.setFocus()
        }

        // TODO bug?
        expression.addKeyListenerInternal(delListener)
        val menu = this.operator.menu
        MenuItem(menu, SWT.SEPARATOR)
        val simple = MenuItem(menu, SWT.NONE)
        simple.text = "simple expression"
        simple.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                dispose() // TODO
            }
        })
    }

    override fun copyTo(parent: EditorWidget): Expression {
        return PrefixExpressionWidget(parent, programElement, operator.text) {
            expression.copyTo(it)
        }
    }

    override fun addKeyListenerInternal(listener: KeyListener) {
        operator.addKeyListenerInternal(listener)
    }

    override fun layoutInternal() {
        layout()
    }

    override fun substitute(
        current: Expression,
        newExpression: Expression
    ) {
        current.dispose()
        expression = newExpression
        expression.layoutInternal()
    }


}