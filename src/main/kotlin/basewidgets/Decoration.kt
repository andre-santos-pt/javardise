package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ControlAdapter
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import api.ICodeDecoration
import java.util.function.BiFunction

internal class Decoration<T : Control>(
    val target: Control,
    val f: BiFunction<Composite, Control, T>,
    val loc: BiFunction<Point, Point, Point>
) : ICodeDecoration<T> {

    private val shell: Shell
    private val contr: T

    init {
        val parent = target.shell
        shell = Shell(parent, SWT.NO_TRIM)
        shell.layout = FillLayout()
        contr = f.apply(shell, target)
        shell.pack()
        setLocation(target, loc, contr)
        parent.addControlListener(object : ControlAdapter() {
            override fun controlMoved(e: ControlEvent) {
                if (!shell.isDisposed) setLocation(target, loc, contr)
            }
            override fun controlResized(e: ControlEvent) {
                if (!shell.isDisposed) setLocation(target, loc, contr)
            }
        })
    }

    fun setAlpha(alpha: Int) {
        shell.alpha = alpha
    }

    fun setBackground(color: Color) {
        shell.background = color
    }

    override val control : T
    get() {
        return contr
    }

    private fun setLocation(
        target: Control,
        loc: BiFunction<Point, Point, Point>,
        c: Control
    ) {
        val targetSize = target.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        targetSize.x += 5
        targetSize.y += 5
        val decoratorSize = c.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        shell.location = target.toDisplay(loc.apply(targetSize, decoratorSize))
    }

    fun setText(text: String?) {
        if (control is Text) {
            (control as Text).text = text
            shell.pack()
        }
    }

    override fun show() {
        setLocation(target, loc, contr)
        shell.isVisible = true
    }

    override fun hide() {
        shell.isVisible = false
    }

    override fun delete() {
        shell.dispose()
    }

    override fun locationHeight() = shell.location.y


}