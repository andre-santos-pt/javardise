package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.empty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class ForEachWidget(parent: SequenceWidget, node: ForEachStmt,
                    override val parentBlock: BlockStmt) :
    StatementWidget<ForEachStmt>(parent, node), SequenceContainer<ForEachStmt> {

    override lateinit var keyword: TokenWidget
    lateinit var variable: ExpressionWidget<*>
    lateinit var iterable: ExpressionWidget<*>
    override lateinit var bodyWidget: SequenceWidget
    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget
    lateinit var colon: FixedToken
    lateinit var firstRow: Composite

    override val body: BlockStmt = node.body.asBlockStmt()
    override val tail: TextWidget
        get() = closingBracket
    init {
        column {
            firstRow = row {
                keyword = newKeywordWidget(this, "for", node)
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                keyword.addEmptyStatement(this@ForEachWidget, parentBlock, node, false)
                keyword.setCopySource(node)

                FixedToken(this, "(")
                variable = createVarExp(this, node.variable)
                colon = FixedToken(this, ":")
                iterable = createIterableExp(this, node.iterable)
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            bodyWidget = createBlockSequence(this, node.body.asBlockStmt())
            closingBracket = TokenWidget(this, "}")
            closingBracket.addEmptyStatement(this@ForEachWidget, parentBlock, node)
        }
        openBracket.addEmptyStatement(this@ForEachWidget, node.body.asBlockStmt())

        observeNotNullProperty<VariableDeclarationExpr>(ObservableProperty.VARIABLE) {
            variable.dispose()
            variable = createVarExp(firstRow, it)
            variable.moveAbove(colon.label)
            variable.requestLayout()
        }

        observeNotNullProperty<Expression>(ObservableProperty.ITERABLE) {
            iterable.dispose()
            iterable = createIterableExp(firstRow, it)
            iterable.moveBelow(colon.label)
            iterable.requestLayout()
        }
    }

    private fun createVarExp(parent: Composite, exp: VariableDeclarationExpr) =
        createExpressionWidget(parent, exp) {
            if (it == null)
                parentBlock.statements.replaceCommand(parentBlock, node, parentBlock.empty())
            else
                node.modifyCommand(node.variable, it as VariableDeclarationExpr, node::setVariable)
        }

    private fun createIterableExp(parent: Composite, exp: Expression) =
        createExpressionWidget(parent, exp) {
            if (it == null)
                parentBlock.statements.replaceCommand(parentBlock, node, parentBlock.empty())
            else
                node.modifyCommand(node.iterable, it, node::setIterable)
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        variable.setFocus()
    }
}

object ForEachFeature : StatementFeature<ForEachStmt, ForEachWidget>(
    ForEachStmt::class.java,
    ForEachWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(':', precondition = { it == "for" }) {
            output(
                ForEachStmt(
                    VariableDeclarationExpr(PrimitiveType.intType(), "it"),
                    NameExpr("iterable"),
                    BlockStmt()
                )
            )
        }
    }
}