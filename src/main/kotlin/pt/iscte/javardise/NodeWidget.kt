package pt.iscte.javardise

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import javax.lang.model.SourceVersion
import kotlin.reflect.KFunction1

val DefaultConfigurationSingleton = DefaultConfiguration()

interface NodeWidget<T> {
    val configuration: Configuration
        get() {
            val conf by lazy { findConf(control) }
            return conf
        }
    val commandStack: CommandStack
        get() {
            val comm by lazy { findComm(control) }
            return comm
        }

    val node: T
    val control: Control


    fun setFocusOnCreation(firstFlag: Boolean = false)

    private fun findConf(n: Control): Configuration =
        if (n is ConfigurationRoot)
            n.configuration
        else if (n.parent != null)
            findConf(n.parent)
        else
            DefaultConfigurationSingleton

    private fun findComm(n: Control): CommandStack =
        if (n is ConfigurationRoot)
            n.commandStack
        else if (n.parent != null)
            findComm(n.parent)
        else
            CommandStack.nullStack

    fun newKeywordWidget(
        parent: Composite, keyword: String,
        node: Node? = null,
        alternatives: () -> List<String> = { emptyList() },
        editAction: (String) -> Unit = {}
    ): TokenWidget {
        val w = TokenWidget(parent, keyword, node, alternatives, editAction)
        w.widget.foreground = configuration.keywordColor
        return w
    }

    fun TextWidget.addFocusLostAction(
        isValid: (String) -> Boolean,
        action: (String) -> Unit
    ): FocusListener {
        val listener = object : FocusAdapter() {
            var prev: String? = null
            override fun focusGained(e: FocusEvent) {
                prev = widget.text
            }

            override fun focusLost(e: FocusEvent) {
                if (isValid(widget.text)) {
                    action(widget.text)
                    widget.background = configuration.backgroundColor
                } else if (widget.text.isBlank())
                    widget.background = configuration.fillInColor
                else if (prev != null)
                    text = prev as String
                else {
                    text = ""
                    widget.background = configuration.fillInColor
                }
            }
        }
        widget.addFocusListener(listener)
        return listener
    }

    fun updateColor(text: Text) {
        if (SourceVersion.isKeyword(text.text))
            text.foreground = configuration.keywordColor
        else if (text.isNumeric)
            text.foreground = configuration.numberColor
        else
            text.foreground = configuration.foregroundColor
    }

    fun addUpdateColor(text: Text) {
        updateColor(text)
        text.addModifyListener {
            updateColor(text)
        }
    }

    fun <E : Any?> Node.modifyCommand(old: E, new: E, setOperation: KFunction1<E, Node>): Boolean =
        commandStack.modifyCommand(this, old, new, setOperation)

    fun <N : Node> NodeList<in N>.addCommand(owner: Node, e: N, index: Int = size) =
        commandStack.addCommand(this, owner, e, index)

    fun <N : Node> NodeList<in N>.setCommand(owner: Node, e: N, index: Int) =
        commandStack.setCommand(this, owner, e, index)

    fun <N : Node> NodeList<in N>.replaceCommand(owner: Node, e: N, newElement: N) =
        commandStack.replaceCommand(this, owner, e, newElement)

    fun <N : Node> NodeList<in N>.removeCommand(owner: Node, e: N) {
        commandStack.removeCommand(this, owner, e)
    }

}

inline fun <reified T : NodeWidget<*>> Control.findAncestor(): T? {
    var w: Control? = this
    while (w !is T && w != null && w.data !is T)
        w = w.parent

    return w as? T
}

inline fun <reified T : Node> Control.findNode(): T? {
    var w: Control? = this
    while (!(w is NodeWidget<*> && w.node is T) && w != null && w.data !is T)
        w = w.parent

    return if (w is NodeWidget<*>) w.node as T else w?.data as? T
}

fun Composite.findChild(model: Node): Control? {
    var n: Control? = null
    traverse {
//        if(it is Text)
//            println("** " + it.data)

        if (it is NodeWidget<*> && it.node == model) {
            n = it
            return@traverse false
        }
        if (it is Text && it.data == model) {
            n = it
            return@traverse false
        }
        return@traverse true
    }
    return n
}


abstract class ObserverWidget<T : Node>(parent: Composite) : Composite(parent, SWT.NONE), NodeWidget<T> {
    private val registeredObservers = mutableListOf<Pair<Node, AstObserver>>()

    init {
        addDisposeListener {
            registeredObservers.forEach {
                it.first.unregister(it.second)
            }
        }
    }

    fun <T> observeProperty(prop: ObservableProperty, target: Node = node, event: (T?) -> Unit): AstObserver {
        val obs = target.observeProperty(prop, event)
        target.register(obs)
        registeredObservers.add(Pair(target, obs))
        return obs
    }

