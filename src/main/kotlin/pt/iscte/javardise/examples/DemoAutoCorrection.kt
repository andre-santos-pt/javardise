package pt.iscte.javardise.examples

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.ReturnStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.loadCompilationUnit
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.javardise.widgets.statements.EmptyStatementWidget


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
    val classWidget = shell.scrollable {
        ClassWidget(it, clazz)
    }
    classWidget.setAutoScroll()

    setOfflineCorrection(model)

    setOnlineCorrection(display)

    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}


// sempre que se muda a variavel da expressao do returt, altera para o texto novo mais "teste"
private fun setOfflineCorrection(model: CompilationUnit) {
    model.register(object : AstObserverAdapter() {
        var modified: Any? = null
        override fun propertyChange(
            observedNode: Node,
            property: ObservableProperty,
            oldValue: Any,
            newValue: Any
        ) {
            // para nao propagar eventos infinitamente
            if (newValue === modified)
                return

            println("$property $observedNode")
            if (newValue is NameExpr) {
                if (observedNode is ReturnStmt) {
                    modified = NameExpr(newValue.nameAsString + "teste")
                    observedNode.setExpression(modified as NameExpr)
                }
                // TODO outros tipos
            }
        }
        // com esta conf, a observacao eh feita em qualquer dos filhos
    }, Node.ObserverRegistrationMode.SELF_PROPAGATING)
}


// captura as teclas pressionadas na janela, quando eh um 'a' substitui por 's'
private fun setOnlineCorrection(display: Display) {
    display.addFilter(SWT.KeyDown) {
        // apenas para widgets de texto
        if (it.widget is Text) {
            val text = it.widget as Text
            val parent = text.parent

            // contexto de statement
            if (parent is EmptyStatementWidget) {
                println("${it.character}")

                if (it.character == 'a') {
                    it.doit = false // cancela o evento

                    // emite um evento de tecla programaticamente
                    val e = Event()
                    e.type = SWT.KeyDown
                    e.character = 's'
                    display.post(e)
                }
            }
            // TODO outros contextos
        }
    }
}
