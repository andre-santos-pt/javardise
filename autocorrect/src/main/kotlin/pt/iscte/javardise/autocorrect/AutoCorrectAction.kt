package pt.iscte.javardise.autocorrect

import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor

val PRIMITIVE_TYPES = setOf("byte", "short", "int", "long", "float", "double", "char", "boolean", "void")


class AutoCorrectAction : Action {
    override val name: String
        get() = "Auto-correct"

    override val iconPath: String
        get() = "autocorrect.png"
    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    private var obs: ((Command?, Boolean?, CommandStack?) -> Unit)? = null

    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle) {
            val types = PRIMITIVE_TYPES.toMutableSet()
            editor.allClasses().mapTo(types) { it.nameAsString }
            obs = { command: Command?, undo: Boolean?, stack: CommandStack? ->
                // TODO class rename
                if (command == null) {
                    editor.allClasses().mapTo(types) { it.nameAsString }
                }

                if (!(command is AutoCorrectCommand || undo == false || command == null || stack == null)) {
                    val focusControl = Display.getDefault()?.focusControl
                    AutoCorrectHandler(stack, types).checkCommand(command)
                    focusControl?.setFocus()
                }
            }
            editor.addCommandObserver(obs!!)
        } else {
            obs?.let {
                // TODO not removed in all command stacks
                editor.removeCommandObserver(it)
            }
        }
    }
}
