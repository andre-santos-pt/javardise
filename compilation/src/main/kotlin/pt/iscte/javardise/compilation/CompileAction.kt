package pt.iscte.javardise.compilation

import com.github.javaparser.Position
import com.github.javaparser.ast.Node
import org.eclipse.swt.SWT
import org.eclipse.swt.events.DisposeListener
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.Command
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.traverse
import pt.iscte.javardise.isWindows
import pt.iscte.javardise.widgets.expressions.*
import pt.iscte.javardise.widgets.members.MemberWidget
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.javardise.widgets.statements.StatementWidget
import javax.tools.Diagnostic
import kotlin.concurrent.thread

const val SYMBOL_ERROR = "compiler.err.cant.resolve.location"
const val DUPID_ERROR = "compiler.err.already.defined"

class CompileAction : Action {

    override val name: String = "Activate continuous compilation"

    override val iconPath: String = "process.png"

    val ERROR_COLOR = if(Display.isSystemDarkTheme() && !isWindows)
        Color(Display.getDefault(), 150, 50, 50)
    else
        Color(Display.getDefault(), 255, 207, 204)

    val WARNING_COLOR = if(Display.isSystemDarkTheme() && !isWindows)
        Color(Display.getDefault(), 212, 196, 53)
    else
        Color(Display.getDefault(), 255, 241, 117)

    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    val errorRemovers = mutableListOf<() -> Unit>()

    var commandObserver: ((Command?, Boolean?) -> Unit)? = null


    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle) {
            commandObserver = { _: Command?, _: Boolean? ->
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
        thread {
            val result = compileNoOutput(editor.folder)
            val diagnostics = result.first.filter { it.source is JavaSourceFromString }
            val filesWithErrors = diagnostics.map { (it.source as JavaSourceFromString).file }.toSet()

            Display.getDefault().asyncExec {
                errorRemovers.forEach { it() }
                errorRemovers.clear()
                editor.setFileErrors(filesWithErrors)
            }
            diagnostics.forEach {
                val srcFile = (it.source as JavaSourceFromString).file
                println("$srcFile ${it.startPosition}  ${it.endPosition} ${it.code}")

                val begin = Position(it.lineNumber.toInt(), it.columnNumber.toInt())
                //val end = Position(begin.line, begin.column + (it.endPosition - it.startPosition - 1).toInt())

                Display.getDefault().asyncExec {
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

            is FieldAccessExpressionWidget -> if(c.node.scope.isThisExpr)
                c.tail
            else
                c.head

            is NewObjectExpressionWidget -> c.id

            is ExpressionWidget<*> -> c.head

            else -> null
        }

        textWidget?.let {
            it.widget.traverse { c ->
                c.background = if(msg.kind == Diagnostic.Kind.ERROR)
                    ERROR_COLOR
                else
                    WARNING_COLOR
                true
            }
            if(!textWidget.isEmpty) {
                val text = msg.getMessage(null).lines().first()
                tip = createToolTip(control.shell, text)
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
            if (textWidget?.widget?.isDisposed == false) {
                textWidget.widget.traverse { c ->
                    c.background = configuration.backgroundColor
                    true
                }
                tip?.dispose()
                textWidget.widget.removeFocusListener(listener)
            }
        }
    }
}

fun createToolTip(shell: Shell, msg: String) = ToolTip(shell, SWT.BALLOON).apply {
    text = msg
//    if(Display.isSystemDarkTheme()) {
//        val field = this::class.java.getDeclaredField("tip")
//        field.isAccessible = true
//        val tipInternal = field.get(this) as Shell
//        tipInternal.background = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND)
//        tipInternal.foreground = Display.getDefault().getSystemColor(SWT.COLOR_TITLE_FOREGROUND)
//    }
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

