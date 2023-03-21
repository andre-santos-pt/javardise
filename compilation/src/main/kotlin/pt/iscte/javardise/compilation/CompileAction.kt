package pt.iscte.javardise.compilation

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.editor.TabData
import pt.iscte.javardise.external.findMainClass

object Compilation {
    val compileErrors: CompileErrors = mutableMapOf()

    fun clear() {
        compileErrors.forEach {
            it.value.forEach {
                it.delete()
            }
        }
        compileErrors.clear()
    }

}
class CompileAction : Action {

    lateinit var editor: CodeEditor

    override val name: String = "Compile"

    override val iconPath: String = "java.png"

    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun run(facade: Facade, toggle: Boolean) {
        facade.model?.let {
            compile(it)
        }
    }

    fun compile(model: ClassOrInterfaceDeclaration) {
        Compilation.clear()
        // FileFilter { it.name.endsWith(".java") }

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

        val errors = checkCompileErrors(files)
        Compilation.compileErrors.putAll(errors)
        Compilation.compileErrors[model]?.forEach {
            it.show()
        }
    }

}