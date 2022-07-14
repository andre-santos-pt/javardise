package pt.iscte.javardise.api

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.widgets.Display

object Editor {
    var fontSize: Int = 32
        get() = field
        set(value) {
            font = Font(Display.getDefault(), if(System.getProperty("os.name").startsWith("Mac")) "Menlo" else "Consolas", value, SWT.NONE)
        }
    var readOnly = false

    var font = Font(Display.getDefault(), FontData(if(System.getProperty("os.name").startsWith("Mac")) "Menlo"
    else "Consolas", 32, SWT.NORMAL))
   // var font = Font(Display.getDefault(), if(System.getProperty("os.name").startsWith("Mac")) "Menlo"
    //else "Consolas", 32, SWT.NONE)

    var background = Display.getDefault().getSystemColor(SWT.COLOR_BLACK)

    var foreground = Display.getDefault().getSystemColor(SWT.COLOR_WHITE)

    val focusFollowsMouse
        get() = false

    val tabLength: Int
        get() = 4
}