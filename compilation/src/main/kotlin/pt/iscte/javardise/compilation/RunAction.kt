package pt.iscte.javardise.compilation

import com.github.javaparser.ast.Modifier
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.traverse
import pt.iscte.javardise.widgets.members.ClassWidget

class RunAction : Action {
    lateinit var editor: CodeEditor

    override val name: String = "Run"

    override val iconPath: String? = null

    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun isEnabled(facade: Facade): Boolean =
        facade.model?.methods?.any {
            it.modifiers.contains(Modifier.publicModifier()) &&
                    it.modifiers.contains(Modifier.staticModifier()) &&
                    it.type.isVoidType &&
                    it.nameAsString == "main"
        } ?: false

    override fun run(facade: Facade, toggle: Boolean) {
        val files = editor.folder.listFiles()?.map {
                CompilationItem(it)
        }?: emptyList()

        val compilation = compileNoOutput(files)
        println(compilation.second)
    }
}
