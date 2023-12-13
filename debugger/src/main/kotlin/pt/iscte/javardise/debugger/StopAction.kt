package pt.iscte.javardise.debugger

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor


class StopAction : Action {
    override val name: String
        get() = "Terminate"

    override val iconPath: String
        get() = "terminate.gif"

    override fun run(editor: CodeEditor, toggle: Boolean) {
        State.process?.stop()
    }

    override fun isEnabled(editor: CodeEditor): Boolean {
        return State.process?.current?.isOver() == false
    }
}
