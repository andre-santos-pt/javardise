package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.external.*
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Commands
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.widgets.expressions.ExpWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget
import pt.iscte.javardise.widgets.members.addInsert

// to expression?
class AssignWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent, node) {
    var target: ExpWidget<*>
    var operator: TokenWidget
    var value: ExpWidget<*>
    val assignment = node.expression as AssignExpr

    init {
        require(node.expression is AssignExpr)
        require(assignment.target is NameExpr || assignment.target is FieldAccessExpr || assignment.target is ArrayAccessExpr)

        layout = ROW_LAYOUT_H_SHRINK

        target = createTargetWidget(assignment.target)
        //target.widget.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))

        operator = TokenWidget(this, assignment.operator.asString(), {
            AssignExpr.Operator.values().map { it.asString() }
        }) {
            assignment.operator = AssignExpr.Operator.values().find { op -> op.asString() == it }
        }

        value = createValueWidget(assignment.value)

        if (this@AssignWidget.parent is SequenceWidget)
            TokenWidget(this, ";").addInsert(this@AssignWidget, this@AssignWidget.parent as SequenceWidget, true)

        assignment.observeProperty<Expression>(ObservableProperty.TARGET) {
            target.dispose()
            target = createTargetWidget(it!!)
            target.moveAbove(operator.widget)
            target.requestLayout()
        }
        assignment.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "??")
            value.setFocus()
        }
        assignment.observeProperty<Expression>(ObservableProperty.VALUE) {
            value.dispose()
            value = createValueWidget(it!!)
            value.moveBelow(operator.widget)
            value.requestLayout()
        }
    }

    private fun Composite.createTargetWidget(target: Expression) =
        createExpressionWidget(this, target) {
            Commands.execute(object : ModifyCommand<Expression>(assignment, assignment.target) {
                override fun run() {
                    assignment.target = it
                }

                override fun undo() {
                    assignment.target = element
                }
            })
        }

    private fun Composite.createValueWidget(expression: Expression) =
        createExpressionWidget(this, expression) {
            Commands.execute(object : ModifyCommand<Expression>(assignment, assignment.value) {
                override fun run() {
                    assignment.value = it
                }

                override fun undo() {
                    assignment.value = element
                }
            })
        }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        value.setFocus()
    }
}