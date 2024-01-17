package pt.iscte.javardise.autocorrect

import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor

val PRIMITIVE_TYPES = setOf("byte", "short", "int", "long", "float", "double", "char", "boolean")


class AutoCorrectAction : Action {
    override val name: String
        get() = "Auto-correct"

    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    override fun run(editor: CodeEditor, toggle: Boolean) {
        val types = PRIMITIVE_TYPES.toMutableSet()
        editor.allClasses().mapTo(types) { it.nameAsString }
        editor.addCommandObserver { command, _, stack ->
            // new file -> update type list
            // TODO class rename
            if (command == null) {
                editor.allClasses().mapTo(types) { it.nameAsString }
            }

            if (command is AutoCorrectCommand || command == null || stack == null)
                return@addCommandObserver
            else
                AutoCorrectHandler(stack, types).checkCommand(command)
        }
    }




}
