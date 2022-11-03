package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import pt.iscte.javardise.Factory
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.row
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

// TODO for each
class ForEachWidget(parent: SequenceWidget, node: ForEachStmt, override val block: BlockStmt) :
    StatementWidget<ForEachStmt>(parent, node), SequenceContainer {

    lateinit var keyword: TokenWidget
    lateinit var variable: ExpressionWidget<*>
    lateinit var iterable: ExpressionWidget<*>
    override lateinit var body: SequenceWidget
    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TextWidget


    init {
        column {
            row {
                keyword = Factory.newKeywordWidget(this, "for")
                FixedToken(this, "(")

                variable = createExpressionWidget(this, node.variable) {

                }

                FixedToken(this, ":")
                iterable = createExpressionWidget(this, node.iterable) {

                }
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.body.asBlockStmt())
            closingBracket = TokenWidget(this, "}")
        }
        openBracket.addInsert(null, body, true)
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