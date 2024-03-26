package pt.iscte.javardise.autocorrect

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.SimpleName
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.FileEvent
import java.io.File

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

    private var commandObserver: ((Command, Boolean, CommandStack?) -> Unit)? = null

    private var fileObserver: ((File, FileEvent, unit: CompilationUnit?) -> Unit)? = null

    private val types = PRIMITIVE_TYPES.toMutableSet()

    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle) {
            editor.allCompilationUnits().flatMap { it.types }
                .mapTo(types) { it.nameAsString }

            commandObserver = { command: Command, undo: Boolean, stack: CommandStack? ->
                // class rename
                if (command is ModifyCommand<*> && command.target is ClassOrInterfaceDeclaration && command.element is SimpleName) {
                    types.remove((command.element as SimpleName).identifier)
                    types.add((command.newElement as SimpleName).identifier)
                }

                if (!(command is AutoCorrectCommand || !undo || stack == null)) {
                    val focusControl = Display.getDefault()?.focusControl
                    AutoCorrectHandler(stack, types).checkCommand(command)
                    focusControl?.setFocus()
                }
            }
            editor.addCommandObserver(commandObserver!!)

            fileObserver = { file: File, event: FileEvent, unit: CompilationUnit? ->
                when (event) {
                    FileEvent.CREATE ->
                        types.addAll(unit?.types?.map { it.nameAsString } ?: emptyList())

                    FileEvent.DELETE ->
                        types.removeAll(unit?.types?.map { it.nameAsString } ?: emptyList())

                    FileEvent.RENAME -> {}
                }
            }
            editor.addFileObserver(fileObserver!!)
        } else {
            commandObserver?.let {
                // TODO not removed in all command stacks
                editor.removeCommandObserver(it)
            }
            fileObserver?.let {
                editor.removeFileObserver(it)
            }
        }
    }
}
