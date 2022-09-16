package javawidgets.statements

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import javawidgets.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import pt.iscte.javardise.api.row

class AssignWidget(
    parent: SequenceWidget,
    override val node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent) {
    lateinit var target: ExpWidget
    lateinit var operator: TokenWidget
    lateinit var expression: ExpWidget

    init {
        require(node.expression is AssignExpr)

        val assignment = node.expression as AssignExpr

        layout = FillLayout()
        row {

            // TODO check valid target
            target = ExpWidget(this, assignment.target) {
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

            operator = TokenWidget(this, "=") {
                listOf("+=", "-=", "*=")
            }

            expression = ExpWidget(this, assignment.value) {
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
            TODO()
        }
        assignment.observeProperty<Expression>(ObservableProperty.VALUE) {
            expression.update(it)
        }
    }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation() {
        expression.setFocus()
    }
}