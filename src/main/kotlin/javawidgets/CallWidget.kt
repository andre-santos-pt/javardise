package javawidgets

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import pt.iscte.javardise.api.row

class CallWidget(parent: SequenceWidget, override val node: ExpressionStmt, override val block: BlockStmt) :
    StatementWidget<ExpressionStmt>(parent) {
    lateinit var target: Id
    lateinit var value: Id

    init {
        require(node.expression is MethodCallExpr)
        val call = node.expression as MethodCallExpr

        layout = FillLayout()
        row {
            if (call.scope.isPresent) {
                target = Id(this, call.scope.get().toString())
                target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
                FixedToken(this, ".")
            }
            value = Id(this, call.name.asString())
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