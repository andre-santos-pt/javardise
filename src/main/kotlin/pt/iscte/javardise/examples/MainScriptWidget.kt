package pt.iscte.javardise.examples

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.loadCompilationUnit
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.statements.SequenceContainer


fun main() {
    val src = """
public class Script {
	public static void main(String[] args) {
        int i = 0;
        while(i < 10) {
            System.out.println(i);
            i++;
        }
    }
}
""".trimIndent()

    val model = loadCompilationUnit(src)
    val clazz = model.findMainClass()!!

    val display = Display()
    val shell = Shell(display)

    shell.layout = FillLayout()
    shell.scrollable {
        val w = MainScriptWidget(it, clazz)
        w.commandStack.addObserver { _,_ ->
            println(clazz)
        }
        w

    }//.setAutoScroll()
    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}

class MainScriptWidget(
    parent: Composite,
    dec: ClassOrInterfaceDeclaration,
    override val node: MethodDeclaration = dec.methods.find {
        it.nameAsString == "main"
    } ?: dec.addMethod("main")

) : ObserverWidget<MethodDeclaration>(parent), SequenceContainer<MethodDeclaration>,
ConfigurationRoot {

    override val body: BlockStmt = node.body.get()

    override val bodyWidget: SequenceWidget

    override val closingBracket: TextWidget
        get() = TODO("Not yet implemented")

    override val control: Control = this

    init {
        layout = ROW_LAYOUT_H_SHRINK
        bodyWidget = createBlockSequence(this, body, tabs = 0)
        addUndoSupport(SWT.MOD1, 'z'.code)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val configuration: Configuration
        get() = DefaultConfigurationSingleton

    override val commandStack: CommandStack = CommandStack.create()
}






