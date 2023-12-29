package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.AssertStmt
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget


class AssertWidget(
    parent: SequenceWidget,
    node: AssertStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<AssertStmt>(parent, node) {
    override val keyword: TokenWidget
    var expression: ExpressionWidget<*>
    override val tail: TokenWidget

    init {
        keyword = newKeywordWidget(this, "assert")
        keyword.addDelete(node, parentBlock)
        keyword.addEmptyStatement(this, parentBlock, node, false)
        keyword.setCopySource(node)

        expression = createExpression(node.check)
        tail = TokenWidget(this, ";")
        tail.addDelete(node, parentBlock)
        tail.addEmptyStatement(this, parentBlock, node)
        // TODO assert message
//        tail.addKeyEvent(':') {
//            node.modifyCommand(node.message.getOrNull, StringLiteralExpr(""), node::setMessage)
//        }

        observeNotNullProperty<Expression>(ObservableProperty.CHECK) {
            expression.dispose()
            expression = createExpression(it)
            expression.moveAbove(tail.widget)
            expression.requestLayout()
            expression.setFocusOnCreation()
        }
    }

    private fun createExpression(exp: Expression) =
        createExpressionWidget(this, exp) {
            if (it == null)
                parentBlock.statements.removeCommand(parentBlock, node)
            else
                node.modifyCommand(exp, it, node::setCheck)
        }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }
}

object AssertFeature : StatementFeature< AssertStmt,  AssertWidget>(
    AssertStmt::class.java,
    AssertWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, precondition = { it == "assert" }) {
            output(AssertStmt(Configuration.hole()))
        }
    }
}