package pt.iscte.javardise.examples

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.BodyDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addMark
import pt.iscte.javardise.basewidgets.addMark2
import pt.iscte.javardise.basewidgets.addMark3
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.loadCompilationUnit
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.findChild
import pt.iscte.javardise.widgets.members.ClassWidget

/*
Opens a shell with a class editor.
 */
fun main() {
    val src = """
public class Student {
	private int number;
	private String name;
	
	public Student(int number, String name) {
		this.number = number;
		this.name = name;
	}

	public int getNumber() {
		return number + 1;
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

    val cw = ClassWidget(shell, clazz)
    var mark: ICodeDecoration<*>? = null
    Button(shell, SWT.PUSH).apply {
        text = "add mark"
        addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val c = cw.findChild(clazz.methods.first { it.nameAsString == "getNumber" })
                mark = c?.addMark3(Display.getDefault().getSystemColor(SWT.COLOR_GREEN), "???")
                mark?.show()
            }
        })
    }

    Button(shell, SWT.PUSH).apply {
        text = "delete mark"
        addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                mark?.delete()
            }
        })
    }

    shell.pack()
    shell.open()


    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
