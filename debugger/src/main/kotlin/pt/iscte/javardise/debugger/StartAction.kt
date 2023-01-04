package pt.iscte.javardise.debugger

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade

class StartAction : Action {
    override val name: String
        get() = "start"

    override val iconPath: String
        get() = "resume.gif"

    override fun run(facade: Facade, toggle: Boolean) {
        check(isEnabled(facade))
        State.process?.start(facade.classWidget!!)
    }

    override fun isEnabled(facade: Facade): Boolean {
        return State.process != null
    }
}