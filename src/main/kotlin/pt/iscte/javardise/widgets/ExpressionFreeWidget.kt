package pt.iscte.javardise.widgets

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.BACKGROUND_COLOR
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.ERROR_COLOR
import pt.iscte.javardise.basewidgets.TextWidget

// TODO observe expression change
class ExpressionFreeWidget(val parent: Composite, var expression: Expression, editEvent: (Expression) -> Unit) :
    TextWidget {

    val textWidget: TextWidget

    init {
        val noparse = expression is NameExpr && (expression as NameExpr).name.asString() == Configuration.NOPARSE
        val text = if (noparse)
            if (expression.orphanComments.isNotEmpty()) expression.orphanComments[0].content else ""
        else
            expression.toString()

        this.textWidget = TextWidget.create(parent, text) { c, s ->
            c.toString()
                .matches(Regex("[a-zA-Z\\d\\[\\]\\.\"'\\+\\-\\*\\\\%=!\\(\\)<>]")) || c == SWT.BS || c == SWT.SPACE
        }
        if (noparse)
            this.textWidget.widget.background = ERROR_COLOR()

        this.textWidget.addFocusListenerInternal(object : FocusAdapter() {
            var existingText: String? = null

            override fun focusGained(e: FocusEvent?) {
                existingText = this@ExpressionFreeWidget.textWidget.text
            }

            override fun focusLost(e: FocusEvent?) {
                if (this@ExpressionFreeWidget.textWidget.text != existingText) {
                    try {
                        expression = StaticJavaParser.parseExpression(this@ExpressionFreeWidget.textWidget.text)
                        this@ExpressionFreeWidget.textWidget.widget.background = BACKGROUND_COLOR()
                        editEvent(expression!!)
                    } catch (_: ParseProblemException) {
                        this@ExpressionFreeWidget.textWidget.widget.background = ERROR_COLOR()
                        val noparse = NameExpr(Configuration.NOPARSE)
                        noparse.addOrphanComment(BlockComment(this@ExpressionFreeWidget.textWidget.text))
                        editEvent(noparse)
                    }
                }
            }
        })
    }

    fun update(e: Expression?) {
        expression = e ?: NameExpr("expression")
        textWidget.text = expression.toString()
    }

    override val widget: Text
        get() = textWidget.widget

    override fun addKeyListenerInternal(listener: KeyListener) {
        textWidget.addKeyListenerInternal(listener)
    }

    override fun addFocusListenerInternal(listener: FocusListener) {
        textWidget.addFocusListenerInternal(listener)
    }

    override fun addFocusLostAction(action: () -> Unit): FocusListener {
        val listener = object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                action()
            }
        }
        textWidget.addFocusListenerInternal(listener)
        return listener
    }
}