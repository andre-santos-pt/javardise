package pt.iscte.javardise.tests

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.junit.jupiter.api.AfterEach
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.members.ClassWidget

abstract class SWTTest(val classModel: ClassOrInterfaceDeclaration, val speed: Int = TEST_SPEED) {

    private val display: Display = Display.getDefault()
    private val shell: Shell = Shell(display)
    private val code: Text
    internal val classWidget: ClassWidget
    constructor(src: String, speed: Int = TEST_SPEED)
            : this(StaticJavaParser.parse(src).findMainClass()!!, speed)

    init {
        shell.layout = FillLayout()
        val sash = SashForm(shell, SWT.HORIZONTAL)
        classWidget = sash.scrollable {
            ClassWidget(it, classModel)
        }
        code = Text(sash, SWT.MULTI)
        shell.pack()
        shell.open()
    }

    var stepIndex = 0
        private set

    fun step(step: (Control?) -> Unit) {
        display.timerExec((stepIndex + 1) * speed) {
            step(display.focusControl)
            if(!shell.isDisposed) {
                code.text = classModel.toString()
                shell.pack()
            }
        }
        stepIndex++

    }

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