package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label

class FixedToken(parent: Composite, token: String) {

    val label = Label(parent, SWT.NONE)

    init {
        label.font = TextWidget.f
        label.text = token
        label.background = parent.background
        label.foreground = parent.foreground
    }
    override fun toString(): String = label.text

    fun addKeyListener(keyListener: KeyListener) = label.addKeyListener(keyListener)

    fun dispose() = label.dispose()
    fun setFocus() = label.setFocus()

    fun moveAbove(c: Control) = label.moveAbove(c)
    fun moveBelow(c: Control) = label.moveBelow(c)

}