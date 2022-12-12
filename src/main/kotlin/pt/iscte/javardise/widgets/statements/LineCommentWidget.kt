package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.row

class LineCommentWidget(parent: SequenceWidget,
                        override val node: EmptyStmt,
                        override val parentBlock: BlockStmt
)
    :StatementWidget<EmptyStmt>(parent, node), NodeWidget<EmptyStmt> {
    init {
        require(node.comment.isPresent)
        row {
            val slashes = TokenWidget(this, "//")
            slashes.widget.foreground = configuration.commentColor
            slashes.addDelete(node, parentBlock)
            slashes.addEmptyStatement(this@LineCommentWidget, parentBlock, node, false)
            val cmt = TextWidget.create(this, node.comment.get().content.trim()) { _, _ -> true }
            cmt.addEmptyStatement(this@LineCommentWidget, parentBlock, node)
            cmt.addFocusLostAction {
                node.modifyCommand(node.comment.getOrNull, LineComment(cmt.text), node::setComment)
            }
            cmt.widget.foreground = configuration.commentColor
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}


object LineCommentFeature : StatementFeature<EmptyStmt, LineCommentWidget>(EmptyStmt::class.java, LineCommentWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.CR, precondition = { it.startsWith("//") }) {
            val stmt = EmptyStmt()
            stmt.setComment(LineComment(insert.text.substring(2)))
            output(stmt)
        }
    }

    override fun targets(stmt: Statement): Boolean {
        return  super.targets(stmt) && stmt.comment.isPresent
    }

}