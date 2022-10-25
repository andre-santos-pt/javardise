package pt.iscte.javardise

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.Observable
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.printer.configuration.PrinterConfiguration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.PrintWriter
import javax.lang.model.SourceVersion


fun main(args: Array<String>) {
    val file = if (args.isEmpty()) File("Unnamed.java") else File(args[0])
    val window = JavardiseWindow(file)
    window.open()
}



class JavardiseWindow(var file: File) {

    var model: ClassOrInterfaceDeclaration? = null

    private val display = Display()
    private val shell = Shell(display)


    var classWidget: Composite? = null

    var srcText: Text
    var col: Composite
    lateinit var stackComp: Composite


    init {
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)
        col = form.column { }
        val sash = SashForm(form, SWT.NONE)
        val textArea = Composite(sash, SWT.NONE)
        textArea.layout = FillLayout()
        srcText = Text(textArea, SWT.MULTI)
        srcText.text = model.toString()
        srcText.editable = false

        createStackView(sash)

        model = load(file)
        model?.let {
            val mirror = ClassWidget(form, it)
            mirror.enabled = false
        }
        col.row {
            addButtons(this)
        }

        // BUG lost focus
        display.addFilter(SWT.KeyDown) {
            if (it.stateMask == SWT.MOD1 && it.keyCode == 'z'.code) {
                Commands.undo()
            }
        }
    }

    fun notFoundLabel(p: Composite) =
        Composite(p, SWT.BORDER).apply {
            layout = FillLayout()
            Label(this, SWT.NONE).apply {
                text = "No public class found"
            }
        }

    private fun createStackView(parent: Composite) {
        stackComp = Composite(parent, SWT.NONE)
        stackComp.layout = RowLayout(SWT.VERTICAL)

        Commands.addObserver {
            srcText.text = model.toString()
            stackComp.children.forEach { it.dispose() }
            Commands.stackElements.forEach {
                Label(stackComp, SWT.BORDER).text = it.asString()
            }
            stackComp.requestLayout()
        }
    }

    private fun addButtons(
        composite: Composite,
    ) {
        composite.button("save") {
            save()
        }

        composite.button("load") {
            load(file)
        }

        composite.button("undo") {
            Commands.undo()
        }


        data class Token(val line: Long, val col: Long, val offset: Int, val node: Node)


        class TestVis2(val list: MutableList<Token>, conf: PrinterConfiguration) : DefaultPrettyPrinterVisitor(conf) {
            override fun visit(n: FieldDeclaration?, arg: Void?) {
                //println("F " + n)
                super.visit(n, arg)
            }

            override fun visit(n: SimpleName, arg: Void?) {
                super.visit(n, arg)
                addToken(n.parentNode.get())
            }

            override fun visit(n: NameExpr, arg: Void?) {
                super.visit(n, arg)
                addToken(n)
            }

            override fun visit(n: ArrayAccessExpr, arg: Void?) {
                super.visit(n, arg)
                addToken(n)
            }
            override fun visit(m: MethodDeclaration, arg: Void?) {
                super.visit(m, arg)
               // addToken(m.type)
               // addToken(m.name)
            }

            override fun visit(n: DoubleLiteralExpr, arg: Void?) {
                super.visit(n, arg)
                addToken(n)
                println("!! $n")
            }

            private fun addToken(n: Node) {
                val offset = n.toString().length
                val init =
                    printer.cursor.column - offset + 1 // because cursor.column is zero-based
                val pos = Token(
                    printer.cursor.line.toLong(),
                    init.toLong(),
                    offset,
                    n
                )
                list.add(pos)
            }
        }

        composite.button("compile") {
            classWidget!!.backgroundDefault()
            val nodeMap = mutableListOf<Token>()
            val printer = DefaultPrettyPrinter({ TestVis2(nodeMap, it) }, DefaultPrinterConfiguration())
            val src = printer.print(model)
            val errors = compile(model!!.nameAsString, src)

            for (e in errors) {
                println("ERROR line ${e.lineNumber} ${e.columnNumber} ${e.getMessage(null)}")
                // zero-based in java compiler
                val t = nodeMap.find { it.line == e.lineNumber && it.col == e.columnNumber }
                t?.let {
                    val child = classWidget!!.findChild(t.node)
                    child?.let {
                        child.traverse {
                            it.background = ERROR_COLOR()
                            true
                        }
                        child.toolTipText = e.getMessage(null)
                        child.requestLayout()
                    }
                }
                // TODO show not handled
            }
        }
    }


    private fun save() {
        val pw = PrintWriter(file)
        pw.print(model.toString())
        pw.close()
    }

    private fun load(file: File): ClassOrInterfaceDeclaration? {
        require(file.name.endsWith(".java")) {
            "Java file must have '.java' extension"
        }
        val typeName = file.name.dropLast(".java".length)

        require(SourceVersion.isIdentifier(typeName)) {
            "'$typeName' is not a valid identifier for a Java type"
        }
        classWidget?.dispose()

        model = if (file.exists()) {
            val cu = loadCompilationUnit(file)
            cu.findMainClass() ?:
                ClassOrInterfaceDeclaration(NodeList(), false, typeName)
        } else {
            val cu = CompilationUnit()
            cu.addClass(typeName)
        }

        classWidget = col.scrollable {
            model?.let { c ->
                ClassWidget(it, c)
            } ?: notFoundLabel(it)
        }

        model!!.observeProperty<SimpleName>(ObservableProperty.NAME) {
            shell.text = it?.toString() ?: "No public class found"
        }
        Commands.reset()
        val parent = stackComp.parent
        stackComp.dispose()
        createStackView(parent)
        shell.requestLayout()
        shell.text = model?.nameAsString ?: "No public class found"
        return model
    }


    fun open() {
        shell.size = Point(1000, 800)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }
}



