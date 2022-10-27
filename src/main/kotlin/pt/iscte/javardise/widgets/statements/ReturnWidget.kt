package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.Factory
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

// TODO adapt to throw?
class ReturnWidget(
    parent: SequenceWidget,
    node: ReturnStmt,
    override val block: BlockStmt
) :
    StatementWidget<ReturnStmt>(parent, node) {
    val keyword: TokenWidget
    var expression: ExpressionWidget<*>? = null
    val semiColon: TokenWidget


    init {
        keyword = Factory.newKeywordWidget(this, "return")
        keyword.addKeyEvent(
            SWT.SPACE,
            precondition = { !node.expression.isPresent }) {
            node.modifyCommand(
                null,
                NameExpr("expression"),
                node::setExpression
            )
        }
        keyword.addDelete(node, block)
       // keyword.setCopySource()
        // keyword.setMoveSource()

        if (node.expression.isPresent) {
            expression = createExpressionWidget(this, node.expression.get()) {
                node.modifyCommand(
                    if (node.expression.isPresent) node.expression.get() else null,
                    it,
                    node::setExpression
                )
            }
        }
        semiColon = TokenWidget(this, ";")
        semiColon.addInsert(this, this.parent as SequenceWidget, true)

        node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it == null) {
                expression?.dispose()
                expression = null
                keyword.widget.requestLayout()
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
                expression!!.moveAbove(semiColon.widget)
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
            semiColon.setFocus()
    }
}

class ReturnFeature : StatementFeature<ReturnStmt, ReturnWidget>(ReturnStmt::class.java, ReturnWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
            output(
                if (it.character == SWT.SPACE) ReturnStmt(NameExpr("expression"))
                else ReturnStmt()
            )
        }
    }
}