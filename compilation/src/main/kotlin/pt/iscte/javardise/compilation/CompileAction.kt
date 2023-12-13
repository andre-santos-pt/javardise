package pt.iscte.javardise.compilation

import com.github.javaparser.Position
import com.github.javaparser.ast.Node
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.Command
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.traverse


class CompileAction : Action {

    override val name: String = "Activate continuous compilation"

    override val iconPath: String = "process.png"

    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    val errorRemovers = mutableListOf<() -> Unit>()

    var commandObserver: ((Command, Boolean) -> Unit)? = null


    override fun run(editor: CodeEditor, toggle: Boolean) {
        if(toggle) {
            commandObserver = { _: Command, _ : Boolean ->
                compile(editor)
            }
            editor.addCommandObserver(commandObserver!!)
            compile(editor)
        }else {
            commandObserver?.let {
                editor.removeCommandObserver(it)
                errorRemovers.forEach { it() }
                errorRemovers.clear()
                editor.setFileErrors(emptySet())
            }
        }
    }

    private fun compile(editor: CodeEditor) {
        errorRemovers.forEach { it() }
        errorRemovers.clear()

//        val files = editor.folder.listFiles()?.filter { it.extension == "java" }?.map {
//                CompilationItem(it)
//        } ?: emptyList()

        val result = compileNoOutput(editor.folder)
        val filesWithErrors = result.first.map { (it.source as JavaSourceFromString).file }.toSet()
        editor.setFileErrors(filesWithErrors)

        result.first.forEach {
            val srcFile = (it.source as JavaSourceFromString).file
            println("${srcFile} ${it.startPosition}  ${it.endPosition} ${it.code}")

            val begin = Position(it.lineNumber.toInt(), it.columnNumber.toInt())
            val end = Position(begin.line, begin.column + (it.endPosition - it.startPosition - 1).toInt())

            val classWidget = editor.getClassWidget(srcFile)
            var w = classWidget?.findLastChild { c ->
                c is NodeWidget<*> && c.node is Node &&
                        (c.node as Node).range.getOrNull?.begin == begin
                               // (c.node as Node).range.getOrNull?.end == begin)
            }

            if(w == null)
                w =  classWidget?.findLastChild { c ->
                    c is NodeWidget<*> && c.node is Node &&
                            (c.node as Node).range.getOrNull?.contains(begin) ?: false
                }

            if (w is NodeWidget<*>)
                errorRemovers.add(w.markError(it.getMessage(null) + "\n" + it.kind + "\n" + it.code))
            else
                println("widget not found")
        }
    }

    fun NodeWidget<*>.markError(msg: String): () -> Unit {
        val prev = control.background
        control.traverse { c ->
            c.background = Color(Display.getDefault(),255, 207, 204)
            c.toolTipText = msg
            true
        }

        return {
            if (!control.isDisposed)
                control.traverse { c ->
                    c.background = prev
                    c.toolTipText = ""
                    true
                }
        }
    }
}

fun Composite.findLastChild(accept: (Control) -> Boolean): Control? {
    var n: Control? = null
    traverse {
        if (accept(it)) {
            n = it
        }
        return@traverse true
    }
    return n
}