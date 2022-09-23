package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.*
import java.util.function.BiFunction

fun <T : Control> Control.addDecoration(
    f: BiFunction<Composite, Control, T>,
    loc: BiFunction<Point, Point, Point>
): ICodeDecoration<T> {
    return Decoration(this, f, loc)
}

fun Control.addNote(
    text: String,
    loc: BiFunction<Point, Point, Point>
): ICodeDecoration<Text> {
    return addDecoration({ p: Composite, c: Control ->
        val t = Text(p, SWT.BORDER or SWT.MULTI)
        t.text = text
        t.isEnabled = false
        t.background = Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
        t.foreground = Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
        t
    }, loc)
}

fun Control.addMark(color: Color): ICodeDecoration<Canvas> {
    class Rec(
        parent: Composite?,
        var control: Control
    ) : Canvas(parent, SWT.NONE) {
        override fun computeSize(wHint: Int, hHint: Int, changed: Boolean): Point {
            val size = control.computeSize(wHint, hHint, changed)
            size.x += 4
            size.y += 4
            return size
        }

        override fun computeSize(wHint: Int, hHint: Int): Point {
            return computeSize(wHint, hHint, true)
        }

        init {
            val dim = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
            background = Display.getDefault().getSystemColor(SWT.TRANSPARENT)
            addPaintListener { e ->
                e.gc.foreground = color
                e.gc.lineWidth = 3
                e.gc.drawRectangle(0, 0, dim.x + 2, dim.y + 2)
            }
        }
    }

    val dec =
        addDecoration(
            BiFunction<Composite, Control, Canvas> { p: Composite?, c: Control -> Rec(p, c) },
            BiFunction<Point, Point, Point> { t: Point?, d: Point? ->
                Point(
                    -2,
                    -2
                )
            }
        )
    if (dec != null) {
        val d =
            dec as Decoration<Canvas>
        d.setAlpha(128)
        d.setBackground(Display.getDefault().getSystemColor(SWT.TRANSPARENT))
    }
    return dec
}

