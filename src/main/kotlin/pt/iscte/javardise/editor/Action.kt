package pt.iscte.javardise.editor

interface Action {
    val name: String
    val iconPath: String?
        get() = null
    val toggle: Boolean
        get() = false

    val toggleDefault: Boolean
        get() = false
    fun init(editor: CodeEditor) { }
    fun isEnabled(editor: CodeEditor): Boolean = true
    fun run(editor: CodeEditor, toggle: Boolean)
}
