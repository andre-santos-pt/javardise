import javawidgets.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

/*
Opens a shell with a class editor.
 */
fun main() {
    val src = """
public class TestExample {

    private String id;

    TestExample(String id) {

        if (id == null) {
            id = "N";
        }
    }

    public static int fact(int n) {
        int x = 7;
        while(x == 0) {
            fact(0);
        }
        if(n == 1) {
            if (n == 7)
                return 0;
            return 1;
        }
        else
            return n * fact(n-1);

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
        //Composite(it, SWT.BORDER)
   }
    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
