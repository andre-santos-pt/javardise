package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.removeCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget
import pt.iscte.javardise.widgets.statements.addInsert

class ExpressionStatementWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) :
    StatementWidget<ExpressionStmt>(parent, node) {
    val expression: ExpressionWidget<*>
    val semiColon: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        expression = createExpressionWidget(this, node.expression) {
           if(it == null)
                block.statements.removeCommand(block, node)
            else {
               // expression.dispose()
               // expression = createExpressionWidget(this, it)
           }
        }
        semiColon = TokenWidget(this, ";")
        semiColon.addInsert(this, parent, true)
        semiColon.addDeleteListener {
            block.statements.removeCommand(block, node)
        }
    }

    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocusOnCreation()
    }
}