package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.ObserverWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.binaryOperators
import pt.iscte.javardise.external.isValidMethodCallScope
import pt.iscte.javardise.external.unaryOperators
import pt.iscte.javardise.parseFillIn

// TODO field access expression
// TODO cast expression
/**
 * ArchRule: subclasses should register node observers using observeNotNullProperty(..)
 * defined here. In this way, those observers are unregistered when the widget is disposed.
 */
abstract class ExpressionWidget<T : Expression>(parent: Composite) :
    ObserverWidget<T>(parent) {

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor
    }

    val tailObservers = mutableListOf<(TextWidget) -> Unit>()

    fun tailChanged() =
        tailObservers.forEach { it(tail) }

    abstract val editEvent: (T?) -> Unit

    abstract val head: TextWidget
    abstract val tail: TextWidget

    open fun updateState() { }

    override fun toString(): String {
        return this::class.simpleName + ": $node"
    }

    override val control: Control
        get() = this
}


fun <E : Expression> createExpressionWidget(
    parent: Composite,
    expression: E,
    editEvent: (Expression?) -> Unit
): ExpressionWidget<*> =
    when (expression) {
        is UnaryExpr -> UnaryExpressionWidget(parent, expression, editEvent)
        is BinaryExpr -> BinaryExpressionWidget(parent, expression, editEvent)
        is CharLiteralExpr -> CharacterExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is StringLiteralExpr -> StringExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is FieldAccessExpr -> FieldAccessExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is MethodCallExpr -> CallExpressionWidget(parent, expression, editEvent)
        is ArrayCreationExpr -> NewArrayExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is ArrayInitializerExpr -> NewArrayInitExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is ArrayAccessExpr -> ArrayAccessExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is ObjectCreationExpr -> NewObjectExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is EnclosedExpr -> BracketsExpressionWidget(
            parent,
            expression,
            editEvent
        )
        is AssignExpr -> AssignExpressionWidget(parent, expression, editEvent)
        is VariableDeclarationExpr -> VariableDeclarationWidget(
            parent,
            expression,
            editEvent
        )
        is CastExpr -> CastExpressionWidget(parent, expression, editEvent)
        else -> SimpleExpressionWidget(parent, expression, editEvent)
    }.apply {
        if (this !is SimpleExpressionWidget) {
            tail.apply {
                addKeyListenerInternal(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (widget.isDisposed)
                            return

                        if (e.character == SWT.CR) {
                            tail.widget.traverse(SWT.TRAVERSE_TAB_NEXT)
                        } else {
                            if (isAtBeginning) {
                                // TODO -> (encl)
//                                if(e.character == '(') {
//                                    editEvent(EnclosedExpr(expression.clone()))
//                                }
//                                else {
                                val op =
                                    unaryOperators.filter { it.isPrefix }
                                        .find {
                                            it.asString()
                                                .startsWith(e.character)
                                        }
                                op?.let {
                                    updateState()
                                    editEvent(
                                        UnaryExpr(
                                            expression,
                                            it
                                        )
                                    )
                                }
//                                }
                            } else if (isAtEnd || !isModifiable) {
                                if (e.character == '.' && isValidMethodCallScope(
                                        expression
                                    )
                                ) {
                                    editEvent(
                                        MethodCallExpr(
                                            expression,
                                            Configuration.fillInToken
                                        )
                                    )
                                } else {
                                    val op = binaryOperators.find {
                                        it.asString().startsWith(e.character)
                                    }
                                    op?.let {
                                        updateState()
                                        editEvent(
                                            BinaryExpr(
                                                expression,
                                                Configuration.hole(),
                                                it
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
    }




