package pt.iscte.javardise

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.PropertyObserver
import pt.iscte.javardise.external.isValidType
import pt.iscte.javardise.external.traverse
import javax.lang.model.SourceVersion

interface NodeWidget<T> {
    val configuration: Conf get() = Configuration
    val node: T
    fun setFocusOnCreation(firstFlag: Boolean = false)

    fun <T : Node> observeProperty(prop: ObservableProperty, event: (T?) -> Unit): AstObserver {
        val obs = object : PropertyObserver<T>(prop) {
            override fun modified(oldValue: T?, newValue: T?) {
                event(newValue)
            }
        }
        (node as Node).register(obs)
        return obs
    }

    fun newKeywordWidget(
        parent: Composite, keyword: String,
        alternatives: () -> List<String> = { emptyList() },
        editAtion: (String) -> Unit = {}
    ): TokenWidget {
        val w = TokenWidget(parent, keyword, alternatives, editAtion)
        w.widget.foreground = configuration.KEYWORD_COLOR
        return w
    }

    fun updateColor(textWidget: TextWidget) {
        if (SourceVersion.isKeyword(textWidget.text))
            textWidget.widget.foreground = configuration.KEYWORD_COLOR
        else if (isNumeric(textWidget.text))
            textWidget.widget.foreground = configuration.NUMBER_COLOR
        else
            textWidget.widget.foreground = configuration.FOREGROUND_COLOR
    }
}

inline fun <reified T : NodeWidget<*>> Control.findAncestor(): T? {
    var w : Control? = this
    while(w !is T && w != null && w.data !is T)
        w = w.parent

    return w as? T
}

inline fun <reified T : Node> Control.findNode(): T? {
    var w : Control? = this
    while(!(w is NodeWidget<*> && w.node is T) && w != null && w.data !is T)
        w = w.parent

    return if(w is NodeWidget<*>) w.node as T else w?.data as? T
}

fun Composite.findChild(model: Node): Control? {
    var n: Control? = null
    traverse {
        if (it is NodeWidget<*> && it.node === model) {
            n = it
            return@traverse false
        }

        if (it is Text && it.data === model) {
            n = it
            return@traverse false
        }
        return@traverse true
    }
    return n
}

open class TypeId(parent: Composite, id: String) : Id(parent, id, TYPE_CHARS, {
        s ->
    try {
        StaticJavaParser.parseType(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
})

class SimpleNameWidget<N : Node>(parent: Composite, override val node: N, getName: (N) -> String)
    : NodeWidget<N>, Id(parent, getName(node), ID_CHARS, {
        s ->
    try {
        StaticJavaParser.parseSimpleName(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
}) {
    init {
        textWidget.data = node
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        textWidget.setFocus()
    }

    override fun isValid(): Boolean = SourceVersion.isIdentifier(textWidget.text) && !SourceVersion.isKeyword(textWidget.text)

}

class SimpleTypeWidget<N : Node>(parent: Composite,  override val node: N, getName: (N) -> String)
    : TypeId(parent, getName(node)), NodeWidget<N> {
    init {
        textWidget.data = node
    }

    override fun isValid(): Boolean = isValidType(textWidget.text)
    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}
