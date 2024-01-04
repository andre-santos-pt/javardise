package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget


class ThrowWidget(
    parent: SequenceWidget,
    node: ThrowStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<ThrowStmt>(parent, node) {
    override val keyword: TokenWidget
    var expression: ExpressionWidget<*>
    override val tail: TokenWidget

    init {
        keyword = newKeywordWidget(this, "throw", node)
        keyword.addDelete(node, parentBlock)
        keyword.addEmptyStatement(this, parentBlock, node, false)
        keyword.setCopySource(node)

        expression = createExpression(node.expression)
        tail = TokenWidget(this, ";")
        tail.addDelete(node, parentBlock)
        tail.addEmptyStatement(this, parentBlock, node)

        observeNotNullProperty<Expression>(ObservableProperty.EXPRESSION) {
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
                node.modifyCommand(exp, it, node::setExpression)
        }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }
}

object ThrowFeature : StatementFeature< ThrowStmt,  ThrowWidget>(
    ThrowStmt::class.java,
    ThrowWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, precondition = { it == "throw" }) {
            output(ThrowStmt(Configuration.hole()))
        }
    }
}