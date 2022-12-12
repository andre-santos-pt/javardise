package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

// TODO multi init/update
class ForWidget(
    parent: SequenceWidget,
    node: ForStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<ForStmt>(parent, node), SequenceContainer<ForStmt> {

    lateinit var keyword: TokenWidget
    var init: ExpressionWidget<*>? = null
    lateinit var firstSemiColon: FixedToken
    lateinit var condition: ExpressionWidget<*>
    lateinit var secondSemiColon: FixedToken
    lateinit var firstRow: Composite
    var prog: ExpressionWidget<*>? = null
    lateinit var openBracket: TokenWidget
    override lateinit var bodyWidget: SequenceWidget
    override val closingBracket: TextWidget
    override val body: BlockStmt = node.body.asBlockStmt()

    init {
        val col = column {
            firstRow = row {
                keyword = newKeywordWidget(this, "for")
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                keyword.addEmptyStatement(this@ForWidget, parentBlock, node, false)
                keyword.setCopySource(node)

                FixedToken(this, "(")

                init = if (node.initialization.isEmpty()) null
                else createInitExp(node.initialization[0])

                firstSemiColon = FixedToken(this, ";")
                condition = createCompareExp(node.compare.get())
                secondSemiColon = FixedToken(this, ";")

                prog = if (node.update.isEmpty()) null
                else createUpdateExp(node.update[0])

                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            bodyWidget = createSequence(this, node.body.asBlockStmt())
            openBracket.addEmptyStatement(this@ForWidget, node.body.asBlockStmt())
        }
        closingBracket = TokenWidget(col, "}")
        closingBracket.addEmptyStatement(this, parentBlock, node)

        node.initialization.observeList(object : ListObserver<Expression> {
            override fun elementReplace(
                list: NodeList<Expression>,
                index: Int,
                old: Expression,
                new: Expression
            ) {
                init?.dispose()
                init = firstRow.createInitExp(new)
                init?.moveAbove(firstSemiColon.label)
                init?.requestLayout()
                init?.setFocus()
            }
        })

        node.observeProperty<Expression>(ObservableProperty.COMPARE) {
            condition.dispose()
            condition = firstRow.createCompareExp(it!!)
            condition.moveAbove(secondSemiColon.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }

        node.update.observeList(object : ListObserver<Expression> {
            override fun elementReplace(
                list: NodeList<Expression>,
                index: Int,
                old: Expression,
                new: Expression
            ) {
                prog?.dispose()
                prog = firstRow.createUpdateExp(new)
                prog?.moveBelow(secondSemiColon.label)
                prog?.requestLayout()
                prog?.setFocus()
            }
        })
    }

    private fun Composite.createInitExp(exp: Expression) =
        createExpressionWidget(this, exp) {
            if (it == null)
                parentBlock.statements.removeCommand(parentBlock.parentNode.get(), node)
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

    private fun Composite.createUpdateExp(exp: Expression) =
        createExpressionWidget(this, exp) {
            if (it != null)
                node.update.changeCommand(node, it, 0)
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        init?.setFocus() ?: condition.setFocus()
    }
}

object ForFeature : StatementFeature<ForStmt, ForWidget>(
    ForStmt::class.java,
    ForWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
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