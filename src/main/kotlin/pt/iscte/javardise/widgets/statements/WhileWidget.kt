package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.block
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class WhileWidget(
    parent: SequenceWidget,
    node: WhileStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<WhileStmt>(parent, node), SequenceContainer<WhileStmt> {

    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    lateinit var firstRow: Composite
    override lateinit var bodyWidget: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget

    override val body: BlockStmt = node.block.asBlockStmt()

    init {
        val col = column {
            firstRow = row {
                keyword = newKeywordWidget(this, "while",
                    alternatives = { listOf("if") }) {
                    commandStack.execute(object : Command {
                        override val target: Node = parentBlock
                        override val kind: CommandKind = CommandKind.MODIFY
                        override val element: Statement = node

                        val iff = IfStmt(
                            node.condition.clone(),
                            node.block.clone(),
                            null
                        )
                        override fun run() {
                            node.replace(iff)
                        }

                        override fun undo() {
                            iff.replace(element)
                        }

                    })
                }
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                keyword.addEmptyStatement(this@WhileWidget, parentBlock, node, false)
                keyword.setCopySource(node)

                openClause = FixedToken(this, "(")
                condition = createExpWidget(node.condition)
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            bodyWidget = createSequence(this, node.block)
            openBracket.addEmptyStatement(this@WhileWidget, node.block.asBlockStmt())

        }
        closingBracket = TokenWidget(col, "}")
        closingBracket.addEmptyStatement(this, parentBlock, node)
        addMoveBracket()



        node.observeProperty<Expression>(ObservableProperty.CONDITION) {
            condition.dispose()
            condition = firstRow.createExpWidget(
                it ?: NameExpr(Configuration.fillInToken)
            )
            condition.moveBelow(openClause.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }
    }


    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            if (it == null)
                parentBlock.statements.replaceCommand(
                    parentBlock.parentNode.get(),
                    node,
                    EmptyStmt()
                )
            else
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        condition.setFocus()
    }
}

object WhileFeature : StatementFeature<WhileStmt, WhileWidget>(
    WhileStmt::class.java,
    WhileWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '(', precondition = { it == "while" }) {
            output(WhileStmt(NameExpr(Configuration.fillInToken), BlockStmt()))
        }
    }
}