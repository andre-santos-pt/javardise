package javawidgets

import basewidgets.TokenWidget
import button
import column
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import row
import java.io.File
import java.io.PrintWriter


fun main(args: Array<String>) {
    require(args.isNotEmpty()) {
        "file argument  missing"
    }
    val file = File(args[0])
    require(file.exists()) {
        "file $file does not exist"
    }

    val window = JavardiseWindow(file)
    window.open()
}


class JavardiseWindow(file: File) {

    var model = loadModel(file)

    private val display = Display()
    private val shell = Shell(display)


    lateinit var classWidget: Composite

    lateinit var srcText: Text
    lateinit var col: Composite
    lateinit var stackComp: Composite
    init {
        shell.text = model.types[0].name.id
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)

        col = form.column {
            row {
                addButtons(this, file)
            }

            classWidget = scrollable {
                ClassWidget(it, model.types[0] as ClassOrInterfaceDeclaration)
            }
        }

        val sash = SashForm(form, SWT.NONE)

        val textArea = Composite(sash, SWT.NONE)
        textArea.layout = FillLayout()
        srcText = Text(textArea, SWT.MULTI)
        srcText.text = model.toString()


        createStackView(sash)
        // BUG lost focus
        display.addFilter(SWT.KeyDown) {
            if(it.stateMask ==  SWT.MOD1 && it.keyCode == 'z'.code) {
                Commands.undo()
            }
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
        file: File
    ) {
        composite.button("test") {
            model.types[0].methods[1].parameters.removeAt(2)
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
            val pw = PrintWriter(file)
            pw.print(model.toString())
            pw.close()
        }

        composite.button("load") {
            classWidget.dispose()
            model = loadModel(file)

            // TODO several types

            classWidget = col.scrollable {
                ClassWidget(it, model.types[0] as ClassOrInterfaceDeclaration)
            }
            Commands.reset()
            val parent = stackComp.parent
            stackComp.dispose()
            createStackView(parent)
            requestLayout()
        }

        composite.button("undo") {
            Commands.undo()
        }
    }


    fun open() {
        shell.pack()
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }
}

fun <T:Composite> Composite.scrollable(create: (Composite) -> T) : Composite {
    val scroll = ScrolledComposite(this, SWT.H_SCROLL or SWT.V_SCROLL)
    scroll.layout = GridLayout()
    scroll.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    scroll.setMinSize(100, 100)
    scroll.expandHorizontal = true
    scroll.expandVertical = true

    val content = create(scroll)
    scroll.content = content

    val list: PaintListener = object  : PaintListener {
        override fun paintControl(e: PaintEvent?) {
            if(!scroll.isDisposed) {
                val size = computeSize(SWT.DEFAULT, SWT.DEFAULT)
                scroll.setMinSize(size)
                scroll.requestLayout()
            }
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
    override val element: E)
    : Command



abstract class ModifyCommand<E:Node>(target: Node, previous: E?)
    : AbstractCommand<E>(target, CommandKind.MODIFY, previous?.clone() as E)


object Factory {
    fun newTokenWidget(parent: Composite, keyword: String): TokenWidget {
        val w = TokenWidget(parent, keyword)
        w.widget.foreground = Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
        return w
    }
}

object Clipboard {
    var onCopy: Pair<Node,(Node, Node, Int?) -> Unit>? = null
   // var cut: Boolean = false

    fun copy(node: Node, copy: (Node,Node, Int?) -> Unit) {
        onCopy = Pair(node,copy)
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
abstract class NodeWidget<T>(parent: Composite, style: Int = SWT.NONE)
    : Composite(parent, style) {
    abstract val node: T

    abstract fun setFocusOnCreation()
}