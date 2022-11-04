package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.block
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.removeCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class WhileWidget(
    parent: SequenceWidget,
    node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent, node), SequenceContainer<WhileStmt> {

    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    lateinit var firstRow: Composite
    override lateinit var body: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget
    init {
        val col = column {
            firstRow = row {
                keyword = newKeywordWidget(this, "while")
                //keyword.setCopySource(node)
                openClause = FixedToken(this, "(")
                condition = createExpWidget(node.condition)
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.block)
            openBracket.addInsert(null, body, false)

        }
        closingBracket = TokenWidget(col, "}")
            closingBracket.addInsert(this, parent, true)

        keyword.addDelete(node, block)

        node.observeProperty<Expression>(ObservableProperty.CONDITION) {
            condition.dispose()
            condition = firstRow.createExpWidget(it ?: NameExpr("condition"))
            condition.moveBelow(openClause.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }
    }

    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            if(it == null)
                block.statements.removeCommand(block.parentNode.get(), node)
            else
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        condition.setFocus()
    }
}

object WhileFeature : StatementFeature<WhileStmt, WhileWidget>(WhileStmt::class.java, WhileWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '(', precondition = { it == "while"}) {
            output( WhileStmt(NameExpr("condition"), BlockStmt()))
        }
    }
}