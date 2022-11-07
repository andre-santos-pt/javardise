package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
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


interface Configuration {
    val tabLength: Int
    val fontSize: Int
    val fontFace: String
    val noParseToken: String
    val errorColor: Color
    val font: Font
    val foregroundColor: Color
    val backgroundColor: Color
    val numberColor: Color
    val commentColor: Color
    val keywordColor: Color
    val statementFeatures: List<StatementFeature<out Statement, out StatementWidget<out Statement>>>
}

interface ConfigurationRoot {
    val configuration: Configuration
    val commandStack: CommandStack
}

open class DefaultConfiguration : Configuration {
    override val tabLength = 4
    override val noParseToken = "\$NOPARSE"

    override val fontSize = 14
    override val fontFace = "Menlo"

    override val errorColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_RED)
    }

    override val foregroundColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND)
    }

    override val backgroundColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
    }

    override val numberColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_CYAN)
    }

    override val commentColor by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_GREEN)
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
        widget.widget.foreground = configuration.errorColor
        widget.setToolTip("Unsupported")
        widget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val control: Control
        get() = this
}
