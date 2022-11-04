package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget

class EmptyStatementWidget(
    parent: SequenceWidget,
    node: EmptyStmt,
    override val block: BlockStmt
) :
    StatementWidget<EmptyStmt>(parent, node), TextWidget   {

    val semiColon: TextWidget

    init {
        semiColon = TokenWidget(parent, ";")
        //semiColon.addInsert(semiColon.widget, parent, true)
        semiColon.addDelete(node, block)
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
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, ';', precondition = { it.isEmpty() }) {
            output(EmptyStmt())
        }
    }

}