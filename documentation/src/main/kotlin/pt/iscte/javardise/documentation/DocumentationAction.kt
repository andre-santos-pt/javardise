package pt.iscte.javardise.documentation

import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.label
import pt.iscte.javardise.external.message
import pt.iscte.javardise.external.shell

class DocumentationAction : Action {
    override val name: String = "Documentation"

    override val iconPath: String = "book.png"
    override fun isEnabled(editor: CodeEditor): Boolean {
        return editor.classOnFocus != null
    }

    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (editor.classOnFocus?.node == null)
            Display.getDefault().activeShell.message {
                label("Please select a file")
            }
        else
            shell {
                ClassDocumentationView(this, editor.classOnFocus?.node!!)
            }.open()
    }
}