package pt.iscte.javardise.compilation

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addMark
import pt.iscte.javardise.findChild
import pt.iscte.javardise.widgets.members.ClassWidget

typealias CompileErrors = MutableMap<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>

object CompilationProcess {
    val compileErrors: CompileErrors = mutableMapOf()

    fun compile(files: List<Pair<ClassOrInterfaceDeclaration, ClassWidget>>) {
        val errors = checkCompileErrors(files)
        compileErrors.putAll(errors)
    }

    fun showErrors(model: ClassOrInterfaceDeclaration) {
        compileErrors[model]?.forEach {
            it.show()
        }
    }

    fun clear() {
        compileErrors.forEach {
            it.value.forEach {
                it.delete()
            }
        }
        compileErrors.clear()
    }

    private fun checkCompileErrors(models: List<Pair<ClassOrInterfaceDeclaration, ClassWidget>>): CompileErrors {
        if (models.isEmpty())
            return mutableMapOf()

        val errorDecs =
            mutableMapOf<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>()

        val nodeMap =
            mutableMapOf<ClassOrInterfaceDeclaration, MutableList<Token>>()

        for (i in models) {
            val model = i.first
            nodeMap[model] = buildNodeSourceMap(model)
        }

        val errors = compile(models.map { it.first })
        for (e in errors) {
            println(
                "${(e.source as JavaSource).name} ERROR line ${e.lineNumber} ${e.columnNumber} ${e.kind} ${e.code} ${
                    e.getMessage(
                        null
                    )
                }"
            )
            // zero-based in java compiler
            val m = (e.source as JavaSource).model
            val t = nodeMap[m]?.find { it.line == e.lineNumber && it.col == e.columnNumber }

            val widget = models.find { it.first == m }?.second
            if (t != null) {
                val child = widget?.findChild(t.node)
                if (child != null) {
                    errorDecs.putPair(
                        m,
                        child.addMark(
                            widget.configuration.errorColor,
                            e.getMessage(null)
                        )
                    )
                    //child.addNote(e.getMessage(null), ICodeDecoration.Location.TOP).show()
                } else {
                    errorDecs.putPair(
                        m,
                        widget!!.addMark(
                            widget.configuration.errorColor,
                            e.getMessage(null)
                        )
                    )
                }
            } else {
                errorDecs.putPair(
                    m,
                    widget!!.addMark(
                        widget.configuration.errorColor,
                        e.getMessage(null)
                    )
                )
            }


        }
        return errorDecs
    }
}