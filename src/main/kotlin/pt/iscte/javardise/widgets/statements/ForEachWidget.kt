package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.observeNotNullProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class ForEachWidget(parent: SequenceWidget, node: ForEachStmt, override val block: BlockStmt) :
    StatementWidget<ForEachStmt>(parent, node), SequenceContainer<ForEachStmt> {

    lateinit var keyword: TokenWidget
    lateinit var variable: ExpressionWidget<*>
    lateinit var iterable: ExpressionWidget<*>
    override lateinit var body: SequenceWidget
    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget
    lateinit var colon: FixedToken
    lateinit var firstRow: Composite

    init {
        column {
            firstRow = row {
                keyword = newKeywordWidget(this, "for")
                keyword.addDelete(node, block)
                FixedToken(this, "(")
                variable = this.createVarExp(this, node.variable)
                colon = FixedToken(this, ":")
                iterable = createIterableExp(this, node.iterable)
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.body.asBlockStmt())
            closingBracket = TokenWidget(this, "}")
            closingBracket.addInsert(this@ForEachWidget, parent, true)
        }
        openBracket.addInsert(null, body, true)

        node.observeNotNullProperty<VariableDeclarationExpr>(ObservableProperty.VARIABLE) {
            variable.dispose()
            variable = createVarExp(firstRow, it)
            variable.moveAbove(colon.label)
            variable.requestLayout()
        }

        node.observeNotNullProperty<Expression>(ObservableProperty.ITERABLE) {
            iterable.dispose()
            iterable = createIterableExp(firstRow, it)
            iterable.moveBelow(colon.label)
            iterable.requestLayout()
        }
    }

    private fun Composite.createVarExp(parent: Composite, exp: VariableDeclarationExpr) =
        createExpressionWidget(parent, exp) {
            if (it == null)
                ;//block.statements.removeCommand(block.parentNode.get(), node)
            else
                node.modifyCommand(node.variable, it as VariableDeclarationExpr, node::setVariable)
        }

    private fun Composite.createIterableExp(parent: Composite, exp: Expression) =
        createExpressionWidget(parent, exp) {
            if (it == null)
                ;//block.statements.removeCommand(block.parentNode.get(), node)
            else
                node.modifyCommand(node.iterable, it, node::setIterable)
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        variable.setFocus()
    }
}

class ForEachFeature : StatementFeature<ForEachStmt, ForEachWidget>(
    ForEachStmt::class.java,
    ForEachWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
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