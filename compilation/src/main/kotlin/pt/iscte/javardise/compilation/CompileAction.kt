package pt.iscte.javardise.compilation

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.editor.TabData
import pt.iscte.javardise.external.findMainClass

class CompileAction : Action {
    val compileErrors: CompileErrors = mutableMapOf()
    lateinit var editor: CodeEditor

    override val name: String
        get() = "Compile"

    override val iconPath: String?
        get() = "java.png"
    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun run(facade: Facade, toggle: Boolean) {
        facade.model?.let {
            compile(it)
        }
    }

    fun compile(model: ClassOrInterfaceDeclaration) {
        compileErrors.forEach {
            it.value.forEach { it.delete() }
        }

        compileErrors.clear()
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
        compileErrors.putAll(errors)

        compileErrors[model]?.forEach {
            it.show()
        }
    }

}