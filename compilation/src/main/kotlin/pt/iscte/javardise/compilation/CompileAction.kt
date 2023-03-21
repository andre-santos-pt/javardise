package pt.iscte.javardise.compilation

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.editor.TabData
import pt.iscte.javardise.external.findMainClass

class CompileAction : Action {

    lateinit var editor: CodeEditor

    override val name: String = "Compile"

    override val iconPath: String = "java.png"

    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun run(facade: Facade, toggle: Boolean) {
        Display.getDefault().focusControl?.traverse(SWT.TRAVERSE_TAB_NEXT)
        Display.getDefault().focusControl?.traverse(SWT.TRAVERSE_TAB_PREVIOUS)
        CompilationProcess.clear()
        facade.model?.let {
            compile()
            CompilationProcess.showErrors(it)
        }
    }

    private fun compile() {
        val files = editor.folder.listFiles()
            .map {
                Triple(
                    it.absolutePath,
                    StaticJavaParser.parse(it).findMainClass(),
                    null
                )
            }

            .filter { it.second != null }
            .map {
                Pair(
                    it.second!!,
                    editor.openTabs.find { w -> (w.data as TabData).file.absolutePath == it.first }?.data as? TabData
                )
            }
            .filter { it.second != null }
            .map {
                Pair(
                    it.first,
                    it.second!!.classWidget!!
                )
            }
            .map { println(it); it }
            .toList()

        CompilationProcess.compile(files)
    }
}