object Factory {
    fun newKeywordWidget(
        parent: Composite, keyword: String,
        alternatives: () -> List<String> = { emptyList() },
        editAtion: (String) -> Unit = {}
    ): TokenWidget {
        val w = TokenWidget(parent, keyword, alternatives, editAtion)
        w.widget.foreground = KEYWORD_COLOR()
        return w
    }
}


interface NodeWidget<T> {
    val node: T

    fun setFocusOnCreation(firstFlag: Boolean = false)

    fun <T : Node> observeProperty(prop: ObservableProperty, event: (T?) -> Unit): AstObserver {
        val obs = object : PropertyObserver<T>(prop) {
            override fun modified(oldValue: T?, newValue: T?) {
                event(newValue)
            }
        }
        (node as Node).register(obs)
        return obs
    }

}



inline fun <reified T : NodeWidget<*>> Control.findAncestor(): T? {
    var w : Control? = this
    while(w !is T && w != null && w.data !is T)
        w = w.parent

    return w as? T
}

inline fun <reified T : Node> Control.findNode(): T? {
    var w : Control? = this
    while(!(w is NodeWidget<*> && w.node is T) && w != null && w.data !is T)
        w = w.parent

    return if(w is NodeWidget<*>) w.node as T else w?.data as? T
}

fun Control.traverse(visit: (Control) -> Boolean) {
    val enter = visit(this)
    if (this is Composite && enter)
        this.children.forEach { it.traverse(visit) }
}

fun Composite.findChild(model: Node): Control? {
    var n: Control? = null
    traverse {
        if (it is NodeWidget<*> && it.node === model) {
            n = it
            return@traverse false
        }

        if(it is Text && it.data != null)
            println(it.text + " " + it.data::class.java)

        if (it is Text && it.data === model) {
            n = it
            return@traverse false
        }
        return@traverse true
    }
    return n
}

fun Control.backgroundDefault() = this.traverse {
    background = BACKGROUND_COLOR()
    foreground = FOREGROUND_COLOR()
    true
}

class SimpleNameWidget<N : Node>(parent: Composite, override val node: N, getName: (N) -> String)
    : NodeWidget<N>, Id(parent, getName(node), ID_CHARS, {
        s ->
    try {
        StaticJavaParser.parseSimpleName(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
}) {
    init {
        textWidget.data = node
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        textWidget.setFocus()
    }

    override fun isValid(): Boolean = SourceVersion.isIdentifier(textWidget.text) && !SourceVersion.isKeyword(textWidget.text)

}

class SimpleTypeWidget<N : Node>(parent: Composite,  override val node: N, getName: (N) -> String)
    : TypeId(parent, getName(node)), NodeWidget<N> {
    init {
        textWidget.data = node
    }

    override fun isValid(): Boolean = isValidType(textWidget.text)
    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}

class UnsupportedWidget(parent: Composite, node: Node) : Composite(parent, SWT.NONE) {
    init {
        layout = FillLayout()
        val label = Label(this, SWT.NONE)
        label.text = node.toString()
        label.foreground = ERROR_COLOR()
        label
    }
}