package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

open class SequenceWidget(
    parent: Composite,
    tabLength: Int,
    val insertWidgetCreator: (SequenceWidget, Boolean) -> TextWidget
) : Composite(parent, SWT.NONE) {

    var insertWidget: Control? = null

    init {
        background = parent.background
        foreground = parent.foreground
        font = parent.font
        val layout = GridLayout(1, true)
        layout.marginLeft = tabLength * 5
        layout.marginTop = 0
        layout.marginBottom = 0
        layout.verticalSpacing = 0
        layout.horizontalSpacing = 0
        setLayout(layout)
    }

    fun insertBeginning() : TextWidget {
        return if(children.isEmpty()) {
            val insert = insertWidgetCreator(this, false)
            addDeleteBehavior(insert)
            requestLayout()
            insert.setFocus()
            insert
        }
        else
            insertLineAt(children.first())
    }

    fun insertLine() : TextWidget {
        return if(children.isEmpty()) {
            val insert = insertWidgetCreator(this, false)
            addDeleteBehavior(insert)
            requestLayout()
            insert.setFocus()
            insert
        }
        else
            insertLineAfter(children.last())
    }

    fun insertLineAt(location: Control) : TextWidget {
        val insert = insertWidgetCreator(this, false)
        addDeleteBehavior(insert)
        insert.widget.moveAbove(location)
        requestLayout()
        insert.setFocus()
        return insert
    }

    private fun addDeleteBehavior(insert: TextWidget) {
        insert.addDeleteEmptyListener {
            val index = children.indexOf(insert.widget) - 1
            insert.delete()
            if (children.isNotEmpty() && index != -1)
                children[index].setFocus()
            else
                parent.setFocus()
            requestLayout()
        }
    }

    fun insertLineAfter(location: Control) : TextWidget {
        val insert = insertWidgetCreator(this, false)
        addDeleteBehavior(insert)
        insert.widget.moveBelow(location)
        requestLayout()
        insert.setFocus()
        return insert
    }


    fun totalElements() = children.size

    fun isFirst(c: Control) = children.first() === c

    fun focusFirst() = if(children.isNotEmpty()) children.first().setFocus() else { parent.setFocus() }

    fun focusLast() = if(children.isNotEmpty())  children.last().setFocus() else { parent.setFocus()}

    fun focusAt(index: Int) = if(index <= 0)
        focusFirst()
    else if(index >= children.size)
        focusLast()
    else
        children[index].setFocus()

    fun clear() {
        children.forEach { if(it != insertWidget) it.dispose() }
    }

    fun isEmpty(): Boolean = children.isEmpty()
}