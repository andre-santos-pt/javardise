package pt.iscte.javardise

import com.github.javaparser.ast.Node
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.widgets.expressions.AssignmentFeature
import pt.iscte.javardise.widgets.expressions.CallFeature
import pt.iscte.javardise.widgets.expressions.VariableDeclarationFeature
import pt.iscte.javardise.widgets.statements.*
import javax.lang.model.SourceVersion

val ERROR_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_RED) }

val FOREGROUND_COLOR =
    { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND) }

val BACKGROUND_COLOR =
    { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND) }

val COMMENT_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_GREEN) }

val KEYWORD_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA) }

val NUMBER_COLOR by lazy { Display.getDefault().getSystemColor(SWT.COLOR_CYAN) }


object Configuration {
    const val tabLength = 4
    const val NOPARSE = "\$NOPARSE"

    const val fontSize = 18
    const val fontFace = "Menlo"

    val CODE_FONT by lazy {
        Font(
            Display.getDefault(),
            fontFace,
            fontSize,
            SWT.NORMAL
        )
    }

    val statementFeatures = listOf(
        EmptyStatementFeature(),
        ReturnFeature(),
        IfFeature(),
        WhileFeature(),
        ForFeature(),
        VariableDeclarationFeature(),
        AssignmentFeature(),
        CallFeature()
    )
}

fun updateColor(textWidget: TextWidget) {
    if (SourceVersion.isKeyword(textWidget.text))
        textWidget.widget.foreground = KEYWORD_COLOR()
    else if (isNumeric(textWidget.text))
        textWidget.widget.foreground = NUMBER_COLOR
    else
        textWidget.widget.foreground = FOREGROUND_COLOR()
}

fun isNumeric(toCheck: String): Boolean {
    val regex = "-?\\d+(\\.\\d+)?".toRegex()
    return toCheck.matches(regex)
}

class UnsupportedWidget<T: Node> (parent: Composite, override val node: T) : Composite(parent,
    SWT.NONE
), NodeWidget<T> {
    init {
        layout = FillLayout()
        val label = Label(this, SWT.NONE)
        label.text = node.toString()
        label.font = Configuration.CODE_FONT
        label.foreground = Display.getDefault()
            .getSystemColor(SWT.COLOR_DARK_GRAY)
        label.toolTipText = "Unsupported"
        label
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
       setFocus()
    }
}