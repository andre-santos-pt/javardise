package javawidgets.statements

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import javawidgets.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import row

class VariableWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent, node) {
    lateinit var type: Id
    lateinit var target: Id
    lateinit var expression: ExpressionFreeWidget

    init {
        require(node.expression is VariableDeclarationExpr)

        val assignment = node.expression as VariableDeclarationExpr
        val decl = assignment.variables[0] // TODO multi

        layout = FillLayout()
        row {
            type = SimpleTypeWidget(this, decl.type) { it.asString() }
            target = SimpleNameWidget(this, decl.name) { it.asString() }
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))

            if (decl.initializer.isPresent) {
                FixedToken(this, "=")
                expression = ExpressionFreeWidget(this, decl.initializer.get()) {
                    Commands.execute(object : ModifyCommand<Expression>(decl, decl.initializer.get()) {
                        override fun run() {
                            decl.setInitializer(it)
                        }

                        override fun undo() {
                            decl.setInitializer(element)
                        }
                    })
                }
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

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }
}