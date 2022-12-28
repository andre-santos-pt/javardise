package pt.iscte.javardise.documentation

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.label
import pt.iscte.javardise.external.message
import pt.iscte.javardise.external.shell

class DocumentationAction : Action {
    override val name: String
        get() = "Documentation"

    override val iconPath: String?
        get() = "book.png"
    override fun isEnabled(facade: Facade): Boolean {
        return facade.classWidget != null
    }

    override fun run(facade: Facade, toggle: Boolean) {
        if (facade.model == null)
            Display.getDefault().activeShell.message {
                label("Please select a file")
            }
        else
            shell {
                ClassDocumentationView(this, facade.model!!)
            }.open()
    }
}