package pt.iscte.javardise

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.widgets.expressions.AssignmentFeature
import pt.iscte.javardise.widgets.expressions.CallFeature
import pt.iscte.javardise.widgets.expressions.UnaryExpressionStatementFeature
import pt.iscte.javardise.widgets.expressions.VariableDeclarationFeature
import pt.iscte.javardise.widgets.statements.*
import java.util.*
import kotlin.math.roundToInt


val isWindows: Boolean = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")

interface Configuration {
    val tabLength: Int
    val fontSize: Int
    val fontFace: String
    val darkMode: Boolean
    val errorColor: Color
    val fillInColor: Color
    val font: Font
    val foregroundColor: Color
    val foregroundColorLight: Color
    val backgroundColor: Color
    val numberColor: Color
    val commentColor: Color
    val keywordColor: Color
    val statementFeatures: List<StatementFeature<out Statement, out StatementWidget<out Statement>>>

    val classModifiers: List<List<Modifier.Keyword>>
    val fieldModifiers: List<List<Modifier.Keyword>>
    val methodModifiers: List<List<Modifier.Keyword>>

    companion object {
        const val noParseToken = "\$NOPARSE"
        const val fillInToken = "\$HOLE"
        val maskKey = SWT.MOD1

        fun hole() = NameExpr(fillInToken)

        fun idHole() = SimpleName(fillInToken)

        fun typo(text: String): NameExpr {
            val t = NameExpr(noParseToken)
            t.setComment(LineComment(text))
            return t
        }
    }
}


val Node.isNoParse get() = (this is NameExpr && this.name.identifier == Configuration.noParseToken) ||
        (this is SimpleName && this.identifier == Configuration.noParseToken)

val Node.isFillIn get() = (this is NameExpr || this is SimpleName) && toString() == Configuration.fillInToken

fun nodeText(node: Node): String {
    return if (node.isNoParse) {
        if (node.comment.isPresent) node.comment.get().content.trim() else ""
    } else if (node.isFillIn)
        ""
    else if(node is NameExpr)
        node.nameAsString
    else if(node is SimpleName)
        node.identifier
    else if(node is LiteralStringValueExpr)
        node.value
    else if(node is BooleanLiteralExpr)
        node.value.toString()
    else if(node is NullLiteralExpr)
        "null"
    else
        node.toString()


}


internal inline fun parseFillIn(exp: String): Expression =
    try {
        StaticJavaParser.parseExpression(exp)
    } catch (_: ParseProblemException) {
        if (exp.isBlank())
            Configuration.hole()
        else
            Configuration.typo(exp)
    }

fun KeyEvent.isCombinedKey(key: Int) =
    stateMask == Configuration.maskKey && keyCode == key

interface ConfigurationRoot {
    val configuration: Configuration
    val commandStack: CommandStack

    fun addUndoSupport(mask: Int, keycode: Int) {
        Display.getDefault().addFilter(SWT.KeyDown) {
            if (it.stateMask == mask && it.keyCode == keycode) {
                commandStack.undo()
                it.doit = false
            }
        }

    }
}

fun Color.brighter(f: Int) =
    Color(
        Display.getDefault(),
        (red + f).coerceIn(0, 255),
        (green + f).coerceIn(0, 255),
        (blue + f).coerceIn(0, 255)
    )

fun Color.luminance(): Int =
    (red * .21 + green * .71 + red * .08).roundToInt()


open class DefaultConfiguration : Configuration {
    override val tabLength = 4
    override val fontSize = 12
    override val fontFace = if(isWindows) "Consolas" else "Menlo"

    override val darkMode: Boolean
        get() = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).luminance() < 128

    override val errorColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_RED)
    }

    override val fillInColor by lazy {
        backgroundColor.brighter(if (darkMode) 30 else -30)
    }

    override val foregroundColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND)
    }

    override val foregroundColorLight by lazy {
        foregroundColor.brighter(if (darkMode) -160 else 160)
    }

    override val backgroundColor by lazy {
        if (darkMode)
            Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
        else
            Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
    }

    override val numberColor by lazy {
        if (darkMode)
            Display.getDefault().getSystemColor(SWT.COLOR_CYAN)
        else
            Display.getDefault().getSystemColor(SWT.COLOR_BLUE)
    }

    override val commentColor by lazy {
        if (darkMode)
            Display.getDefault().getSystemColor(SWT.COLOR_GREEN)
        else
            Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN)
    }

    override val keywordColor by lazy {
        Color(Display.getDefault(), 204, 120, 50)
    }


    override val font by lazy {
        Font(
            Display.getDefault(),
            fontFace,
            fontSize,
            SWT.NORMAL
        )
    }

    override val statementFeatures = listOf(
        EmptyStatementFeature,
        LineCommentFeature,
        ReturnFeature,
        IfFeature,
        WhileFeature,
        DoWhileFeature,
        ForFeature,
        ForEachFeature,

        BreakFeature,
        ContinueFeature,

        VariableDeclarationFeature,
        AssignmentFeature,
        CallFeature,
        UnaryExpressionStatementFeature,

        AssertFeature
    )

    override val classModifiers = listOf(
        listOf(Modifier.Keyword.PUBLIC),
        listOf(Modifier.Keyword.FINAL, Modifier.Keyword.ABSTRACT)
    )

    override val fieldModifiers = listOf(
        listOf(
            Modifier.Keyword.PUBLIC,
            Modifier.Keyword.PROTECTED,
            Modifier.Keyword.PRIVATE
        ),
        listOf(Modifier.Keyword.STATIC),
        listOf(Modifier.Keyword.FINAL)
    )

    override val methodModifiers = listOf(
        listOf(
            Modifier.Keyword.PUBLIC,
            Modifier.Keyword.PROTECTED,
            Modifier.Keyword.PRIVATE
        ),
        listOf(Modifier.Keyword.STATIC, Modifier.Keyword.ABSTRACT),
        listOf(Modifier.Keyword.FINAL, Modifier.Keyword.ABSTRACT),
        listOf(Modifier.Keyword.SYNCHRONIZED, Modifier.Keyword.ABSTRACT)
    )
}


// TODO arrow down
class UnsupportedWidget<T : Node>(parent: Composite, override val node: T) :
    Composite(
        parent,
        SWT.NONE
    ), NodeWidget<T> {
    val widget: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        widget = TokenWidget(this, node.toString())
        widget.widget.font = configuration.font
        widget.widget.foreground = parent.foreground
        widget.widget.background = configuration.fillInColor
        widget.setToolTip("Unsupported")
        widget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val control: Control
        get() = this
}
