package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.empty
import pt.iscte.javardise.external.isIncrementorOrDecrementor
import pt.iscte.javardise.isFillIn
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

open class ExpressionStatementWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<ExpressionStmt>(parent, node) {
    var expression: ExpressionWidget<*>

    override val tail: TokenWidget


    init {
        layout = ROW_LAYOUT_H_SHRINK
        expression = createExpression(node.expression)
        expression.head.addEmptyStatement(this, parentBlock, node, false)
        expression.head.setCopySource(node)
        tail = TokenWidget(this, ";")
        tail.addEmptyStatement(this, parentBlock, node)
        tail.addDelete(node, parentBlock)

        observeNotNullProperty<Expression>(ObservableProperty.EXPRESSION) {
            expression.dispose()
            expression = createExpression(it)
            expression.moveAbove(tail.widget)
            expression.requestLayout()
            expression.setFocusOnCreation()
        }
    }

    private fun createExpression(e: Expression): ExpressionWidget<*> =
        createExpressionWidget(this, e) {
            if (it == null) {
                parentBlock.statements.replaceCommand(
                    parentBlock,
                    node,
                   parentBlock.empty()
                )
            }
            else if (
                it.isVariableDeclarationExpr ||
                it.isAssignExpr ||
                it.isMethodCallExpr ||
                it.isIncrementorOrDecrementor
            )
                node.modifyCommand(node.expression, it, node::setExpression)
            else if(it.isFillIn) {
                parentBlock.statements.replace(node, parentBlock.empty())
            }
        }

    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if (node.expression.isUnaryExpr)
            tail.setFocus()
        else
            expression.setFocusOnCreation()
    }

}

