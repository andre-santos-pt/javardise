package pt.iscte.javardise.compilation

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade

class ClearAction : Action {
    lateinit var editor: CodeEditor

    override val name: String = "Clear"

    override val iconPath: String? = null

    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun run(facade: Facade, toggle: Boolean) {
       CompilationProcess.clear()
    }
}