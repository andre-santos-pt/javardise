package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.api.Editor
import java.lang.IllegalStateException
import kotlin.reflect.KClass

open class SequenceWidget(
        parent: Composite,
        tabs: Int,
        val insertWidgetCreator: (SequenceWidget, Boolean) -> TextWidget
) : EditorWidget(parent) {

    var insertWidget: Control? = null

    init {
        background = parent.background
        foreground = parent.foreground
        val layout = GridLayout(1, true)
        layout.marginLeft = tabs * Editor.tabLength * 5
        layout.marginTop = 0
        layout.marginBottom = 0
        layout.verticalSpacing = 0
        layout.horizontalSpacing = 0
        setLayout(layout)
        if(!Editor.readOnly) {
            val w = insertWidgetCreator(this, true) //configuration.language.createInsertWidget(this, true)
            insertWidget = if (w is Control) w else w.widget
            insertWidget!!.addMouseListener(object : MouseAdapter() {
                override fun mouseDown(e: MouseEvent) {
                    insertWidget!!.setFocus()
                }
            })
        }
    }

    fun insertLine() : TextWidget {
        return if(children.isEmpty()) {
            val insert = insertWidgetCreator(this, false)
            requestLayout()
            insert.setFocus()
            insert
        }
        else
            insertLineAfter(children.last())
    }

    fun insertLineAt(location: Control) : TextWidget {
        val insert = insertWidgetCreator(this, false)  as Control
        insert.moveAbove(location)
        requestLayout()
        insert.setFocus()
        return insert as TextWidget
    }

    fun insertLineAfter(location: Control) : TextWidget {
        val insert = insertWidgetCreator(this, false) as Control
        insert.moveBelow(location)
        requestLayout()
        insert.setFocus()
        return insert as TextWidget;
    }


    fun findIndexExcluding(location: Control, type: Class<*>): Int {
        var i = 0
        for (c in children) {
            if (c === location) return i
            if (!type.isInstance(c)) i++
        }
        assert(false)
        return -1
    }

    fun findChildIndex(location: Control): Int {
        for ((i, c) in children.withIndex())
            if (c === location) return i
        return -1
    }

    fun removeAndGetPrevious(c: Control) : Control? {
        val children = children
        var ret : Control? = null
        for (i in 0 until children.size - 1) {
            if (children[i] === c) {
                children[i].dispose()
                if(i != 0)
                    ret = children[i-1]
                break
            }
        }
        requestLayout()
        return ret
    }

    fun totalElements() = children.size

    fun isFirst(c: Control) = children.first() === c

    fun previous(c: Control) : Control {
        require(!isFirst(c))
        val p = children[0]
        for(i in 1 until children.size)
            if(children[i] === c)
                return children[i-1]
        throw IllegalStateException()
    }

    fun <T : Control> addElement(f: (SequenceWidget) -> T): T = addElement(totalElements(), f)

    fun <T : Control> addElementAfter(c: Control, f: (SequenceWidget) -> T): T = addElement(findChildIndex(c)+1, f)

    fun<T : Control> addElement(index: Int, f: (SequenceWidget) -> T): T {
        val el = if(index < children.size) children[index] else null
        val w = f(this)
        if(el != null)
            w.moveAbove(el)
        return w
    }

    fun<T : Control> addElementSkip(index: Int, skip: KClass<*>, f: (SequenceWidget) -> T): T {
        var i = 0
        for(c in children) {
            if(!skip.isInstance(c))
                i++
            if(i == index)
                break
        }
        i += index
        val el = if(i < children.size) children[i] else null
        val w = f(this)
        if(el != null)
            w.moveAbove(el)
        return w
    }

    fun <T : EditorWidget> addLineAndElement(f: (Composite) -> T): T {
        val e = addElement(totalElements(), f)
        insertLineAt(e)
        return e
    }


    fun removeElement(e: Any) {
        val children = children.copyOf()
        for (i in children.indices) {
            if (children[i] is EditorWidget && (children[i] as EditorWidget).programElement === e) {
                children[i].dispose()
                if(i < children.lastIndex)
                    children[i + 1].setFocus()
                else if(i > 0)
                    children[i - 1].setFocus()
                else
                    parent.setFocus()
                break
            }
        }
        requestLayout()
    }

    fun focusPreviousStatement(statement: Control) {
        val children = children
        if (children.first() === statement) {
            val parent = statement.parent
            val parent2 = (parent as SequenceWidget).parent
            if (parent2 is SequenceWidget) parent2.focusPreviousStatement(statement)
            else parent2.setFocus()
        } else {
            for (i in 1 until children.size) {
                if (children[i] === statement) {
                    if (children[i - 1] is SequenceContainer) (children[i - 1] as SequenceContainer).body.focusLast() else children[i - 1].setFocus()
                    break
                }
            }
        }
    }

    fun focusNextStatement(statement: Control) {
        if (statement is SequenceContainer) {
            (statement as SequenceContainer).body.focusFirst()
        } else {
            val children = children
            if (children.last() === statement) {

                Display.getDefault().focusControl?.traverse(SWT.TRAVERSE_TAB_NEXT)
            } else {
                for (i in 0 until children.size - 1) {
                    if (children[i] === statement) {
                        children[i + 1].setFocus()
                        break
                    }
                }
            }
        }
    }

    // there is at list InsertWidget?

    fun focusFirst() = if(children.isNotEmpty()) children.first().setFocus() else {}

    fun focusLast() = if(children.isNotEmpty())  children.last().setFocus() else {}

    fun clear() {
        children.forEach { if(it != insertWidget) it.dispose() }
    }

    //override fun setReadOnly(readonly: Boolean) {
        //super.setReadOnly(readonly)
        //insertWidget.visible = false
    //}
}