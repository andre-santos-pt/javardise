package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.*
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import api.ICodeDecoration
import api.IWidget
import java.util.*
import java.util.function.BiFunction
import kotlin.math.*

open class EditorWidget : Composite, IWidget {
    override val programElement: Any?

    constructor(parent: Composite) : super(parent, SWT.NONE) {
        layout = Constants.ROW_LAYOUT_H_ZERO
        background = parent.background
        foreground = parent.foreground
        programElement = null
    }

    protected constructor(parent: Composite, element: Any?, vararg extra: Any?) : super(parent, SWT.NONE) {
        layout = Constants.ROW_LAYOUT_H_ZERO
        background = parent.background
        foreground = parent.foreground
        programElement = element
        if (element != null) map[element] = this
        //		else
//			System.err.println(this.getClass());

        extra.forEach {
            if(it != null)
                map[it] = this
        }
    }

    override fun setReadOnly(readonly: Boolean) {
        isEnabled = !readonly
    }

    override val control: Control
        get() = this

    fun addColorPolicy(foreground: () -> Color) {
        // TODO
    }

    fun popup(menu: Menu, control: Control) {
        menu.setLocation(control.toDisplay(0, 40))
    }

    val ownerSequence: SequenceWidget
        get() {
            var parent = parent
            while (parent !is SequenceWidget) parent = parent!!.parent
            assert(parent != null) { "not applicable" }
            return parent
        }

    private val rootSequence: SequenceWidget?
        get() {
            var p = parent
            while (p !is SequenceWidget && p != null)
                p = p!!.parent
            assert(p != null) { "not applicable" }
            return p as SequenceWidget?
        }

