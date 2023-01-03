package pt.iscte.javardise.debugger

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade


class StopAction : Action {
    override val name: String
        get() = "Terminate"

    override val iconPath: String
        get() = "terminate.gif"

    override fun run(facade: Facade, toggle: Boolean) {
        check(isEnabled(facade))

        Process.stop()
    }

    override fun isEnabled(facade: Facade): Boolean {
        return Process.current?.isOver() == false
    }
}
