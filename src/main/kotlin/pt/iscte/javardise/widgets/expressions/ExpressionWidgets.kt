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

abstract class ExpressionWidget<T : Expression>(parent: Composite)
    : Composite(parent, SWT.NONE), NodeWidget<T> {

    init {
        font = parent.font
    }

    abstract val editEvent: (Expression?) -> Unit
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
    expression: Expression,
    editEvent: (Expression?) -> Unit
): ExpressionWidget<*> =
        when (expression) {  // TODO edit events
        is UnaryExpr -> UnaryExpressionWidget(parent, expression, editEvent)
        is BinaryExpr -> BinaryExpressionWidget(parent, expression, editEvent)
        is CharLiteralExpr -> CharacterExpressionWidget(parent, expression, editEvent)
        is StringLiteralExpr -> StringExpressionWidget(parent, expression, editEvent)
        is MethodCallExpr -> CallExpressionWidget(parent, expression, editEvent)
        is ArrayCreationExpr -> NewArrayExpressionWidget(parent, expression, editEvent)
        is ArrayInitializerExpr -> NewArrayInitExpressionWidget(parent, expression, editEvent)
        is ObjectCreationExpr -> NewObjectExpressionWidget(parent, expression, editEvent)
        is EnclosedExpr -> BracketsExpressionWidget(parent, expression, editEvent)
        is AssignExpr -> AssignExpressionWidget(parent, expression, editEvent)
        else -> SimpleExpressionWidget(parent, expression, editEvent)
    }.apply {
        if (this !is SimpleExpressionWidget) {
            tail.apply {
                addKeyListenerInternal(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if(e.character == SWT.CR) {
                            tail.widget.traverse(SWT.TRAVERSE_TAB_NEXT)
                        }
                        else {
                            // TODO BUG widget is disposed
                            if (isAtBeginning) {
                                val op = unaryOperators.filter { it.isPrefix }
                                    .find {
                                        it.asString().startsWith(e.character)
                                    }
                                op?.let {
                                    editEvent(UnaryExpr(expression.clone(), it))
                                }
                            } else if (isAtEnd) {
                                val op = binaryOperators.find {
                                    it.asString().startsWith(e.character)
                                }
                                op?.let {
                                    editEvent(
                                        BinaryExpr(
                                            expression.clone(),
                                            NameExpr("expression"),
                                            it
                                        )
                                    )
                                }
                            }
                        }
                    }
                })
            }
        }
    }




