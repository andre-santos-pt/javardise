package javawidgets

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import pt.iscte.javardise.api.row

class AssignWidget(
    parent: SequenceWidget,
    override val node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent) {
    lateinit var target: Id
    lateinit var expression: ExpWidget

    init {
        require(node.expression is AssignExpr)
        val assignment = node.expression as AssignExpr

        layout = FillLayout()
        row {
            target = Id(this, assignment.target.toString())
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
            FixedToken(this, "=")
            expression = ExpWidget(this, assignment.value) {
                Commands.execute(object : Command {
                    val old = assignment.value
                    override fun run() {
                        assignment.value = it
                    }

                    override fun undo() {
                        assignment.value = old.clone()
                    }
                })
            }
            FixedToken(this, ";")
        }

        node.observeProperty<Expression>(ObservableProperty.TARGET) {
            TODO()
        }
        node.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            TODO()
        }
        node.observeProperty<Expression>(ObservableProperty.VALUE) {
            expression.update(it!!)
        }
    }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation() {
        expression.setFocus()
    }
}