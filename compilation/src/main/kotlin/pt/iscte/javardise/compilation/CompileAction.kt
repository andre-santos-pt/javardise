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
import pt.iscte.javardise.*
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
import kotlin.concurrent.thread

const val SYMBOL_ERROR = "compiler.err.cant.resolve.location"
const val DUPID_ERROR = "compiler.err.already.defined"

class CompileAction : Action {

    override val name: String = "Activate continuous compilation"

    override val iconPath: String = "process.png"

    val ERROR_COLOR = if (Display.isSystemDarkTheme() && !isWindows)
        Color(Display.getDefault(), 150, 50, 50)
    else
        Color(Display.getDefault(), 255, 207, 204)

    val WARNING_COLOR = if (Display.isSystemDarkTheme() && !isWindows)
        Color(Display.getDefault(), 212, 196, 53)
    else
        Color(Display.getDefault(), 255, 241, 117)

    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    val errorRemovers = mutableListOf<Pair<Text, () -> Unit>>()

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
                errorRemovers.forEach { it.second() }
                errorRemovers.clear()
                editor.setFileErrors(emptySet())
            }
        }
    }

    private fun compile(editor: CodeEditor) {
        editor.debugRanges() // TODO tmp
        thread {
            val result = compileNoOutput(editor.folder)
            val diagnostics = result.first.filter { it.source is JavaSourceFromString }
            val filesWithErrors = diagnostics.map { (it.source as JavaSourceFromString).file }.toSet()

            Display.getDefault().asyncExec {
                editor.setFileErrors(filesWithErrors)
                val newErrors = mutableListOf<Pair<Text, () -> Unit>>()
                diagnostics.forEach {
                    val srcFile = (it.source as JavaSourceFromString).file
                    val token =
                        (it.source as JavaSourceFromString).code.substring(it.startPosition.toInt() until it.endPosition.toInt())
                    val begin = Position(it.lineNumber.toInt(), it.columnNumber.toInt())
                    val end = Position(begin.line, begin.column + (it.endPosition - it.startPosition - 1).toInt())
                    println("$srcFile ${it.startPosition}  ${it.endPosition} ${it.code} \"$token\" $begin $end")


                    val classWidget = editor.getClassWidget(srcFile)
                    val w = classWidget?.findChild { c ->
                        c is Text && c.data is Node && (c.data as Node).range.getOrNull?.begin == begin
                    } as? Text ?: classWidget?.findLastChild { c ->
                        c is Text && c.data is Node && (c.data as Node).range.getOrNull?.contains(begin) ?: false
                    } as? Text

                    if (w != null) {
                        newErrors.add(
                            Pair(w, w.markError(it, classWidget?.configuration ?: DefaultConfigurationSingleton))
                        )
                    } else
                        println("widget not found - $begin - $it")
                }
                val remove = errorRemovers.filter { old -> newErrors.none { it.first == old.first } }
                remove.forEach { it.second() }
                errorRemovers.removeAll(remove)
                errorRemovers.addAll(newErrors)
            }
        }
    }

    val Rectangle.dimension: Point get() = Point(width, height)

    fun Text.markError(msg: Diagnostic<*>, configuration: Configuration): () -> Unit {
        var tip: ToolTip? = getData("error") as? ToolTip

        if (tip?.isDisposed == false && tip.text == msg.getMessage(null))
            return { }

        traverse { c ->
            c.background = if (msg.kind == Diagnostic.Kind.ERROR)
                ERROR_COLOR
            else
                WARNING_COLOR
            true
        }

        if (text.isNotEmpty()) {
            val text = msg.getMessage(null)// msg.getMessage(null).lines().first()
            tip = createToolTip(shell, text)
            setData("error", tip)
        }

        val listener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                tip?.setLocation(toDisplay(bounds.dimension))
                tip?.visible = true
            }

            override fun focusLost(e: FocusEvent) {
                tip?.visible = false
            }
        }

        val disposeListener = DisposeListener {
            if (tip?.isDisposed == false)
                tip.visible = false
        }

        addFocusListener(listener)
        addDisposeListener(disposeListener)

        return {
            if (isDisposed == false) {
                traverse { c ->
                    c.background = configuration.backgroundColor
                    true
                }
                setData("error", null)
                tip?.dispose()
                removeFocusListener(listener)
            }
        }
    }


    private fun createToolTip(shell: Shell, msg: String) = ToolTip(shell, SWT.BALLOON).apply {
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

    fun Composite.findChild(accept: (Control) -> Boolean): Control? {
        var n: Control? = null
        traverse {
            if (accept(it) && n == null) {
                n = it
                return@traverse false
            }
            return@traverse true
        }
        return n
    }

    fun CodeEditor.debugRanges() {
        allClassWidgets().forEach {
            it?.traverse {
//            if (it is ExpressionWidget<*>) {
//                it.head.widget.toolTipText = (it.node as Node).range.getOrNull?.toString()
//            }
//            else
                if (it is Text) {
                    it.toolTipText = (it.data as? Node)?.toString() + "\n" + (it.data as? Node)?.range?.toString()
                }
                return@traverse true
            }
        }
    }

    fun NodeWidget<*>.markErrorOld(msg: Diagnostic<*>): () -> Unit {
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

            is FieldAccessExpressionWidget -> if (c.node.scope.isThisExpr)
                c.tail
            else
                c.head

            is NewObjectExpressionWidget -> c.id

            is ExpressionWidget<*> -> c.head

            else -> null
        }

        textWidget?.let {
            it.widget.traverse { c ->
                c.background = if (msg.kind == Diagnostic.Kind.ERROR)
                    ERROR_COLOR
                else
                    WARNING_COLOR
                true
            }
            if (!textWidget.isEmpty) {
                val text = msg.getMessage(null)// msg.getMessage(null).lines().first()
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
                if (tip?.isDisposed == false)
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