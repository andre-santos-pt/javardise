package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.binaryOperators
import pt.iscte.javardise.external.unaryOperators

abstract class ExpressionWidget<T : Expression>(parent: Composite)
    : Composite(parent, SWT.NONE), NodeWidget<T> {

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = parent.font
    }

    abstract val editEvent: (T?) -> Unit
    abstract val tail: TextWidget
    override fun toString(): String {
        return this::class.simpleName + ": $node"
    }

    override fun dispose() {
        tail.removeKeyListeners()
        super.dispose()
    }

    override val control: Control
        get() = this
}

// TODO Expression features
abstract class ExpressionFeature<M: Expression, W: ExpressionWidget<*>>(val modelClass: Class<M>, val widgetClass: Class<W>) {
    init {
        val paramTypes = widgetClass.constructors[0].parameterTypes
        require(paramTypes.size == 3)
        require(paramTypes[0] == Composite::class.java)
        require(Expression::class.java.isAssignableFrom(paramTypes[1]))
        // TODO check third param
        //require(paramTypes[2] == BlockStmt::class.java)
    }
    fun create(parent: Composite, exp: Expression, editEvent: (Expression?) -> Unit): ExpressionWidget<M> =
        widgetClass.constructors[0].newInstance(parent, exp, editEvent) as ExpressionWidget<M>

    //open fun targets(stmt: Statement) = modelClass.isInstance(stmt)

    abstract fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    )
}

fun <E: Expression> createExpressionWidget(
    parent: Composite,
    expression: E,
    editEvent: (Expression?) -> Unit
): ExpressionWidget<*> =
        when (expression) {
        is UnaryExpr -> UnaryExpressionWidget(parent, expression, editEvent)
        is BinaryExpr -> BinaryExpressionWidget(parent, expression, editEvent)
        is CharLiteralExpr -> CharacterExpressionWidget(parent, expression, editEvent)
        is StringLiteralExpr -> StringExpressionWidget(parent, expression, editEvent)
        is MethodCallExpr -> CallExpressionWidget(parent, expression, editEvent)
        is ArrayCreationExpr -> NewArrayExpressionWidget(parent, expression, editEvent)
        is ArrayInitializerExpr -> NewArrayInitExpressionWidget(parent, expression, editEvent)
            is ArrayAccessExpr -> ArrayAccessExpressionWidget(parent, expression, editEvent)
        is ObjectCreationExpr -> NewObjectExpressionWidget(parent, expression, editEvent)
        is EnclosedExpr -> BracketsExpressionWidget(parent, expression, editEvent)
        is AssignExpr -> AssignExpressionWidget(parent, expression, editEvent)
            is VariableDeclarationExpr -> VariableDeclarationWidget(parent, expression, editEvent)
        else -> SimpleExpressionWidget(parent, expression, editEvent)
    }.apply {
        if (this !is SimpleExpressionWidget) {
            tail.apply {
                addKeyListenerInternal(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if(widget.isDisposed)
                            return

                        if(e.character == SWT.CR) {
                            tail.widget.traverse(SWT.TRAVERSE_TAB_NEXT)
                        }
                        else {
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




