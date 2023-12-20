package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

// TODO adapt to throw?
class ReturnWidget(
    parent: SequenceWidget,
    node: ReturnStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<ReturnStmt>(parent, node) {
    override val keyword: TokenWidget
    var expression: ExpressionWidget<*>? = null
    override val tail: TextWidget

    init {
        keyword = newKeywordWidget(this, "return")
        keyword.addKeyEvent(
            SWT.SPACE,
            precondition = { !node.expression.isPresent }) {
            node.modifyCommand(
                null,
                NameExpr(Configuration.fillInToken),
                node::setExpression
            )
        }
        keyword.addDelete(node, parentBlock)
        keyword.addEmptyStatement(this, parentBlock, node, false)
        keyword.setCopySource(node)

        if (node.expression.isPresent) {
            expression = createExpressionWidget(this, node.expression.get()) {
                node.modifyCommand(
                    if (node.expression.isPresent) node.expression.get() else null,
                    it,
                    node::setExpression
                )
            }
        }
        tail = TokenWidget(this, ";")
        tail.addDelete(node, parentBlock)
        tail.addEmptyStatement(this, parentBlock, node)

        observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it == null) {
                expression?.dispose()
                expression = null
                keyword.widget.requestLayout()
                keyword.setFocus()
            }
            else {
                expression?.dispose()
                expression = createExpressionWidget(this, it) { e ->
                    node.modifyCommand(
                        if (node.expression.isPresent) node.expression.get() else null,
                        e,
                        node::setExpression
                    )
                }
                expression!!.moveAbove(tail.widget)
                expression!!.requestLayout()
                expression!!.setFocusOnCreation()
            }
        }
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        if(expression != null)
            expression!!.setFocus()
        else
            tail.setFocus()
    }
}

object ReturnFeature : StatementFeature<ReturnStmt, ReturnWidget>(ReturnStmt::class.java, ReturnWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
            output(
                if (it.character == SWT.SPACE) ReturnStmt(NameExpr(Configuration.fillInToken))
                else ReturnStmt()
            )
        }
    }
}