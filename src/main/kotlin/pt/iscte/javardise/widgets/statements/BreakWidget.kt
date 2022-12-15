package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.setCopySource
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class BreakWidget(
    parent: SequenceWidget,
    node: BreakStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<BreakStmt>(parent, node) {
    val keyword: TokenWidget
    val semiColon: TokenWidget

    init {
        keyword = newKeywordWidget(this, "break")
        keyword.addDelete(node, parentBlock)
        keyword.addEmptyStatement(this, parentBlock, node, false)
        keyword.setCopySource(node)

        semiColon = TokenWidget(this, ";")
        semiColon.addDelete(node, parentBlock)
        semiColon.addEmptyStatement(this, parentBlock, node)
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        semiColon.setFocus()
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