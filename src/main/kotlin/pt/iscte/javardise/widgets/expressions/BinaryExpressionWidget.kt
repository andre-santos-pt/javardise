package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.binaryOperators

// TODO __ + + to incrementor etc
class BinaryExpressionWidget(
    parent: Composite,
    override val node: BinaryExpr,
    override val editEvent: (Expression?) -> Unit
) :
    ExpressionWidget<BinaryExpr>(parent) {

    var left: ExpressionWidget<*>
    var operator: TokenWidget
    var right: ExpressionWidget<*>

    private val rightExp get() = right

    init {
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = { binaryOperators.map { it.asString() } }) {
            node.modifyCommand(
                node.operator,
                binaryOperators.find { op -> op.asString() == it },
                node::setOperator
            )
        }
        left = drawLeft(this, node.left)
        right = drawRight(this, node.right)
        observeNotNullProperty<Expression>(ObservableProperty.LEFT) {
            left.dispose()
            drawLeft(this, it)
        }
        observeNotNullProperty<Expression>(ObservableProperty.RIGHT) {
            right.dispose()
            drawRight(this, it)
            tailChanged()
        }
        observeNotNullProperty<BinaryExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it.asString())
            operator.widget.selectAll()
        }
    }

    private fun drawLeft(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        left = createExpressionWidget(parent, expression) {
            if (it != null)
                node.modifyCommand(node.left, it, node::setLeft)
            else
                editEvent(node.right)
        }
        left.moveAbove(operator.widget)
        left.requestLayout()
        left.setFocusOnCreation()
//        left.tail.addKeyListenerInternal(object : KeyAdapter() {
//            override fun keyPressed(e: KeyEvent) {
//                if(e.stateMask == Configuration.maskKey && e.keyCode == SWT.ARROW_RIGHT) {
//                    commandStack.execute(object: Command {
//                        override val target: Node
//                            get() = node
//                        override val kind: CommandKind
//                            get() = CommandKind.MOVE
//                        override val element: Expression
//                            get() = node.left
//
//                        val prevright = node.right
//
//                        override fun run() {
//                            editEvent(BinaryExpr(prevright.clone(), element.clone(),node.operator))
//                        }
//
//                        override fun undo() {
//                            editEvent(BinaryExpr(element.clone(), prevright.clone(), node.operator))
//                        }
//
//                    })
//                }
//            }
//        })
        return left
    }

    private fun drawRight(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        right = createExpressionWidget(parent, expression) {
            if (it != null)
                node.modifyCommand(node.right, it, node::setRight)
            else {
                if (node.operator == BinaryExpr.Operator.GREATER_EQUALS)
                    node.modifyCommand(
                        node.operator,
                        BinaryExpr.Operator.GREATER,
                        node::setOperator
                    )
                else if (node.operator == BinaryExpr.Operator.LESS_EQUALS)
                    node.modifyCommand(
                        node.operator,
                        BinaryExpr.Operator.LESS,
                        node::setOperator
                    )
                else
                    editEvent(node.left)
            }

        }
        right.moveBelow(operator.widget)
        right.requestLayout()
        right.setFocusOnCreation()
        right.head.addKeyEvent('=') {
            if (node.operator == BinaryExpr.Operator.GREATER)
                node.modifyCommand(
                    node.operator,
                    BinaryExpr.Operator.GREATER_EQUALS,
                    node::setOperator
                )
            else if (node.operator == BinaryExpr.Operator.LESS)
                node.modifyCommand(
                    node.operator,
                    BinaryExpr.Operator.LESS_EQUALS,
                    node::setOperator
                )
        }
//        right.tail.addKeyListenerInternal(object : KeyAdapter() {
//            override fun keyPressed(e: KeyEvent) {
//                if(e.stateMask == Configuration.maskKey && e.keyCode == SWT.ARROW_LEFT) {
//                    commandStack.execute(object: Command {
//                        override val target: Node
//                            get() = node
//                        override val kind: CommandKind
//                            get() = CommandKind.MOVE
//                        override val element: Expression
//                            get() = node.right
//
//                        val prevleft = node.left
//
//                        override fun run() {
//                            editEvent(BinaryExpr(element.clone(), prevleft.clone(), node.operator))
//                        }
//
//                        override fun undo() {
//                            editEvent(BinaryExpr(prevleft.clone(), element.clone(),node.operator))
//                        }
//
//                    })
//                }
//            }
//        })
        return right
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        right.setFocus()
    }


    override fun toString(): String {
        return "BiExp $node"
    }

    override val tail: TextWidget
        get() = right.tail

    override val head: TextWidget
        get() = left.head
}