package pt.iscte.javardise.debugger

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade

class StepAction : Action {
    override val name: String
        get() = "step"

    override val iconPath: String
        get() = "stepinto.gif"

    override fun run(facade: Facade, toggle: Boolean) {
        check(isEnabled(facade))
        Process.step(facade)
    }

    override fun isEnabled(facade: Facade): Boolean {
        return Process.current?.isOver() == false
    }
}