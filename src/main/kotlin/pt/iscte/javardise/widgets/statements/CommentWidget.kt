package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.row

class CommentWidget(parent: Composite, override val node: Statement) : Composite(parent, SWT.NONE), NodeWidget<Statement> {
    init {
        layout = FillLayout()
        row {
            val slashes = TokenWidget(this, "//")
            slashes.widget.foreground = configuration.commentColor
            val cmt = TextWidget.create(this, node.comment.get().content.trim()) { _, _ -> true }
            cmt.addFocusLostAction {
                node.modifyCommand(node.comment.getOrNull, LineComment(cmt.text), node::setComment)
            }
            cmt.widget.foreground = configuration.commentColor
        }
    }

    override val control: Control
        get() = this

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}