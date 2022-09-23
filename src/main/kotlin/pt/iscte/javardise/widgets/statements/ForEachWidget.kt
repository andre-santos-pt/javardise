package pt.iscte.javardise.widgets.statements

import pt.iscte.javardise.external.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import org.eclipse.swt.layout.RowLayout
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.ExpressionFreeWidget

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