package javawidgets

import basewidgets.TokenWidget
import button
import column
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
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


    lateinit var classWidget: ClassWidget

    lateinit var srcText: Text

    init {
        shell.text = model.types[0].name.id
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)



        form.column {
            row {
                addButtons(this, file, this@column)
            }

            scrollable {
                ClassWidget(it, model.types[0] as ClassOrInterfaceDeclaration) // TODO cast!
            }
        }

        val textArea = Composite(form, SWT.NONE)
        textArea.layout = FillLayout()
        srcText = Text(textArea, SWT.MULTI)
        srcText.text = model.toString()

        Commands.observers.add { srcText.text = model.toString() }

//        display.addFilter(SWT.KeyDown) {
//            println(it)
//            if(it.stateMask and SWT.CTRL != 0 && it.character == 'z') {
//                executor.undo()
//            }
//        }
    }

    private fun addButtons(
        composite: Composite,
        file: File,
        composite0: Composite
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
            classWidget = ClassWidget(composite0, model.types[0] as ClassOrInterfaceDeclaration)
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

fun Composite.scrollable(create: (Composite) -> Composite) {
    val scroll = ScrolledComposite(this, SWT.H_SCROLL or SWT.V_SCROLL)
    scroll.layout = GridLayout()
    scroll.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    scroll.setMinSize(100, 100)
    scroll.expandHorizontal = true
    scroll.expandVertical = true

    scroll.content = create(scroll)

    addPaintListener {
        val size = computeSize(SWT.DEFAULT, SWT.DEFAULT)
        scroll.setMinSize(size)
        scroll.requestLayout()
    }
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
}

interface Command {
    fun run()
    fun undo()
}

object Factory {
    fun newTokenWidget(parent: Composite, keyword: String): TokenWidget {
        val w = TokenWidget(parent, keyword)
        w.widget.foreground = Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
        return w
    }
}

//object Colors {
//    fun get(c: Control) : Color =
//        when(c) {
//            is TokenWidget -> Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
//            else -> Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
//        }
//}
