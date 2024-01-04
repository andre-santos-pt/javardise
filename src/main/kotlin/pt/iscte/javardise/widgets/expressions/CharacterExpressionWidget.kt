package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.Configuration.Companion.hole
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING

// TODO escape chars
class CharacterExpressionWidget(
    parent: Composite,
    override val node: CharLiteralExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<CharLiteralExpr>(parent) {
    val text: TextWidget
    val open: TokenWidget
    val close: TokenWidget

    init {
        layout = ROW_LAYOUT_H_STRING
        open = TokenWidget(this, "'")
        open.widget.foreground = configuration.commentColor
        text = TextWidget.create(this, node.value, node)
        text.addDeleteListener {
            editEvent(hole())
        }
        text.widget.foreground =  configuration.commentColor
        close = TokenWidget(this, "'")
        close.addDeleteListener {
            editEvent(NameExpr(if(node.value[0].isLetter()) node.value else Configuration.fillInToken))
        }
        close.widget.foreground =  configuration.commentColor

        observeNotNullProperty<String>(ObservableProperty.VALUE) {
            text.text = it
            close.setFocus()
        }

        text.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if((e.stateMask == SWT.NONE || e.stateMask == SWT.SHIFT) && e.character >= SWT.SPACE && e.character <= '~') {
                    editEvent(CharLiteralExpr(e.character.toString()))
                }
            }
        })
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }

    override val head: TextWidget
        get() = open

    override val tail: TextWidget
        get() = close
}