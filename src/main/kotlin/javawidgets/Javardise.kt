package javawidgets

import basewidgets.TokenWidget
import button
import column
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.visitor.TreeVisitor
import com.github.javaparser.ast.visitor.VoidVisitor
import compile
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import row
import java.io.File
import java.io.PrintWriter


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

        composite.button("compile") {
            save()
            load(file)
            val errors = compile(file!!)
            val newParse = StaticJavaParser.parse(file!!)

            for (e in errors) {
                println("line ${e.lineNumber} ${e.columnNumber}")
                val len = e.endPosition - e.startPosition
                println("len $len")
                object : TreeVisitor() {
                    override fun process(node: Node) {
                        val line = node.begin.get().line.toLong()
                        val col = node.begin.get().column.toLong()
                        if(line == e.lineNumber && col == e.columnNumber && node.toString().length.toLong() == len) {
                            println("ERROR " + node.toString() + "  " + node.begin.get().toString() + " " + node::class)
                            val child = classWidget!!.findChild(node)
                            child?.let {
                                child.background = Display.getDefault().getSystemColor(SWT.COLOR_RED)
                            }
                        }
                    }
                }.visitBreadthFirst(newParse)
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

fun <T : Composite> Composite.scrollable(create: (Composite) -> T): Composite {
    val scroll = ScrolledComposite(this, SWT.H_SCROLL or SWT.V_SCROLL)
    scroll.layout = GridLayout()
    scroll.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    scroll.setMinSize(100, 100)
    scroll.expandHorizontal = true
    scroll.expandVertical = true

    val content = create(scroll)
    scroll.content = content

    val list = PaintListener {
        if (!scroll.isDisposed) {
            val size = computeSize(SWT.DEFAULT, SWT.DEFAULT)
            scroll.setMinSize(size)
            scroll.requestLayout()
        }
    }
    addPaintListener(list)
    addDisposeListener {
        removePaintListener(list)
    }
    return scroll
}

object Commands {
    val stack = ArrayDeque<Command>()
    val observers = mutableListOf<(Command) -> Unit>()

    fun execute(c: Command) {
        c.run()
        stack.addLast(c)
        observers.forEach {
            it(c)
        }
    }

    fun undo() {
        if (stack.isNotEmpty()) {
            val cmd = stack.removeLast()
            cmd.undo()
            observers.forEach {
                it(cmd)
            }
        }
    }

    fun reset() {
        stack.clear()
        observers.clear()
    }
}

//fun <N:Node> Node.runCommand(kind: CommandKind, action: () -> Unit): (N, () -> Unit) -> Unit =
//    { element: N, undoAction: () -> Unit ->
//        Commands.execute(object : AbstractCommand<N>(this, kind, element) {
//            override fun run() {
//                action()
//            }
//
//            override fun undo() {
//                undoAction()
//            }
//        })
//    }


enum class CommandKind {
    ADD, REMOVE, MODIFY
}

interface Command {
    val target: Node
    val kind: CommandKind
    val element: Node
    fun run()
    fun undo()

    fun asString(): String = "$kind - ${target::class.simpleName}"

}

abstract class AbstractCommand<E : Node>(
    override val target: Node,
    override val kind: CommandKind,
    override val element: E
) : Command


abstract class ModifyCommand<E : Node>(target: Node, previous: E?) :
    AbstractCommand<E>(target, CommandKind.MODIFY, previous?.clone() as E)


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

object Clipboard {
    var onCopy: Pair<Node, (Node, Node, Int?) -> Unit>? = null
    // var cut: Boolean = false

    fun copy(node: Node, copy: (Node, Node, Int?) -> Unit) {
        onCopy = Pair(node, copy)
    }

//    fun cut(node: Node, copy: (Node,Node, Int?) -> Unit) {
//        copy(node, copy)
//        cut = true
//    }

    fun paste(block: BlockStmt, index: Int) {
        Commands.execute(AddStatementCommand(onCopy!!.first.clone() as Statement, block, index))
    }
}

//object Colors {
//    fun get(c: Control) : Color =
//        when(c) {
//            is TokenWidget -> Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
//            else -> Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
//        }
//}
abstract class NodeWidget<T>(parent: Composite, style: Int = SWT.NONE) : Composite(parent, style) {
    abstract val node: T

    abstract fun setFocusOnCreation()


}

fun Control.traverse(visit: (Control) -> Boolean) {
    val enter = visit(this)
    if(this is Composite && enter)
        this.children.forEach { it.traverse(visit) }
}

fun Composite.findChild(model: Node): NodeWidget<*>? {
    var n : NodeWidget<*>? = null
    traverse {
        if(it is NodeWidget<*> && it.node === model) {
            n = it
            return@traverse false
        }
        else
            return@traverse true
    }
    return n
}