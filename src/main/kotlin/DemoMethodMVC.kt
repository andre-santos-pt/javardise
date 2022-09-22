import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ExpressionStmt
import javawidgets.MethodWidget
import javawidgets.loadModel
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.api.fillGridHorizontal
import pt.iscte.javardise.api.scrollable
import java.io.File

/*
Opens two views for the editing same method of the model.
Any edits on either window will be reflected on the other.
 */
fun main() {
    val model = loadModel(File("src/test/kotlin/TestExample.java"))
    val method = model.types[0].methods[0]

    val display = Display()

    val shell1 = createShell(display, method, false)
    shell1.open()

    val shell2 = createShell(display, method, true)
    shell2.open()

    while (!shell1.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}

private fun createShell(display: Display, model: MethodDeclaration, readonly: Boolean): Shell {
    val shell = Shell(display)
    shell.layout = FillLayout()
    shell.text = (model.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString
    val methodWidget = shell.column {
       layout = FillLayout()
        val w = scrollable {
            MethodWidget(it, model, SWT.BORDER)
        }
        w.enabled = readonly
        grid(2) {

            label("node")
            val t = text("") {
                enabled = false
                fillGridHorizontal()
            }

            label("detail")
            val detail = text("") {
                enabled = false
                fillGridHorizontal()
            }


            label("part")
            val tt = text("") {
                enabled = false
                fillGridHorizontal()
            }
            w.addObserver { node, data ->
                t.text = node?.let { node::class.simpleName } ?: ""
                //detail.text = if(node is ExpressionStmt) node.expression::class.simpleName else ""

                tt.text = data?.let { data.toString() } ?: ""
            }
        }

    }
    // add dispose listener
    methodWidget.shell.pack()
    return shell
}