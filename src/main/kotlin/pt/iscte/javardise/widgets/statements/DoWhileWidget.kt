package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.row
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class DoWhileWidget(
    parent: SequenceWidget,
    node: DoStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<DoStmt>(parent, node), SequenceContainer<DoStmt> {

    override lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    lateinit var lastRow: Composite
    override lateinit var bodyWidget: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget
    override val tail: TextWidget
        get() = closingBracket

    override val body: BlockStmt = node.body.asBlockStmt()

    init {
        column {
            row {
                keyword = newKeywordWidget(this, "do", node)
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                keyword.addEmptyStatement(this@DoWhileWidget, parentBlock, node, false)
                keyword.setCopySource(node)
                openBracket = TokenWidget(this, "{")
            }
            bodyWidget = createBlockSequence(this, node.body.asBlockStmt())
            openBracket.addEmptyStatement(this@DoWhileWidget, node.body.asBlockStmt())
            TokenWidget(this, "}")

            lastRow = row {
                newKeywordWidget(this, "while")
                openClause = FixedToken(this, "(")
                condition = createExpWidget(node.condition)
                FixedToken(this, ")")
                closingBracket = TokenWidget(this, ";")
                closingBracket.addEmptyStatement(this@DoWhileWidget, parentBlock, node)
                closingBracket.addDelete(node, parentBlock)
            }
        }
        observeNotNullProperty<Expression>(ObservableProperty.CONDITION) {
            condition.dispose()
            condition = lastRow.createExpWidget(it)
            condition.moveBelow(openClause.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }
    }

    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            if(it == null)
                node.modifyCommand(node.condition, Configuration.hole(), node::setCondition)
            else
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        bodyWidget.setFocus()
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
            output(DoStmt(BlockStmt(NodeList(EmptyStmt())), Configuration.hole()))
        }
    }
}