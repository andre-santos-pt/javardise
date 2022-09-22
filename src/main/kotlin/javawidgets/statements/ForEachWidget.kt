package javawidgets.statements

import basewidgets.FixedToken
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import javawidgets.ExpressionFreeWidget
import javawidgets.StatementWidget
import javawidgets.createSequence
import org.eclipse.swt.layout.RowLayout
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

// TODO
class ForEachWidget(parent: SequenceWidget, node: ForEachStmt, override val block: BlockStmt) :
    StatementWidget<ForEachStmt>(parent, node) {

    lateinit var keyword: TokenWidget
    lateinit var iterable: ExpressionFreeWidget
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = TokenWidget(this, "for")
                FixedToken(this, "(")

                FixedToken(this, ":")
                iterable = ExpressionFreeWidget(this, node.iterable) {

                }
                FixedToken(this, ") {")
            }
            body = createSequence(this, node.asBlockStmt())
            FixedToken(this, "}")
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        iterable.setFocus()
    }

}