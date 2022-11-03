package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.COMMENT_COLOR
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.ID
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget

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
                editEvent(NameExpr(node.value.filter { it.toString().matches(ID) }))
        }
    }

    init {
        val open = FixedToken(this, "\"")
        open.label.foreground = COMMENT_COLOR()
        open.addKeyListener(delListener)
        text = TextWidget.create(this, node.value) { _, _ -> true }
        text.widget.foreground = COMMENT_COLOR()
        close = TokenWidget(this, "\"")
        close.widget.foreground = COMMENT_COLOR()
        close.widget.addKeyListener(delListener)

        text.addFocusLostAction {
            Commands.execute(object : Command {
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