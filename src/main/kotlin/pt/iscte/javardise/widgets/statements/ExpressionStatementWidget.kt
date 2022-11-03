package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.removeCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class ExpressionStatementWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) :
    StatementWidget<ExpressionStmt>(parent, node) {
    var expression: ExpressionWidget<*>
    val semiColon: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        expression = createExpression(node.expression)
        semiColon = TokenWidget(this, ";")
        semiColon.addInsert(this, parent, true)
        semiColon.addDeleteListener {
            block.statements.removeCommand(block, node)
        }

        node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it == null)
                block.statements.removeCommand(block, node)
            else {
                expression.dispose()
                expression = createExpression(it)
                expression.moveAbove(semiColon.widget)
                expression.requestLayout()
                expression.setFocus()
            }
        }
    }

    private fun createExpression(e: Expression): ExpressionWidget<*> =
        createExpressionWidget(this, e) {
            if (it == null)
                block.statements.removeCommand(block, node)
            else
                node.modifyCommand(node.expression, it, node::setExpression)
        }
    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if(node.expression.isUnaryExpr)
            semiColon.setFocus()
        else
            expression.setFocusOnCreation()
    }
}

