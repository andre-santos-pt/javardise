package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.setCopySource

class BreakWidget(
    parent: SequenceWidget,
    node: BreakStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<BreakStmt>(parent, node) {
    override val keyword: TokenWidget
    override val tail: TextWidget
    init {
        keyword = newKeywordWidget(this, "break")
        keyword.addDelete(node, parentBlock)
        keyword.addEmptyStatement(this, parentBlock, node, false)
        keyword.setCopySource(node)

        tail = TokenWidget(this, ";")
        tail.addDelete(node, parentBlock)
        tail.addEmptyStatement(this, parentBlock, node)
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        tail.setFocus()
    }
}

object BreakFeature : StatementFeature<BreakStmt, BreakWidget>(BreakStmt::class.java, BreakWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "break" }) {
            output(BreakStmt())
        }
    }
}