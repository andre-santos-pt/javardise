package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.ContinueStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.setCopySource

class ContinueWidget(
    parent: SequenceWidget,
    node: ContinueStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<ContinueStmt>(parent, node) {
    override val keyword: TokenWidget
    override val tail: TextWidget

    init {
        keyword = newKeywordWidget(this, "continue", node)
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

object ContinueFeature : StatementFeature<ContinueStmt, ContinueWidget>(ContinueStmt::class.java, ContinueWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "continue" }) {
            output(ContinueStmt())
        }
    }
}