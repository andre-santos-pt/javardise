package javawidgets

import button
import column
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.*
import row
import java.io.File


fun main(args: Array<String>) {
    require(args.isNotEmpty()) {
        "file argument  missing"
    }
    val file = File(args[0])
    require(file.exists()) {
        "file $file does not exist"
    }
    val model = loadModel(file)
    val window = JavardiseWindow(model)
    window.open()
}


class JavardiseWindow(model: CompilationUnit) {

    private val display = Display()
    private val shell = Shell(display)

    private lateinit var table: Table

    lateinit var srcText: Text
    val executor = CommandExecutor()

    init {
        shell.text = model.types[0].name.id
        shell.layout = FillLayout()
        val form = SashForm(shell, SWT.HORIZONTAL)

        form.column {
            row {
                button("test") {
                    model.types[0].addMethod("testM")
                    model.types[0].addField("int", "f", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
                    model.types[0].members.add(
                        0, FieldDeclaration(
                            NodeList(Modifier.publicModifier(), Modifier.finalModifier()), NodeList(), NodeList(
                                VariableDeclarator(StaticJavaParser.parseType("String"), "s")
                            )
                        )
                    )
                }
                button("save") {
                    srcText.text = model.toString()
                }
                button("undo") {
                    executor.undo()
                }
            }
            val w = ClassWidget(this, model.types[0], executor)
        }

        val textArea = Composite(form, SWT.NONE)
        textArea.layout = FillLayout()
        srcText = Text(textArea, SWT.MULTI)

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



