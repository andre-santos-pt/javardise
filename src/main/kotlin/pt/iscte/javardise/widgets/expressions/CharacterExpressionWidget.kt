package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget

class CharacterExpressionWidget(
    parent: Composite,
    override val node: CharLiteralExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<CharLiteralExpr>(parent) {
    val text: TextWidget
    val close: TokenWidget

    init {
        val open = FixedToken(this, "'")
        open.label.foreground = Configuration.COMMENT_COLOR
        text = TextWidget.create(this, node.value)

        text.widget.foreground =  Configuration.COMMENT_COLOR
        close = TokenWidget(this, "'")
        close.addDeleteListener {
            editEvent(NameExpr(if(node.value[0].isLetter()) node.value else "expression"))
        }
        close.widget.foreground =  Configuration.COMMENT_COLOR

        text.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if(e.character.isLetter() || e.character.isDigit() || e.character == SWT.SPACE) {
                    Commands.execute(object : Command {
                        override val target = node
                        override val kind = CommandKind.MODIFY
                        override val element = node.value

                        override fun run() {
                            node.value = e.character.toString()
                        }

                        override fun undo() {
                            node.value = element
                            text.text = element
                        }
                    })
                    text.text = e.character.toString()
                    close.setFocus()
                }
            }
        })
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }

    override val tail: TextWidget
        get() = close
}