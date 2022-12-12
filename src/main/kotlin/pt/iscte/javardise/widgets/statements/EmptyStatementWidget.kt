package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.indexOfIdentity
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy

class EmptyStatementWidget(
    parent: SequenceWidget,
    node: EmptyStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<EmptyStmt>(parent, node), TextWidget   {

    val semiColon: TextWidget

    init {
        require(!node.comment.isPresent)
       background = Display.getDefault().getSystemColor(SWT.COLOR_CYAN)

        semiColon = TextWidget.create(this, "") { c, s ->
            c.toString().matches(Regex("\\w|\\[|]|\\.|\\+|-|\\*|/|%"))
                    || c == SWT.SPACE && !s.endsWith(SWT.SPACE)
                    || c == SWT.BS
        }
        semiColon.addKeyEvent(SWT.BS, precondition = {it.isEmpty()}) {
            parentBlock.statements.removeCommand(parentBlock, node)
        }

        semiColon.addKeyEvent(SWT.CR) {
            parentBlock.statements.addCommand(parentBlock, EmptyStmt(), parentBlock.statements.indexOfIdentity(node)+1)
        }

        semiColon.addFocusLostAction {
            if(semiColon.text.startsWith("//")) {
                val stmt = EmptyStmt()
                stmt.setComment(LineComment(semiColon.text.substring(2)))
                parentBlock.statements.replaceCommand(parentBlock, node, stmt)
            }
            else
                semiColon.clear()
        }

        configuration.statementFeatures.forEach {
            it.configureInsert(semiColon, parentBlock, node,commandStack) {
                parentBlock.statements.replaceCommand(parentBlock, node, it)
            }
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        semiColon.setFocus()
    }

    override val widget: Text
        get() = semiColon.widget

    override fun setFocus(): Boolean {
        return semiColon.setFocus()
    }

    override fun addKeyListenerInternal(listener: KeyListener) {
        semiColon.addKeyListenerInternal(listener)
    }
}

object EmptyStatementFeature : StatementFeature<EmptyStmt, EmptyStatementWidget>(EmptyStmt::class.java, EmptyStatementWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
//        insert.addKeyEvent(SWT.CR, precondition = { it.isEmpty() }) {
//            output(EmptyStmt())
//        }
    }

    override fun targets(stmt: Statement): Boolean {
        return super.targets(stmt) && !stmt.comment.isPresent
    }
}
