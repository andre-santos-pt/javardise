package javawidgets

import basewidgets.*
import button
import column
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.visitor.TreeVisitor
import com.github.javaparser.ast.visitor.VoidVisitor
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.printer.configuration.PrinterConfiguration
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import compile
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.api.ICodeDecoration
import pt.iscte.javardise.api.scrollable
import row
import java.io.File
import java.io.PrintWriter
import java.util.function.BiFunction


fun main(args: Array<String>) {
    //require(args.isNotEmpty()) {
    //    "file argument  missing"
    //}
    val file = if (args.isEmpty()) null else File(args[0])
    //require(file.exists()) {
    //    "file $file does not exist"
    //}

    val window = JavardiseWindow(file)
    window.open()
}


val ERROR_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_RED) }

val BACKGROUND_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND) }

val COMMENT_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_GREEN) }

class JavardiseWindow(var file: File?) {

    var model: ClassOrInterfaceDeclaration? = null //= loadModel(file)

    private val display = Display()
    private val shell = Shell(display)


    var classWidget: Composite? = null

    var srcText: Text
    var col: Composite
    lateinit var stackComp: Composite


    init {
        //shell.text = model.types[0].name.id
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)

        col = form.column {
            row {
                addButtons(this)
            }
        }


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

        Commands.observers.add {
            srcText.text = model.toString()
            stackComp.children.forEach { it.dispose() }
            Commands.stack.forEach {
                Label(stackComp, SWT.BORDER).text = it.asString()
            }
            stackComp.requestLayout()
        }
    }

    private fun addButtons(
        composite: Composite,
    ) {
        composite.button("test") {
            //model.types[0].methods[1].parameters.removeAt(2)
            //  model.types[0].methods[1].parameters.add(1, Parameter(PrimitiveType(PrimitiveType.Primitive.CHAR), SimpleName("character")))
            //                    model.types[0].addMethod("testM")
            //                    model.types[0].addField("int", "f", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
            //                    model.types[0].members.add(
            //                        0, FieldDeclaration(
            //                            NodeList(Modifier.publicModifier(), Modifier.finalModifier()), NodeList(), NodeList(
            //                                VariableDeclarator(StaticJavaParser.parseType("String"), "s")
            //                            )
            //                        )
            //                    )
        }
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
                val offset = n.asString().length
                val init = printer.cursor.column - offset + 1 // because cursor.column is zero-based
                val pos = Token(printer.cursor.line.toLong(), init.toLong(), offset, n)
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
                object : TreeVisitor() {
                    override fun process(node: Node) {
                        // zero-based in java compiler
                        val t = nodeMap.find { it.line == e.lineNumber && it.col == e.columnNumber + 1 }
                        t?.let {
                            val child = classWidget!!.findChild(t.node)
                            child?.let {
                               child.background = Display.getDefault().getSystemColor(SWT.COLOR_RED)
                            }
                        }
                    }
                }.visitBreadthFirst(model)
            }
        }
    }


    private fun save() {
        if (file == null)
            file = File(model!!.nameAsString + ".java")
        val pw = PrintWriter(file)
        pw.print(model.toString())
        pw.close()
    }

    private fun load(file: File?): ClassOrInterfaceDeclaration? {
        classWidget?.dispose()

        model = if (file?.exists() == true) {
            val cu = loadModel(file)
            cu.findPublicClass()
        } else {
            val cu = CompilationUnit()
            cu.addClass("Test")
        }

        classWidget = col.scrollable {
            model?.let { c ->
                ClassWidget(it, c)
            } ?: notFoundLabel(it)
        }

        model!!.observeProperty<SimpleName>(ObservableProperty.NAME) {
            println("TYPE!! $it")
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
    fun newTokenWidget(
        parent: Composite, keyword: String,
        alternatives: () -> List<String> = { emptyList() },
        editAtion: (String) -> Unit = {}
    ): TokenWidget {
        val w = TokenWidget(parent, keyword, alternatives, editAtion)
        w.widget.foreground = Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
        return w
    }
}


abstract class NodeWidget<T>(parent: Composite, style: Int = SWT.NONE) : Composite(parent, style) {
    abstract val node: T

    abstract fun setFocusOnCreation()


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
        } else if (it is Text && it.data === model) {
            n = it
            return@traverse false
        } else
            return@traverse true
    }
    return n
}

fun Control.backgroundDefault() = this.traverse {
    background = Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
    foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE)
    true
}

class SimpleNameWidget<N : Node>(parent: Composite, node: N, getName: (N) -> String)
    : Id(parent, getName(node), ID_CHARS, {
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
}

class SimpleTypeWidget<N : Node>(parent: Composite, node: N, getName: (N) -> String)
    : TypeId(parent, getName(node)) {
    init {
        textWidget.data = node
    }
}