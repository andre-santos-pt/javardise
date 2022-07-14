package basewidgets

interface SequenceContainer {
    val body: SequenceWidget
    fun setFocus(): Boolean
}