    fun <P> observeNotNullProperty(prop: ObservableProperty, target: Node = node, event: (P) -> Unit): AstObserver {
        val obs = target.observeNotNullProperty(prop, event)
        target.register(obs)
        registeredObservers.add(Pair(target, obs))
        return obs
    }
}

fun <T : Node> Control.observeListUntilDispose(list: NodeList<T>, observer: ListObserver<T>) {
    val obs = list.observeList(observer)
    addDisposeListener {
        list.unregister(obs)
    }
}


val ID = Regex("[a-zA-Z][a-zA-Z0-9_]*")
val ID_CHARS = Regex("[a-zA-Z0-9_]")
val QNAME_CHARS = Regex("[a-zA-Z0-9.]")
val QNAMEA_CHARS = Regex("[a-zA-Z0-9.*]")
val TYPE_CHARS = Regex("[a-zA-Z0-9_\\[\\]<>]")

data class Validation(val ok: Boolean, val msg: String) {
    val fail get() = !ok
}

open class Id(
    parent: Composite, val id: Node, allowedChars: Regex,
    validate: (String) -> Validation
) :
    TextWidget {
    private var readOnly: Boolean
    internal val textWidget: TextWidget
    private var skip = false

    init {
        fun nodeText(): String {
            val text = if(id is NodeWithSimpleName<*>)
                id.nameAsString
            else
                id.toString()

            return if (text == Configuration.fillInToken)
                ""
            else
                text
        }

        readOnly = false

        textWidget = TextWidget.create(parent, nodeText(), id) { c, _, _ ->
            skip ||
                    !readOnly && (
                    c.toString().matches(allowedChars)
                            || c == SWT.BS
                            || c == SWT.CR)
        }
        textWidget.widget.menu = Menu(textWidget.widget) // prevent system menu
    }


    open fun isValid() = true

    override val widget: Text get() = textWidget.widget

    override fun setFocus(): Boolean {
        textWidget.setFocus()
        textWidget.widget.requestLayout()
        return true
    }


    override fun addKeyListenerInternal(listener: KeyListener) {
        textWidget.addKeyListenerInternal(listener)
    }

    fun setReadOnly() {
        readOnly = true
        textWidget.widget.editable = false
    }

    fun set(text: String?) {
        skip = true
        textWidget.text = text ?: ""
        skip = false
    }

    override var text: String
        get() = super.text
        set(value) {
            textWidget.text = value
        }
}

class SimpleNameWidget<N : Node>(
    parent: Composite,
    override val node: N
) : NodeWidget<N>, Id(parent, node, ID_CHARS, { s ->
    try {
        StaticJavaParser.parseSimpleName(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
}) {
    init {
        textWidget.widget.data = node
        addUpdateColor(textWidget.widget)
    }

    override val control: Control
        get() = textWidget.widget

    override fun setFocusOnCreation(firstFlag: Boolean) {
        textWidget.setFocus()
    }

    override fun isValid(): Boolean =
        SourceVersion.isIdentifier(textWidget.text) && !SourceVersion.isKeyword(textWidget.text)

}

class NameWidget<N : Node>(
    parent: Composite,
    override val node: N,
    allowAsterics: Boolean = false
) : NodeWidget<N>, Id(parent, node, if(allowAsterics) QNAMEA_CHARS else QNAME_CHARS, { s ->
    try {
        StaticJavaParser.parseName(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
}) {
    init {
        textWidget.widget.data = node
        addUpdateColor(textWidget.widget)
    }

    override val control: Control
        get() = textWidget.widget

    override fun setFocusOnCreation(firstFlag: Boolean) {
        textWidget.setFocus()
    }

    override fun isValid(): Boolean =
        SourceVersion.isName(textWidget.text) && !SourceVersion.isKeyword(textWidget.text)

}

open class TypeId(parent: Composite, id: Node) : Id(parent, id, TYPE_CHARS, { s ->
    try {
        StaticJavaParser.parseType(s)
        Validation(true, "")
    } catch (e: ParseProblemException) {
        Validation(false, e.message.toString())
    }
})

class SimpleTypeWidget<N : Type>(parent: Composite, override val node: N) :
//    TypeId(parent, object : NodeWithSimpleName<N> {
//        override fun getName() = SimpleName(node.asString())
//
//        override fun setName(name: SimpleName?): N {
//            // do nothing, not applicable
//            return node
//        }
//    }
    TypeId(parent, node), NodeWidget<N> {
    init {
        textWidget.widget.data = node
        addUpdateColor(textWidget.widget)
    }

    override fun isValid(): Boolean = isValidType(textWidget.text)
    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val control: Control
        get() = textWidget.widget
}




