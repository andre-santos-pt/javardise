package pt.iscte.javardise.examples

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
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
    override val control: Control = this

    init {
        layout = ROW_LAYOUT_V_SPACED
        bodyWidget = createBlockSequence(this, body, tabs = 0)
        addUndoSupport(SWT.MOD1, 'z'.code)
        closingBracket = TokenWidget(this, " ")
        observeListUntilDispose(body.statements, object : ListObserver<Statement> {
            override fun elementRemove(
                list: NodeList<Statement>,
                index: Int,
                node: Statement
            ) {
                if(list.size == 1)
                    list.add(body.empty())
            }
        })
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val configuration: Configuration
        get() = DefaultConfigurationSingleton

    override val commandStack: CommandStack = CommandStack.nullStack
}






