package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.binaryOperators
import pt.iscte.javardise.external.observeProperty

class BinaryExpressionWidget(parent: Composite, override val node: BinaryExpr) : ExpWidget<BinaryExpr>(parent) {

    var left: ExpWidget<*>
    var operator: TokenWidget
    var right: ExpWidget<*>
    val leftObserver: AstObserver
    val rightObserver: AstObserver
    val operatorObserver: AstObserver

    init {
        layout = ROW_LAYOUT_H_SHRINK
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = { binaryOperators.map { it.asString() } }) {
            Commands.execute(object : Command {
                override val target: Node = node
                override val kind = CommandKind.MODIFY
                override val element = node.operator

                override fun run() {
                    node.operator = binaryOperators.find { op -> op.asString() == it }
                }

                override fun undo() {
                    node.operator = element
                }

            })
        }
        left = drawLeft(this, node.left)
        right = drawRight(this, node.right)
        leftObserver = node.observeProperty<Expression>(ObservableProperty.LEFT) {
            //if (!this.isDisposed) {
            left.dispose()
            drawLeft(this, it!!)
            //}
        }
        rightObserver = node.observeProperty<Expression>(ObservableProperty.RIGHT) {
            // if (!this.isDisposed) {
            right.dispose()
            drawRight(this, it!!)
            //
        }
        operatorObserver = node.observeProperty<BinaryExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "??")
            operator.setFocus()
        }
    }

    override fun dispose() {
        super.dispose()
        node.unregister(leftObserver)
        node.unregister(rightObserver)
        node.unregister(operatorObserver)
    }

    private fun drawLeft(parent: Composite, expression: Expression): ExpWidget<*> {
        left = createExpressionWidget(parent, expression) {
            Commands.execute(object :
                ModifyCommand<Expression>(node, node.left) {
                override fun run() {
                    node.left = it
                }

                override fun undo() {
                    node.left = element
                }
            })
            left.dispose()
            drawLeft(parent, it)
        }
        left.moveAbove(operator.widget)
        left.requestLayout()
        left.setFocusOnCreation()
        return left
    }

    private fun drawRight(parent: Composite, expression: Expression): ExpWidget<*> {
        right = createExpressionWidget(parent, expression) {
            Commands.execute(object :
                ModifyCommand<Expression>(node, node.right) {
                override fun run() {
                    node.right = it
                }

                override fun undo() {
                    node.right = element
                }
            })
            right.dispose()
            drawRight(parent, it)
        }
        right.moveBelow(operator.widget)
        right.requestLayout()
        right.setFocusOnCreation()
        return right
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        right.setFocus()
    }


    override fun toString(): String {
        return "BiExp $node"
    }
}