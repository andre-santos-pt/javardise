package pt.iscte.javardise.tests

import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import pt.iscte.javardise.basewidgets.TokenWidget

abstract class SWTTest(val speed: Int) {

    private val display: Display = Display.getDefault()
    private val shell: Shell = Shell(display)

    init {
        shell.layout = FillLayout()
        addContent(shell)
        shell.pack()
        shell.open()
    }

    var stepIndex = 0

    fun step(step: (Control?) -> Unit) {
        display.timerExec((stepIndex + 1) * speed) {
            step(display.focusControl)
            if(!shell.isDisposed)
                shell.pack()
        }
        stepIndex++
    }

    val focusWidget = display.focusControl as? TokenWidget

    abstract fun addContent(shell: Shell)

    @AfterEach
    fun waitForTests() {
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }

    fun terminate() {
        step {
            shell.dispose()
        }
    }
}