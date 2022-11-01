package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import pt.iscte.javardise.external.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Factory
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.changeCommand
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.removeCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

// TODO for
class ForWidget(
    parent: SequenceWidget,
    node: ForStmt,
    override val block: BlockStmt
) :
    StatementWidget<ForStmt>(parent, node) {

    lateinit var keyword: TokenWidget
    var init: ExpressionWidget<*>? = null
    lateinit var firstSemiColon: FixedToken
    lateinit var condition: ExpressionWidget<*>
    lateinit var secondSemiColon: FixedToken
    lateinit var firstRow: Composite
    var prog: ExpressionWidget<*>? = null
    lateinit var openBracket: TokenWidget
    lateinit var body: SequenceWidget


    init {
        layout = RowLayout()
        column {
            firstRow = row {
                keyword = Factory.newKeywordWidget(this, "for")
                keyword.addDelete(node, block)
                FixedToken(this, "(")

                init = if (node.initialization.isEmpty()) null
                else createInitExp()

                firstSemiColon = FixedToken(this, ";")
                condition = createCompareExp(node.compare.get())
                secondSemiColon = FixedToken(this, ";")

                prog = if (node.update.isEmpty()) null
                else createUpdateExp()

                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.body.asBlockStmt())
            openBracket.addInsert(null, body, true)
            FixedToken(this, "}")
        }

        node.initialization.observeList(object : ListObserver<Expression> {
            override fun elementReplace(
                list: NodeList<Expression>,
                index: Int,
                old: Expression,
                new: Expression
            ) {
                init?.dispose()
                init = firstRow.createInitExp()
                init?.moveAbove(firstSemiColon.label)
                init?.requestLayout()
            }
        })

        node.observeProperty<Expression>(ObservableProperty.COMPARE) {
            condition.dispose()
            condition = firstRow.createCompareExp(it!!)
            condition.moveAbove(secondSemiColon.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }
    }

    private fun Composite.createInitExp() =
        createExpressionWidget(
            this,
            node.initialization[0]
        ) {
            if (it == null)
                block.statements.removeCommand(block.parentNode.get(), node)
            else
                node.initialization.changeCommand(node, it, 0)
        }

    private fun Composite.createCompareExp(exp: Expression) =
        createExpressionWidget(this, exp) {
            if (it == null)
                node.modifyCommand(node.compare.get(), BooleanLiteralExpr(true), node::setCompare)
            else
                node.modifyCommand(node.compare.get(), it, node::setCompare)
        }

    private fun Composite.createUpdateExp() =
        createExpressionWidget(
            this,
            node.update[0]
        ) {
            if (it != null)
                node.update.changeCommand(node, it, 0)
        }






    override fun setFocusOnCreation(firstFlag: Boolean) {
        init?.setFocus() ?: condition.setFocus()
    }

}

class ForFeature : StatementFeature<ForStmt, ForWidget>(
    ForStmt::class.java,
    ForWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '(', precondition = { it == "for" }) {
            output(
                ForStmt(
                    NodeList(
                        VariableDeclarationExpr(
                            VariableDeclarator(
                                PrimitiveType.intType(),
                                "i",
                                IntegerLiteralExpr("0")
                            )
                        )
                    ), NameExpr("condition"),
                    NodeList(
                        UnaryExpr(
                            NameExpr("i"),
                            UnaryExpr.Operator.POSTFIX_INCREMENT
                        )
                    ),
                    BlockStmt()
                )
            )
        }
    }
}