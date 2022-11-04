package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class DoWhileWidget(
    parent: SequenceWidget,
    node: DoStmt,
    override val block: BlockStmt
) :
    StatementWidget<DoStmt>(parent, node), SequenceContainer<DoStmt> {

    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    lateinit var lastRow: Composite
    override lateinit var body: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget

    init {
        column {
            row {
                keyword = newKeywordWidget(this, "do")
                keyword.addDelete(node, block)
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.body.asBlockStmt())
            openBracket.addInsert(null, body, false)
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

class DoWhileFeature : StatementFeature<DoStmt, DoWhileWidget>(DoStmt::class.java, DoWhileWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "do"}) {
            output( DoStmt(BlockStmt(), NameExpr("condition")))
        }
    }
}