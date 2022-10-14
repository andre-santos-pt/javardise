package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.expressions.ExpWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class WhileWidget(
    parent: SequenceWidget,
    node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent, node), SequenceContainer {

    lateinit var keyword: TokenWidget
    lateinit var exp: ExpWidget<*>
    lateinit var firstRow: Composite
    override lateinit var body: SequenceWidget
    lateinit var openClause: FixedToken

    lateinit var openBracket: TokenWidget
    override lateinit var closingBracket: TokenWidget
    init {
        layout = RowLayout()
        val col = column {
            firstRow = row {
                keyword = Factory.newKeywordWidget(this, "while")
                keyword.setCopySource()
                openClause = FixedToken(this, "(")
                exp = createExpWidget(node.condition)
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.block)
            openBracket.addInsert(null, body, false)

        }
        closingBracket = TokenWidget(col, "}")
            closingBracket.addInsert(this, this.parent as SequenceWidget, true)

        keyword.addDelete(node, block)

        node.observeProperty<Expression>(ObservableProperty.CONDITION) {
            exp.dispose()
            exp = firstRow.createExpWidget(it!!)
            exp.moveBelow(openClause.label)
            firstRow.requestLayout()
        }
    }

    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            Commands.execute(object : AbstractCommand<Expression>(node, CommandKind.MODIFY, condition) {
                override fun run() {
                    node.condition = it
                }

                override fun undo() {
                    node.condition = element
                }
            })
        }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        exp.setFocus()
    }
}