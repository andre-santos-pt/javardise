package javawidgets.statements

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import javawidgets.SimpleNameWidget
import javawidgets.StatementWidget
import javawidgets.createDeleteEvent
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import pt.iscte.javardise.api.row

class CallWidget(parent: SequenceWidget,
               node: ExpressionStmt, override val block: BlockStmt) :
    StatementWidget<ExpressionStmt>(parent, node) {
    lateinit var target: Id
    lateinit var value: Id

    init {
        require(node.expression is MethodCallExpr)
        val call = node.expression as MethodCallExpr

        layout = FillLayout()
        row {
            if (call.scope.isPresent) {
                target = SimpleNameWidget(this, call.scope.get()) { it.toString() }
                target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
                FixedToken(this, ".")
            }
            value = SimpleNameWidget(this, call.name){it.asString()}
            FixedToken(this, "(")
            // TODO args
            FixedToken(this, ")")
            FixedToken(this, ";")
        }
    }

    override fun setFocus(): Boolean {
        return value.setFocus()
    }

    override fun setFocusOnCreation() {
        value.setFocus()
    }
}