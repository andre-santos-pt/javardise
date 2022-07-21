package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.layout.RowLayout


interface Constants {
    object GridDatas {
        @JvmField
		val SHOW_GRID = GridData(SWT.FILL, SWT.FILL, true, true)
        @JvmField
		val HIDE_GRID = GridData(SWT.LEFT, SWT.TOP, false, false)
        @JvmField
		val SHOW_ROW = RowData(SWT.DEFAULT, SWT.DEFAULT)
        @JvmField
		val HIDE_ROW = RowData(SWT.DEFAULT, SWT.DEFAULT)

        init {
            SHOW_GRID.exclude = false
            HIDE_GRID.exclude = true
            SHOW_ROW.exclude = false
            HIDE_ROW.exclude = true
        }
    }

    companion object {
        fun create(style: Int, top: Int = 0, spacing: Int = 0): RowLayout {
            val layout = RowLayout(style)
            layout.marginLeft = 0
            layout.marginRight = 0
            layout.marginTop = top
            layout.marginBottom = 0
            layout.spacing = spacing
            return layout
        }

        fun addInsertLine(widget: TextWidget, after: Boolean = false) {
            widget.widget.addKeyListener(INSERT_LINE(after))
            widget.widget.data = widget
        }

        class INSERT_LINE(val after: Boolean) : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == SWT.CR) {
                    val w = e.widget.data as TextWidget
                    val statement = w.statement
                    val seq = statement.parent as SequenceWidget
                    if (w is TokenWidget && !after || w.isAtBeginning && !w.isAtEnd)
                        seq.insertLineAt(statement)
                    else if(after || w.isAtEnd)
                        seq.insertLineAfter(statement)
                    e.doit = false
                }
            }
        }

        fun createHeader(parent: EditorWidget): EditorWidget {
            val c = EditorWidget(parent)
            c.layout = ROW_LAYOUT_H
            return c
        }

        @JvmStatic
		fun isNumber(text: String): Boolean {
            return try {
                text.toInt()
                true
            } catch (e: NumberFormatException) {
                try {
                    text.toDouble()
                    true
                } catch (ex: NumberFormatException) {
                    false
                }
            }
        }

        const val MENU_KEY = SWT.SPACE
        const val DEL_KEY = SWT.BS

        val ROW_LAYOUT_H_SHRINK = create(SWT.HORIZONTAL, 0)
		val ROW_LAYOUT_H_ZERO = create(SWT.HORIZONTAL, 2)
		val ROW_LAYOUT_H = create(SWT.HORIZONTAL, 3)
        val ROW_LAYOUT_H_DOT = create(SWT.HORIZONTAL, 0)
        val ROW_LAYOUT_V_ZERO = create(SWT.VERTICAL, 2)
        val ROW_LAYOUT_V_SPACED = create(SWT.VERTICAL, 20)

        val ALIGN_TOP = GridData(SWT.LEFT, SWT.TOP, false, false)
        val data = GridData(SWT.LEFT, SWT.TOP, false, false)
    }
}