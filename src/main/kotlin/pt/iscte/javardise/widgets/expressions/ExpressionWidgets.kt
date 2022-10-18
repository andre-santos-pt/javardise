package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.binaryOperators
import pt.iscte.javardise.external.unaryOperators

abstract class ExpWidget<T : Expression>(parent: Composite)
    : Composite(parent, SWT.NONE), NodeWidget<T> {

    abstract val tail: TextWidget
    override fun toString(): String {
        return this::class.simpleName + ": $node"
    }

    override fun dispose() {
        tail.removeKeyListeners()
        super.dispose()
    }
}

fun createExpressionWidget(
    parent: Composite,
    node: Expression,
    editEvent: (Expression) -> Unit
): ExpWidget<*> =
    when (node) {
        is UnaryExpr -> UnaryExpressionWidget(parent, node)
        is BinaryExpr -> BinaryExpressionWidget(parent, node)
        is CharLiteralExpr -> CharacterExpressionWidget(parent, node)
        is StringLiteralExpr -> StringExpressionWidget(parent, node)
        is MethodCallExpr -> CallExpressionWidget(parent, node)
        else -> SimpleExpressionWidget(parent, node, editEvent)
    }.apply {
        if (this !is SimpleExpressionWidget)
            tail.apply {
                addKeyListenerInternal(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (isAtBeginning) {
                            val op = unaryOperators.filter { it.isPrefix }
                                .find { it.asString().startsWith(e.character) }
                            op?.let {
                                editEvent(UnaryExpr(node.clone(), it))
                            }
                        } else if (isAtEnd) {
                            val op = binaryOperators.find {
                                it.asString().startsWith(e.character)
                            }
                            op?.let {
                                editEvent(
                                    BinaryExpr(
                                        node.clone(),
                                        NameExpr("expression"),
                                        it
                                    )
                                )
                            }
                        }
                    }
                })
            }
    }




