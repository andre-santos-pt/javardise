package pt.iscte.javardise

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.basewidgets.TextWidget
import javax.lang.model.SourceVersion

val ERROR_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_RED) }

val FOREGROUND_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND) }

val BACKGROUND_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND) }

val COMMENT_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_GREEN) }

val KEYWORD_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA) }

val CODE_FONT by lazy {  Font(Display.getDefault(), Configuration.fontFace, Configuration.fontSize, SWT.NORMAL) }

object Configuration {
    const val tabLength = 4
    const val NOPARSE = "\$NOPARSE"
    const val fontSize = 18
    const val fontFace = "Menlo"
}

fun updateColor(textWidget: TextWidget) {
    if (SourceVersion.isKeyword(textWidget.text))
        textWidget.widget.foreground = KEYWORD_COLOR()
    else
        textWidget.widget.foreground = FOREGROUND_COLOR()
}