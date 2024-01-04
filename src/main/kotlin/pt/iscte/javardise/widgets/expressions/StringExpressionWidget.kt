package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.ID
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_DATA_STRING
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING

class StringExpressionWidget(
    parent: Composite,
    override val node: StringLiteralExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<StringLiteralExpr>(parent) {

    val text: TextWidget
    val open: TokenWidget
    val close: TokenWidget

    private val delListener = object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.character == SWT.BS)
                if (node.value.any { it.toString().matches(ID) })
                    editEvent(NameExpr(node.value.filter {
                        it.toString().matches(ID)
                    }))
                else
                    editEvent(Configuration.hole())
        }
    }

    init {
        layout = ROW_LAYOUT_H_STRING
        open = TokenWidget(this, "\"")
        open.widget.foreground = configuration.numberColor
        open.addKeyListenerInternal(delListener)
        text = TextWidget.create(this, node.value, node) { c, s, i ->
            c != '"' || s.substring(0, i).endsWith('\\') && !s.substring(i)
                .startsWith("\"")
        }
        text.widget.foreground = configuration.numberColor
        if (node.value.isEmpty())
            text.widget.layoutData = ROW_DATA_STRING
        text.widget.addModifyListener {
            text.widget.layoutData =
                if (text.text.isEmpty()) ROW_DATA_STRING else null
        }
        text.addDeleteEmptyListener {
            editEvent(Configuration.hole())
        }
        close = TokenWidget(this, "\"")
        close.widget.foreground = configuration.numberColor
        close.widget.addKeyListener(delListener)

        observeNotNullProperty<String>(ObservableProperty.VALUE) {
            text.text = it
            close.setFocus()
        }

        text.addFocusLostAction {
            if (text.text.matches(Regex("([^\\\\]|(\\\\[tbnrf'\"\\\\]))*")))
                commandStack.execute(object : Command {
                    override val target = node
                    override val kind = CommandKind.MODIFY
                    override val element = node.value

                    override fun run() {
                        node.value = text.text
                    }

                    override fun undo() {
                        node.value = element
                    }
                })
            else
                text.text = node.value
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }

    override val head: TextWidget
        get() = open

    override val tail: TextWidget
        get() = close
}