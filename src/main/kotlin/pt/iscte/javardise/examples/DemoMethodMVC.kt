package pt.iscte.javardise.examples

import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.MethodWidget

/*
Opens two views for the editing same method of the model.
Any edits on either window will be reflected on the other.
 */


fun main() {
    val src = """
    public static int fact(int n) {
        if(n == 1)
            return 1;
        else
            return n * fact(n-1);

    }
""".trimIndent()

    val method = loadMethod(src)
    val display = Display()

    val shell1 = createShell(display, method, true)
    shell1.open()

    val shell2 = createShell(display, method, true)
    shell2.open()

    while (!shell1.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}

private fun createShell(display: Display, model: MethodDeclaration, editable: Boolean): Shell {
    val shell = Shell(display)
    shell.layout = FillLayout()
    val methodWidget = shell.column {
       layout = FillLayout()
        val methodWidget = scrollable {
            MethodWidget(it, model, style = SWT.BORDER)
        }
        methodWidget.enabled = editable
        grid(2) {

            label("node")
            val nodeText = text("") {
                enabled = false
                fillGridHorizontal()
            }

            label("detail")
            val detailText = text("") {
                enabled = false
                fillGridHorizontal()
            }
            methodWidget.addObserver { node, data ->
                nodeText.text = node?.let { node::class.simpleName } ?: ""
                detailText.text = data?.let { data.toString() } ?: ""
            }

            button("on focus") {
                val child = methodWidget.getChildNodeOnFocus()
                message {
                    column {
                        child?.let {
                            label(child::class.simpleName!!)
                        }
                        label(child?.toString() ?: "no selection")
                    }

                }
            }
        }

    }
    // add dispose listener
    methodWidget.shell.pack()
    return shell
}