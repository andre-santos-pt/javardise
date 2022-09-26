package pt.iscte.javardise

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Display

val ERROR_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_RED) }

val FOREGROUND_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND) }

val BACKGROUND_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND) }

val COMMENT_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_GREEN) }

val KEYWORD_COLOR = { Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA) }

val CODE_FONT = {  Font(Display.getDefault(), "Menlo", 16, SWT.NORMAL) }

object Configuration {
    const val tabLength = 4
    const val focusFollowsMouse = false
    const val NOPARSE = "\$NOPARSE"
}