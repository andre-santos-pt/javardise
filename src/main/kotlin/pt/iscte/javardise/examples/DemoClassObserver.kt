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
        label("member node")
        val memberNode = text("") {
            enabled = false
            fillGridHorizontal()
        }

        label("statement node")
        val statementNode = text("") {
            enabled = false
            fillGridHorizontal()
        }

        label("focus node")
        val focusNode = text("") {
            enabled = false
            fillGridHorizontal()
        }

        classWidget.addFocusObserver { member, statement, node ->
            memberNode.text =
                if (member == null) "" else member::class.simpleName
            statementNode.text =
                if (statement == null) "" else statement::class.simpleName
            focusNode.text =
                if (node == null) "" else node::class.simpleName

        }

        text("click to loose editor focus") {
            fillGridHorizontal()
        }
    }

    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
