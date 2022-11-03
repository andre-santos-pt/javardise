package pt.iscte.javardise.examples

import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.ClassWidget

/*
Opens a shell with a class editor. An observer is plugged in to react when focus changes.
 */
fun main() {
    val src = """
public class Student {
	private int number;
	private String name;
	
	public Student(int number, String name) {
		this.number = number;
		this.name = name;
        if(true) {
            i = 1;
            while(true) {
                j= 0;
                }
        }
	}

	public int getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}
}
""".trimIndent()

    val model = loadCompilationUnit(src)
    val clazz = model.findMainClass()!!

    val display = Display()
    val shell = Shell(display)

    shell.layout = FillLayout()
    val classWidget = shell.scrollable {
        ClassWidget(it, clazz)
    }
    shell.grid(2) {
        label("node class")
        val nodeClass = text("") {
            enabled = false
            fillGridHorizontal()
        }

        label("widget class")
        val widgetClass = text("") {
            enabled = false
            fillGridHorizontal()
        }
        label("detail from widget")
        val detailWidget = text("") {
            enabled = false
            fillGridHorizontal()
        }
        var dec : ICodeDecoration<*>? = null
        classWidget.addFocusObserver { node, widget ->
            nodeClass.text = if(node == null) "" else node::class.simpleName
            widgetClass.text = if(widget == null) "" else widget::class.simpleName
           // detailWidget.text = widget?.getNodeOnFocus()?.toString() ?: ""
            dec?.hide()
            //dec = widget?.addMark(Display.getDefault().getSystemColor(SWT.COLOR_GREEN))
            dec?.show()

        }
    }

    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
