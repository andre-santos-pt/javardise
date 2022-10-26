package pt.iscte.javardise.basewidgets

import pt.iscte.javardise.widgets.statements.IfWidget


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
                if(last is IfWidget && last.elseWidget != null)
                    last.elseWidget?.closingBracket?.setFocus()
                else
                    last.closingBracket.setFocus()
            else
                last.setFocus()
        }
        else
            setFocus()
    }

    val closingBracket: TextWidget
}