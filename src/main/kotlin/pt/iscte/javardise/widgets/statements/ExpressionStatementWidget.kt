package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget
import pt.iscte.javardise.widgets.members.addInsert

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
            // TODO edit event
            println(it)
        }
        semiColon = TokenWidget(this, ";")
        semiColon.setToolTip(node.expression::class.qualifiedName!!)
        semiColon.addInsert(this, parent, true)
    }

    override fun setFocus(): Boolean {
        return expression.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocusOnCreation()
    }
}