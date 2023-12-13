package pt.iscte.javardise.debugger

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor

class StepAction : Action {
    override val name: String
        get() = "step"

    override val iconPath: String
        get() = "stepinto.gif"

    override fun run(editor: CodeEditor, toggle: Boolean) {
        State.process?.step(editor.classOnFocus!!)
    }

    override fun isEnabled(editor: CodeEditor): Boolean {
        return State.process?.current?.isOver() == false
    }
}