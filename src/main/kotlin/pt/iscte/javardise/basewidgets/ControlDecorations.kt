package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.widgets.expressions.SimpleExpressionWidget
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
    val dec = addDecoration({ p: Composite, c: Control ->
        val t = Text(p, SWT.BORDER or SWT.MULTI)
        t.text = text
        t.isEnabled = false
        t.background =
            Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
        t.foreground =
            Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
        t
    }, loc)
    dec as Decoration<Text>
    dec.setAlpha(128)
    return dec
}

fun Control.addTextbox(
    text: String,
    loc: BiFunction<Point, Point, Point>
): ICodeDecoration<Text> {
    val dec = addDecoration({ p: Composite, c: Control ->
        val t = Text(p, SWT.BORDER)
        t.text = text
        t.background =
            Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
        t.foreground =
            Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
        t
    }, loc)
    dec as Decoration<Text>
    return dec
}

fun Control.addMark(color: Color, text: String = ""): ICodeDecoration<Canvas> {
    class Rec(
        parent: Composite,
        var control: Control
    ) : Canvas(parent, SWT.NO_BACKGROUND) {
        override fun computeSize(
            wHint: Int,
            hHint: Int,
            changed: Boolean
        ): Point {
            val size = control.computeSize(wHint, hHint, changed)
            size.x += 4
            size.y += 4
            return size
        }

        override fun computeSize(wHint: Int, hHint: Int): Point {
            return computeSize(wHint, hHint, true)
        }

        var note: ICodeDecoration<*>? = null

        init {
            background = Display.getDefault().getSystemColor(SWT.TRANSPARENT)
            toolTipText = text

//            if (text.isNotEmpty() && this@addMark is SimpleExpressionWidget) {
//                val w = this@addMark.expression.widget
//                w.addModifyListener {
//                   requestLayout()
//                }
//                note = w.addNote(
//                    text,
//                    ICodeDecoration.Location.RIGHT
//                )
//                w.addFocusListener(object : FocusListener {
//                    override fun focusGained(e: FocusEvent?) {
//                        note?.show()
//                    }
//
//                    override fun focusLost(p0: FocusEvent?) {
//                        note?.hide()
//                    }
//
//                })
//            }
            parent.addPaintListener { e ->
                paint(e.gc)
            }
        }

        private fun paint(gc: GC) {
            val dim = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
            gc.foreground = color
            gc.lineWidth = 3
            gc.drawRectangle(0, 0, dim.x + 2, dim.y + 2)
        }

        override fun dispose() {
            note?.delete()
        }
    }

    val dec =
        addDecoration(
            BiFunction<Composite, Control, Canvas> { p: Composite, c: Control ->
                Rec(
                    p,
                    c
                )
            },
            BiFunction<Point, Point, Point> { t: Point?, d: Point? ->
                Point(
                    -2,
                    -2
                )
            }
        )
    val d =
        dec as Decoration<Canvas>
    d.setAlpha(128)
    d.setBackground(Display.getDefault().getSystemColor(SWT.TRANSPARENT))
    return dec
}

