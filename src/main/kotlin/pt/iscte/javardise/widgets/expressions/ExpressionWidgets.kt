package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.widgets.binaryOperators
import pt.iscte.javardise.widgets.unaryOperators

fun createExpressionWidget(parent: Composite, node: Expression, editEvent: (Expression) -> Unit): ExpWidget<*> =
    when (node) {
        is UnaryExpr -> UnaryExpressionWidget(parent, node)
        is BinaryExpr -> BinaryExpressionWidget(parent, node)
        is CharLiteralExpr -> CharacterExpressionWidget(parent, node)
        is StringLiteralExpr -> StringExpressionWidget(parent, node)
        is MethodCallExpr -> CallExpressionWidget(parent, node)
        else -> SimpleExpressionWidget(parent, node, editEvent)
    }.apply {
        tail?.apply {
            addKeyListenerInternal(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if(isAtBeginning) {
                        val op = unaryOperators.filter { it.isPrefix }.find { it.asString().startsWith(e.character) }
                        op?.let {
                            editEvent(UnaryExpr(node.clone(), it))
                        }
                    }
                    else if(isAtEnd) {
                        val op = binaryOperators.find { it.asString().startsWith(e.character) }
                        op?.let {
                            editEvent(BinaryExpr(node.clone(), NameExpr("expression"), it))
                        }
                    }
                }
            })
        }
    }

abstract class ExpWidget<T>(parent: Composite) : Composite(parent, SWT.NONE), NodeWidget<T> {
    open val tail: TextWidget? = null
}


