package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.ERROR_COLOR
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.updateColor

class SimpleExpressionWidget(
    parent: Composite,
    override var node: Expression,
    val editEvent: (Expression) -> Unit
) :
    ExpWidget<Expression>(parent) {

    // TODO flag to require target expression

    var expression: TextWidget
    val keyListener: KeyListener

    init {
        layout = ROW_LAYOUT_H_SHRINK
        val noparse =
            node is NameExpr && (node as NameExpr).name.asString() == Configuration.NOPARSE
        val text = if (noparse)
            if (node.comment.isPresent) node.comment.get().content else ""
        else
            node.toString()

        expression = TextWidget.create(this, text) { c, s ->
            c.toString()
                .matches(Regex("[a-zA-Z\\d_\\[\\]()+]")) || c == SWT.BS || c == SWT.SPACE
        }
        if (noparse)
            expression.widget.background = ERROR_COLOR()

        updateColor(expression)

        keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (expression.isAtBeginning) {
                    val unop = unaryOperators.filter { it.isPrefix }
                        .find { it.asString().startsWith(e.character) }
                    if (unop != null && tryParseExpression(expression.text)) {
                        node = UnaryExpr(
                            StaticJavaParser.parseExpression(expression.text),
                            unop
                        )
                        expression.removeFocusOutListeners()
                        editEvent(node)
                    }
                } else if (expression.isAtEnd) {
                    val biop = binaryOperators.find {
                        it.asString().startsWith(e.character)
                    }
                    if (biop != null && expression.isAtEnd &&
                        tryParseExpression(expression.text)
                    ) {
                        node =
                            BinaryExpr(
                                StaticJavaParser.parseExpression(
                                    expression.text
                                ), NameExpr("expression"), biop
                            )
                        expression.removeFocusOutListeners()
                        editEvent(node)
                    }
                }
            }
        }
        // bug remove listener?
        expression.addKeyListenerInternal(keyListener)

        expression.addKeyEvent('"') {
            editEvent(StringLiteralExpr(expression.text))
        }
        expression.addKeyEvent('\'', precondition = { it.isEmpty() }) {
            editEvent(CharLiteralExpr('a'))
        }
        expression.addKeyEvent('(', precondition = { tryParseSimpleName(it) }) {
            editEvent(MethodCallExpr(expression.text))
        }

        expression.addKeyEvent(
            SWT.BS,
            precondition = { it.isEmpty() && this.parent is BinaryExpressionWidget }) {
            val pp = (this.parent as BinaryExpressionWidget).parent
            if (pp is SimpleExpressionWidget) {
                val biexp = node.parentNode.get() as BinaryExpr
                val part = if (node === biexp.left) biexp.right else biexp.left
                pp.editEvent(part.clone())
            }
        }
        expression.widget.addModifyListener {
            updateColor(expression)
        }

        expression.addFocusLostAction {
            if (tryParseExpression(expression.text)) {
                val newExp =
                    StaticJavaParser.parseExpression<Expression>(expression.text)
                if (!newExp.equals(node)) {
                    node = newExp
                    editEvent(node)
                }
            } else {
                expression.widget.background = ERROR_COLOR()
                val noparse = NameExpr(Configuration.NOPARSE)
                noparse.setComment(BlockComment(expression.text))
                editEvent(noparse)
            }
        }
    }

    override val tail: TextWidget
        get() = expression

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }

    override fun dispose() {
        expression.widget.removeKeyListener(keyListener)
        super.dispose()
    }
}