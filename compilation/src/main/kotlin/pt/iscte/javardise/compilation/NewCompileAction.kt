package pt.iscte.javardise.compilation

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.traverse
import pt.iscte.javardise.widgets.members.ClassWidget

class NewCompileAction : Action {
    lateinit var editor: CodeEditor

    override val name: String = "New Compile"

    override val iconPath: String? = null

    override fun init(editor: CodeEditor) {
        this.editor = editor
    }

    override fun run(facade: Facade, toggle: Boolean) {
        val src = facade.classWidget?.toSrc()!!
        println(src)

        src.second.forEach {
            it.text.background = null
        }

        val files = editor.folder.listFiles()?.map {
            if(it == facade.file)
                CompilationItem(it, src.first)
            else
                CompilationItem(it)
        }?: emptyList()

        compileNoOutput(files).first.forEach {
            println(it)
            println("${it.startPosition}  ${it.endPosition}")
            val token =
                src.second.find { t -> t.offset == it.startPosition.toInt() && t.end == it.endPosition.toInt() }?:
                src.second.find { t -> t.end == it.endPosition.toInt() }


            if(token != null) {
                token.text.background =
                    Display.getDefault().getSystemColor(SWT.COLOR_RED)
                token.text.toolTipText = it.getMessage(null) + "\n" + it.kind +"\n" + it.code
            }
            else {

            }
        }
        fun type (o: Any?) =
            if(o == null) "null"
        else o::class.simpleName

        src.second.forEach {
            println(
                (it.toString() + " " + it.text.text + " " + type(it.text.data))
                    ?: ""
            )
        }
    }
}

data class WidgetToken(val offset: Int, val end: Int, val text: Text)

fun ClassWidget.toSrc(): Pair<String, List<WidgetToken>> {
    var src = ""
    var offset = 0
    val tokens = mutableListOf<WidgetToken>()
    traverse {
        if (it is Text) {
            tokens.add(
                WidgetToken(
                    src.length,
                    src.length + it.text.length,
                    it
                )
            )
            src += it.text + " "
        } else if (it is Label) {
            src += it.text + " "
        }
        true
    }
    return src to tokens
}