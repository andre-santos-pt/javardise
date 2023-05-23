package pt.iscte.javardise.examples

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.BodyDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.loadCompilationUnit
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.javardise.widgets.members.FieldWidget
import pt.iscte.javardise.widgets.members.MethodWidget

/*
Opens a shell with a class editor.
 */
fun main() {
    val src = """
public class Figure {
	private Point location = new Point(0, 0);

    void setLocation (int x, int y) {
        location = new Point(x, y);
    }
}
""".trimIndent()

    val model = loadCompilationUnit(src)
    val clazz = model.findMainClass()!!

    val display = Display()
    val shell = Shell(display)

    shell.layout = GridLayout()

    FieldWidget(shell, clazz.getFieldByName("location").get(),
        configuration = object : DefaultConfiguration() {
            override val fontFace: String
                get() = "Arial"
            override val fontSize: Int
                get() = 16

        })

    MethodWidget(shell, clazz.getMethodsByName("setLocation").first(),
        configuration = object : DefaultConfiguration() {
            override val backgroundColor: Color?
                get() = Display.getDefault().getSystemColor(SWT.COLOR_GRAY)
        })

    ClassWidget(shell, clazz, configuration = object : DefaultConfiguration() {
        override val fontFace: String
            get() = "Monospaced"
        override val fontSize: Int
            get() = 14

        override val keywordColor: Color
            get() = Display.getDefault().getSystemColor(SWT.COLOR_BLUE)

        override val tabLength: Int
            get() = 0
    })


    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
