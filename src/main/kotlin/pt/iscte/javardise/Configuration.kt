package pt.iscte.javardise

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
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
import kotlin.math.roundToInt


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
    val methodModifiers: List<Modifier.Keyword>

    companion object {
        const val noParseToken = "\$NOPARSE"
        const val fillInToken = "\$HOLE"
        val maskKey = SWT.MOD1

        fun hole() = NameExpr(fillInToken)
    }


}

fun KeyEvent.isCombinedKey(key: Int) =
    stateMask == Configuration.maskKey && keyCode == key

interface ConfigurationRoot {
    val configuration: Configuration
    val commandStack: CommandStack
}

fun Color.brighter(f: Int) =
        Color(Display.getDefault(),
            (red + f).coerceIn(0, 255),
            (green + f).coerceIn(0, 255),
            (blue + f).coerceIn(0, 255))

fun Color.luminance() : Int =
    (red * .21 + green * .71 + red * .08).roundToInt()


open class DefaultConfiguration : Configuration {
    override val tabLength = 4
    override val fontSize = 14
    override val fontFace = "Menlo"

    override val darkMode: Boolean
        get() = backgroundColor.luminance() < 128

    override val errorColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_RED)
    }

    override val fillInColor by lazy {
       backgroundColor.brighter(-30)
    }

    override val foregroundColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND)
    }

    override val foregroundColorLight by lazy {
       foregroundColor.brighter(if(darkMode) -160 else 160)
    }

    override val backgroundColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
    }

    override val numberColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_BLUE)
    }

    override val commentColor by lazy {
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
        VariableDeclarationFeature,

        AssignmentFeature,
        CallFeature,
        UnaryExpressionStatementFeature
    )
    override val methodModifiers = listOf(
        Modifier.Keyword.PUBLIC,
        Modifier.Keyword.PROTECTED,
        Modifier.Keyword.PRIVATE,
        Modifier.Keyword.STATIC,
        Modifier.Keyword.FINAL,
        Modifier.Keyword.ABSTRACT
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
