package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget

class EmptyStatement(
    parent: SequenceWidget,
    node: EmptyStmt,
    override val block: BlockStmt,
    accept: ((Char, String) -> Boolean)? = null
) :
    StatementWidget<EmptyStmt>(parent, node), TextWidget   {

    val semiColon: TextWidget

    init {
        semiColon = TextWidget.create(this, if(accept == null) ";" else "")
        //semiColon.addInsert(semiColon.widget, parent, true)
        //semiColon.addDelete(node, block)
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