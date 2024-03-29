package pt.iscte.javardise.examples

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.BodyDeclaration
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.loadCompilationUnit
import pt.iscte.javardise.external.scrollable
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
    shell.scrollable {
        ClassWidget(it, clazz)
    }
    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
