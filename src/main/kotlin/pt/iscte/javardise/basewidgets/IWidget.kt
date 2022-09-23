package pt.iscte.javardise.basewidgets

import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.*
import java.util.function.BiFunction

interface IWidget {
    val control: Control
    val programElement: Any?
    fun setReadOnly(readonly: Boolean)
    fun addMark(color: Color): ICodeDecoration<Canvas>
    fun addImage(
        image: Image,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<Label>

    fun addNote(
        text: String,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<Text>

    fun addRegionMark(
        color: Color,
        vararg following: IWidget
    ): ICodeDecoration<Canvas>

    fun addArrow(target: IWidget): ICodeDecoration<Canvas>
    fun <T : Control> addDecoration(
        f: BiFunction<Composite, Control, T>,
        loc: BiFunction<Point, Point, Point>
    ): ICodeDecoration<T>


    val source: String
}