    override fun addMark(color: Color): ICodeDecoration<Canvas> {
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
                background = Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT)
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
            d.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT))
        }
        return dec
    }

    override fun addRegionMark(
        color: Color,
        vararg following: IWidget
    ): ICodeDecoration<Canvas> {
        class Rec(
            parent: Composite?,
            var control: Control
        ) : Canvas(parent, SWT.NONE) {
            override fun computeSize(wHint: Int, hHint: Int, changed: Boolean): Point {
                return calcRectangle(wHint, hHint, changed, *following)
            }

            private fun calcRectangle(
                wHint: Int,
                hHint: Int,
                changed: Boolean,
                vararg following: IWidget
            ): Point {
                val size = control.computeSize(wHint, hHint, changed)
                var maxX = size.x
                var maxY = size.y
                for (w in following) {
                    val targetSize = w.control.computeSize(wHint, hHint)
                    val targetLoc = w.control.location
                    if (targetSize.x > maxX) maxX = targetSize.x
                    if (targetLoc.y + targetSize.y > maxY) maxY = targetLoc.y + targetSize.y
                }
                size.x = maxX
                size.y = maxY
                return size
            }

            override fun computeSize(wHint: Int, hHint: Int): Point {
                return computeSize(wHint, hHint, true)
            }

            init {
                background = Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT)
                addPaintListener { e ->
                    e.gc.foreground = color
                    e.gc.lineWidth = 3
                    val r =
                        calcRectangle(SWT.DEFAULT, SWT.DEFAULT, true, *following)
                    e.gc.drawRectangle(0, 0, r.x - 3, r.y - 3)
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
            d.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT))
        }
        return dec
    }

    override fun addImage(
        image: Image,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<Label> {
        return addDecoration(BiFunction<Composite, Control, Label> { p: Composite?, c: Control? ->
            val l = Label(p, SWT.NONE)
            l.image = image
            l
        }, loc)
    }

    override fun addNote(
        text: String,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<Text> {
        return addDecoration(BiFunction<Composite, Control, Text> { p: Composite, c: Control ->
            val t = Text(p, SWT.BORDER or SWT.MULTI)
            t.text = text
            t.isEnabled = false
            t.background = Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
            t.foreground = Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
            t
        }, loc)
    }

    override fun addArrow(targetWidget: IWidget): ICodeDecoration<Canvas> {
        val W = 40

        class Arrow(
            parent: Composite?,
            from: Control?
        ) : Canvas(parent, SWT.NONE) {
            var target: Control?
            fun createArrowForLine(
                device: Device?,
                fromPoint: Point,
                rotationDeg: Double,
                length: Double,
                wingsAngleDeg: Double
            ): Path {
                val ax = fromPoint.x.toDouble()
                val ay = fromPoint.y.toDouble()
                val radB = Math.toRadians(-rotationDeg + wingsAngleDeg)
                val radC = Math.toRadians(-rotationDeg - wingsAngleDeg)
                val resultPath = Path(device)
                resultPath.moveTo(
                    (length * cos(radB) + ax).toFloat(),
                    (length * sin(radB) + ay).toFloat()
                )
                resultPath.lineTo(ax.toFloat(), ay.toFloat())
                resultPath.lineTo(
                    (length * Math.cos(radC) + ax).toFloat(),
                    (length * Math.sin(radC) + ay).toFloat()
                )
                return resultPath
            }

            private fun y(): Int {
                val size = target!!.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                //				if(y < from.getLocation().y)
//					y = -y;
                return target!!.location.y - size.y / 2
            }

            override fun computeSize(wHint: Int, hHint: Int, changed: Boolean): Point {
                val size = target!!.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                return Point(W, target!!.location.y + size.y)
            }

            override fun computeSize(wHint: Int, hHint: Int): Point {
                return computeSize(wHint, hHint, true)
            }

            init {
                var from = from
                target = targetWidget.control
                val dim: Point
                val arrowY: Int
                if (target!!.location.y < from!!.location.y) {
                    val t = target
                    target = from
                    from = t
                    dim = target!!.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                    arrowY = dim.y / 2
                } else {
                    dim = from.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                    arrowY = target!!.location.y
                }
                background = Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT)
                addPaintListener { e ->
                    e.gc.drawArc(0, dim.y / 2, (W * 1.5).toInt(), y(), 90, 180)
                    e.gc.background = Display.getDefault().getSystemColor(SWT.COLOR_BLACK)
                    val path =
                        createArrowForLine(e.display, Point(W, arrowY), 180.0, 15.0, 15.0)
                    e.gc.fillPath(path)
                    path.dispose()
                }
            }
        }

        val dec =
            addDecoration(
                BiFunction<Composite, Control, Canvas> { p: Composite?, c: Control? -> Arrow(p, c) },
                if (targetWidget.control.location.y < location.y) ICodeDecoration.Location.LEFT_TOP else ICodeDecoration.Location.LEFT_BOTTOM
            )
        //				(t,d) -> new Point(-W,0));
        if (dec != null) {
            val d = dec as Decoration<Canvas>
            //			d.setAlpha(50);
            d.setBackground(background)
        }
        return dec
    }



    override fun <T : Control> addDecoration(
        f: BiFunction<Composite, Control, T>,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<T> {
        return Decoration(control, f, loc)
    }

	override val source: String
        get() = getSource(this).toString()

    companion object {
        private val map =
            WeakHashMap<Any, EditorWidget>()

        fun getWidget(e: Any): IWidget? {
            return map[e]
        }

        fun getSource(c: Control): StringBuffer {
            c.setFocus()
            val buffer = StringBuffer()
            if (c is Composite) getSourceAuxComposite(c, buffer)
            else getSourceAuxControl(c, buffer)
            return buffer
        }

        private fun getSourceAuxControl(child: Control, writer: StringBuffer): Boolean {
            val text = child is Text || child is Label
            //			if(text && i == 0) {
//				for(int d = tabs; d > 0; d--)
//					writer.print("\t");
//			}
            if (child is Text) {
                val src = child.text
                //			if(child.getParent() instanceof basewidgets.TextWidget)
//				src = ((basewidgets.TextWidget) child.getParent()).getTextToSerialize();

//			else if(child.getParent() instanceof basewidgets.ExpressionWidget)
//				src = ((basewidgets.ExpressionWidget) child.getParent()).getTextToSerialize();

//			basewidgets.TextWidget w = (basewidgets.TextWidget) child.getParent();
//			if(src.isBlank() && !(child.getParent() instanceof InsertWidget))
//				src = basewidgets.Constants.EMPTY_EXPRESSION_SERIALIZE + child.getParent().getClass();
                writer.append(src)
            } else if (child is Label) writer.append(child.text)
            return text
        }

        private fun getSourceAuxComposite(c: Composite, writer: StringBuffer) {
            val layout = c.layout
            val children = c.children
            for (i in children.indices) {
                val child = children[i]
                val text = getSourceAuxControl(child, writer)
                if (text) {
                    if (layout is GridLayout) // || layout instanceof RowLayout && i == children.length-1)
                        writer.append(System.lineSeparator())
                    else if(i != children.lastIndex)
                        writer.append(" ")
                }
                if (child is Composite) {
                    getSourceAuxComposite(child, writer)
                }
                if (layout is GridLayout) { // || layout instanceof RowLayout && i == children.length-1)
                    //writer.append(System.lineSeparator())
                    //				if(child instanceof basewidgets.SequenceWidget)
//					for(int d = ((basewidgets.SequenceWidget) child).getTabs(); d > 0; d--)
//						writer.print("\t");
                }
            }
        }
    }
}