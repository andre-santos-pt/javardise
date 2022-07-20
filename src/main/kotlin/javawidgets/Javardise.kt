package javawidgets

import basewidgets.TokenWidget
import button
import column
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
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

    private lateinit var table: Table

    lateinit var classWidget: ClassWidget

    lateinit var srcText: Text

    init {
        shell.text = model.types[0].name.id
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)

        form.column {
            row {
                button("test") {
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
                button("save") {
                    val pw = PrintWriter(file)
                    pw.print(model.toString())
                   pw.close()
                }

                button("load") {
                    classWidget.dispose()
                    model = loadModel(file)
                    // TODO several types
                    classWidget = ClassWidget(this@column, model.types[0] as ClassOrInterfaceDeclaration)
                    requestLayout()
                }

                button("undo") {
                    Commands.undo()
                }
            }
            classWidget = ClassWidget(this, model.types[0] as ClassOrInterfaceDeclaration) // TODO cast!
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


    fun open() {
        shell.pack()
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
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
    fun newTokenWidget(parent: Composite, keyword: String) : TokenWidget {
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
