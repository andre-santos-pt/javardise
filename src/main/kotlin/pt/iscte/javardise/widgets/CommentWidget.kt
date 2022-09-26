package pt.iscte.javardise.widgets

import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.COMMENT_COLOR
import pt.iscte.javardise.Commands
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.row

class CommentWidget(parent: Composite, node: Statement) : Composite(parent, SWT.NONE) {
    init {
        layout = FillLayout()
        row {
            val slashes = TokenWidget(this, "//")
            slashes.widget.foreground = COMMENT_COLOR()
            val cmt = TextWidget.create(this, node.comment.get().content.trim()) { _, _ -> true }
            cmt.addFocusLostAction {
                Commands.execute(object : ModifyCommand<Comment>(node, node.comment.get()) {
                    override fun run() {
                        node.setComment(LineComment(cmt.text))
                    }

                    override fun undo() {
                        node.setComment(element)
                    }
                })
            }
            cmt.widget.foreground = COMMENT_COLOR()
        }
    }
}