package pt.iscte.javardise.basewidgets

import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Control
import java.util.function.BiFunction

interface ICodeDecoration<T : Control> {
    fun show()
    fun hide()
    fun delete()
    fun locationHeight() : Int
    val control: T

    fun setTooltip(text: String) {
        control.toolTipText = text
    }

    enum class Location(var f: BiFunction<Point, Point, Point>) :
        BiFunction<Point, Point, Point> {
        LEFT(BiFunction<Point, Point, Point> { t: Point, d: Point ->
            Point(-d.x, t.y / 2 - d.y / 2)
        }),
        LEFT_TOP(
            BiFunction<Point, Point, Point> { t: Point, d: Point ->
                Point(-d.x, t.y - d.y)
            }
        ),
        LEFT_BOTTOM(
            BiFunction<Point, Point, Point> { t: Point, d: Point ->
                Point(-d.x, t.y / 2)
            }
        ),
        RIGHT(
            BiFunction<Point, Point, Point> { t: Point, d: Point ->
                Point(t.x, t.y / 2 - d.y / 2)
            }
        ),
        RIGHT_TOP(
                BiFunction<Point, Point, Point> { t: Point, d: Point ->
                    Point(t.x, t.y - d.y)
                }
        ),
        RIGHT_BOTTOM(
                BiFunction<Point, Point, Point> { t: Point, d: Point ->
                    Point(t.x, t.y / 2)
                }
        ),
        TOP(
            BiFunction<Point, Point, Point> { t: Point, d: Point ->
                Point(
                    t.x / 2 - d.x / 2,
                    -d.y
                )
            }
        ),
        BOTTOM(
            BiFunction<Point, Point, Point> { t: Point, d: Point ->
                Point(
                    t.x / 2 - d.x / 2,
                    d.y
                )
            }
        ),
        ORIGIN(
            BiFunction<Point, Point, Point> { t: Point?, d: Point? ->
                Point(
                    0,
                    0
                )
            }
        );

        override fun apply(
            targetSize: Point,
            decoratorSize: Point
        ): Point {
            return f.apply(targetSize, decoratorSize)
        }

    }
}