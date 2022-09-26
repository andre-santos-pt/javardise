package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import pt.iscte.javardise.external.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import pt.iscte.javardise.Commands
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*

class AssignWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent, node) {
    lateinit var target: ExpressionFreeWidget
    lateinit var operator: TokenWidget
    lateinit var expression: ExpressionFreeWidget

    init {
        require(node.expression is AssignExpr)

        val assignment = node.expression as AssignExpr

        layout = FillLayout()
        row {

            // TODO check valid target
            target = ExpressionFreeWidget(this, assignment.target) {
                Commands.execute(object : ModifyCommand<Expression>(assignment, assignment.target) {
                    override fun run() {
                        assignment.target = it
                    }

                    override fun undo() {
                        assignment.target = element
                    }
                })
            }
            target.setCopySource()
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))

            operator = TokenWidget(this, assignment.operator.asString(), {
                AssignExpr.Operator.values().map { it.asString() }
            }) {
                assignment.operator = AssignExpr.Operator.values().find { op -> op.asString() == it }
            }

            expression = ExpressionFreeWidget(this, assignment.value) {
                Commands.execute(object : ModifyCommand<Expression>(assignment, assignment.value) {
                    override fun run() {
                        assignment.value = it
                    }

                    override fun undo() {
                        assignment.value = element
                    }
                })
            }
            if(this@AssignWidget.parent is SequenceWidget)
                TokenWidget(this, ";").addInsert(this@AssignWidget, this@AssignWidget.parent as SequenceWidget, true)
        }

        assignment.observeProperty<Expression>(ObservableProperty.TARGET) {
            target.update(it)
        }
        assignment.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "NaO")
            expression.setFocus()
        }
        assignment.observeProperty<Expression>(ObservableProperty.VALUE) {
            expression.update(it)
        }
    }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }
}