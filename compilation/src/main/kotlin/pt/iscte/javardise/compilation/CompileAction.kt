package pt.iscte.javardise.compilation

import com.github.javaparser.Position
import com.github.javaparser.ast.Node
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.ToolTip
import pt.iscte.javardise.Command
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.traverse
import pt.iscte.javardise.widgets.expressions.*
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.javardise.widgets.members.MemberWidget
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.javardise.widgets.statements.StatementWidget
import javax.tools.Diagnostic

const val SYMBOL_ERROR = "compiler.err.cant.resolve.location"
const val DUPID_ERROR = "compiler.err.already.defined"

class CompileAction : Action {

    override val name: String = "Activate continuous compilation"

    override val iconPath: String = "process.png"

    val ERROR_COLOR = Color(Display.getDefault(), 255, 207, 204)
    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    val errorRemovers = mutableListOf<() -> Unit>()

    var commandObserver: ((Command, Boolean) -> Unit)? = null


    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle) {
            commandObserver = { _: Command, _: Boolean ->
                compile(editor)
            }
            editor.addCommandObserver(commandObserver!!)
            compile(editor)
        } else {
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

        val result = compileNoOutput(editor.folder)
        val filesWithErrors = result.first.map { (it.source as JavaSourceFromString).file }.toSet()
        editor.setFileErrors(filesWithErrors)

        result.first.forEach {
            val srcFile = (it.source as JavaSourceFromString).file
            println("$srcFile ${it.startPosition}  ${it.endPosition} ${it.code}")

            val begin = Position(it.lineNumber.toInt(), it.columnNumber.toInt())
            val end = Position(begin.line, begin.column + (it.endPosition - it.startPosition - 1).toInt())

            val classWidget = editor.getClassWidget(srcFile)
            var w = classWidget?.findLastChild { c ->
                c is NodeWidget<*> && c.node is Node &&
                        (c.node as Node).range.getOrNull?.begin == begin
                // (c.node as Node).range.getOrNull?.end == begin)
            }

            if (w == null)
                w = classWidget?.findLastChild { c ->
                    c is NodeWidget<*> && c.node is Node &&
                            (c.node as Node).range.getOrNull?.contains(begin) ?: false
                }

            if (w is NodeWidget<*>)
                errorRemovers.add(w.markError(it))
            else
                println("widget not found")
        }
    }

    val Rectangle.dimension: Point get() = Point(width, height)

    fun NodeWidget<*>.markError(msg: Diagnostic<*>): () -> Unit {
        var tip: ToolTip? = null

        val textWidget = when (val c = control) {
            is MemberWidget<*> -> if (msg.code == SYMBOL_ERROR)
                c.type
            else
                c.name

            is MethodWidget.ParamListWidget.ParamWidget ->
                if (msg.code == SYMBOL_ERROR) c.type else c.name

            is AssignExpressionWidget -> c.target.tail

            is VariableDeclarationWidget -> if (msg.code == SYMBOL_ERROR)
                c.type
            else
                c.name

            is StatementWidget<*> -> c.keyword ?: c.tail

            is StringExpressionWidget -> c.text

            is ExpressionWidget<*> -> c.head

            else -> null
        }

        textWidget?.let {
            it.widget.traverse { c ->
                c.background = ERROR_COLOR
                true
            }
            tip = ToolTip(control.shell, SWT.BALLOON).apply {
                text = msg.getMessage(null)
            }
        }


        val listener: FocusListener? = textWidget?.let {
            object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    tip?.setLocation(it.widget.toDisplay(it.widget.bounds.dimension))
                    tip?.visible = true
                }

                override fun focusLost(e: FocusEvent) {
                    tip?.visible = false
                }
            }
        }

        val disposeListener = textWidget?.let {
            DisposeListener {
                if(tip?.isDisposed == false)
                    tip?.visible = false
            }
        }

        textWidget?.widget?.addFocusListener(listener)
        textWidget?.widget?.addDisposeListener(disposeListener)

        return {
            if (!control.isDisposed) {
                textWidget?.widget?.traverse { c ->
                    c.background = configuration.backgroundColor
                    true
                }
                tip?.dispose()
                textWidget?.widget?.removeFocusListener(listener)
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