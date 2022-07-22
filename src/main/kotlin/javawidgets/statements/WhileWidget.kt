package javawidgets.statements

import basewidgets.Constants
import basewidgets.FixedToken
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.WhileStmt
import javawidgets.*
import org.eclipse.swt.layout.RowLayout
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

class WhileWidget(
    parent: SequenceWidget,
    override val node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent) {

    lateinit var keyword: TokenWidget
    lateinit var exp: ExpWidget
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = Factory.newTokenWidget(this, "while")
                keyword.setCopySource()
                FixedToken(this, "(")
                exp = ExpWidget(this, node.condition) {
                    Commands.execute(object : ModifyCommand<Expression>(node, node.condition) {
                        override fun run() {
                            node.condition = it
                        }

                        override fun undo() {
                            node.condition = element
                        }
                    })
                }
                FixedToken(this, ") {")
            }
            body = createSequence(this, node.block)
            val bodyClose = TokenWidget(this, "}")
            Constants.addInsertLine(bodyClose, true)
        }

        keyword.addDelete(node, block)
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation() {
        exp.setFocus()
    }
}