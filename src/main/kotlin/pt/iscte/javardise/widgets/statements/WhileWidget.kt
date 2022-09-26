package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.layout.RowLayout
import pt.iscte.javardise.Commands
import pt.iscte.javardise.Factory
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.Constants
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.external.*

class WhileWidget(
    parent: SequenceWidget,
    node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent, node) {

    lateinit var keyword: TokenWidget
    lateinit var exp: ExpressionFreeWidget
    lateinit var body: SequenceWidget
    lateinit var openBracket: TokenWidget
    init {
        layout = RowLayout()
        column {
            row {
                keyword = Factory.newKeywordWidget(this, "while")
                keyword.setCopySource()
                FixedToken(this, "(")
                exp = ExpressionFreeWidget(this, node.condition) {
                    Commands.execute(object : ModifyCommand<Expression>(node, node.condition) {
                        override fun run() {
                            node.condition = it
                        }

                        override fun undo() {
                            node.condition = element
                        }
                    })
                }
                FixedToken(this, ")")
                openBracket = TokenWidget(this, "{")
            }
            body = createSequence(this, node.block)
            openBracket.addInsert(null, body, false)
            val bodyClose = TokenWidget(this, "}")
            Constants.addInsertLine(bodyClose, true)
        }

        keyword.addDelete(node, block)
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        exp.setFocus()
    }
}