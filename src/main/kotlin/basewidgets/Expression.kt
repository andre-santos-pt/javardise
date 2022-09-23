package basewidgets

import org.eclipse.swt.events.KeyListener

interface Expression {
    fun copyTo(parent: EditorWidget): Expression
    fun dispose()
    fun setFocus(): Boolean
    fun layoutInternal()
    fun addKeyListenerInternal(listener: KeyListener)
    val isSubstitutable: Boolean
        get() = this is SubstitutableExpression

    interface SubstitutableExpression : Expression {
        fun substitute(
            current: Expression,
            newExpression: Expression
        )
    }
}