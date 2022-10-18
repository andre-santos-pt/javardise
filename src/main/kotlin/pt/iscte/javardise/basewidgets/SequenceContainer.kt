package pt.iscte.javardise.basewidgets

import org.w3c.dom.Text

interface SequenceContainer {
    val body: SequenceWidget?
    fun setFocus(): Boolean

    fun focusFirst() {
        body?.setFocus()
    }

    fun focusLast() {
        if(body?.children?.isNotEmpty() == true) {
            val last = body!!.children.last()
            if(last is SequenceContainer)
                last.closingBracket.setFocus()
            else
                last.setFocus()
        }
        else
            setFocus()
    }

    val closingBracket: TextWidget
}