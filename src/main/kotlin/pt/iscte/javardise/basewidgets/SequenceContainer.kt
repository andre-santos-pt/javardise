package pt.iscte.javardise.basewidgets

interface SequenceContainer {
    val body: SequenceWidget
    fun setFocus(): Boolean
}