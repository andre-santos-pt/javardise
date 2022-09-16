import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import javawidgets.MethodWidget
import javawidgets.loadModel
import org.eclipse.swt.SWT
import org.eclipse.swt.internal.cocoa.OS
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import java.io.File

fun main() {
    var model = loadModel(File("src/test/kotlin/TestExample.java"))

    val display = Display()
    val shell1 = openShell(display, model.types[0].methods[0])

    openShell(display, model.types[0].methods[0])

    while (!shell1.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}

private fun openShell(display: Display, model: MethodDeclaration): Shell {
    val shell = Shell(display)
    shell.layout = FillLayout()
    shell.text = (model.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString
    shell.column {
        val methodWidget = MethodWidget(this, model, SWT.BORDER)
        button("on focus") {
            println(methodWidget.getNodeOnFocus())
        }
    }
       shell.pack()
    shell.open()
    return shell
}