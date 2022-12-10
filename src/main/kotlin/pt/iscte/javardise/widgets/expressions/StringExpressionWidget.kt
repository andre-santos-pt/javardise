package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget

// TODO empty string "     " and delete " editEvent
class StringExpressionWidget(
    parent: Composite,
    override val node: StringLiteralExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<StringLiteralExpr>(parent) {
    val text: TextWidget
    val close: TokenWidget

    val delListener = object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if(e.character == SWT.BS)
                if(node.value.any { it.toString().matches(ID) })
                    editEvent(NameExpr(node.value.filter { it.toString().matches(ID) }))
                else
                    editEvent(null)
        }
    }

    init {
        val open = FixedToken(this, "\"")
        open.label.foreground = configuration.commentColor
        open.addKeyListener(delListener)
        text = TextWidget.create(this, node.value) { _, _ -> true }
        text.widget.foreground = configuration.commentColor
        close = TokenWidget(this, "\"")
        close.widget.foreground = configuration.commentColor
        close.widget.addKeyListener(delListener)

        text.addFocusLostAction {
            commandStack.execute(object : Command {
                override val target = node
                override val kind = CommandKind.MODIFY
                override val element = node.value

                override fun run() {
                    node.setEscapedValue(text.text.replace("\"","\\\""))
                }

                override fun undo() {
                    node.value = element
                    text.text = element
                }
            })
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }

    override val tail: TextWidget
        get() = close
}