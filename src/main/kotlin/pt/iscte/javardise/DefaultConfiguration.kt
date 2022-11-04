package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Composite
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
    val NOPARSE: String
    val ERROR_COLOR: Color
    val CODE_FONT: Font
    val FOREGROUND_COLOR: Color
    val BACKGROUND_COLOR: Color
    val NUMBER_COLOR: Color
    val COMMENT_COLOR: Color
    val KEYWORD_COLOR: Color
    val statementFeatures: List<StatementFeature<out Statement, out StatementWidget<out Statement>>>
}

object DefaultConfiguration : Configuration {
    override val tabLength = 4
    override val NOPARSE = "\$NOPARSE"

    override val fontSize = 18
    override val fontFace = "Menlo"

    override val ERROR_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_RED)
    }

    override val FOREGROUND_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND)
    }

    override val BACKGROUND_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
    }

    override val NUMBER_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_CYAN)
    }

    override val COMMENT_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_GREEN)
    }

    override val KEYWORD_COLOR by lazy {
        Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
    }


    override val CODE_FONT by lazy {
        Font(
            Display.getDefault(),
            fontFace,
            fontSize,
            SWT.NORMAL
        )
    }

    override val statementFeatures = listOf(
        EmptyStatementFeature(),
        ReturnFeature(),
        IfFeature(),
        WhileFeature(),
        DoWhileFeature(),
        ForFeature(),
        ForEachFeature(),
        VariableDeclarationFeature(),
        AssignmentFeature(),
        CallFeature(),
        UnaryExpressionStatementFeature()
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
        widget.widget.font = configuration.CODE_FONT
        widget.widget.foreground = configuration.ERROR_COLOR
        widget.setToolTip("Unsupported")
        widget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}
