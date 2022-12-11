package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class DoWhileWidget(
    parent: SequenceWidget,
    node: DoStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<DoStmt>(parent, node), SequenceContainer<DoStmt> {

    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    lateinit var lastRow: Composite
    override lateinit var bodyWidget: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget

    override val body: BlockStmt = node.body.asBlockStmt()

    init {
        column {
            row {
                keyword = newKeywordWidget(this, "do")
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                openBracket = TokenWidget(this, "{")
            }
            bodyWidget = createSequence(this, node.body.asBlockStmt())
            openBracket.addInsert(null, bodyWidget, false)
            TokenWidget(this, "}")

            lastRow = row {
                newKeywordWidget(this, "while")
                openClause = FixedToken(this, "(")
                condition = createExpWidget(node.condition)
                FixedToken(this, ")")
                closingBracket = TokenWidget(this, ";")
                closingBracket.addInsert(this@DoWhileWidget, parent, true)
            }
        }
        node.observeProperty<Expression>(ObservableProperty.CONDITION) {
            condition.dispose()
            condition = lastRow.createExpWidget(it ?: NameExpr("condition"))
            condition.moveBelow(openClause.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }
    }

    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            if(it != null)
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        condition.setFocus()
    }
}

object DoWhileFeature : StatementFeature<DoStmt, DoWhileWidget>(DoStmt::class.java, DoWhileWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "do"}) {
            output( DoStmt(BlockStmt(), NameExpr("condition")))
        }
    }
}