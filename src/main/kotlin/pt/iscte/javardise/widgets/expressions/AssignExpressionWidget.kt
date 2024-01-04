package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.Configuration.Companion.hole
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

class AssignExpressionWidget(
    parent: Composite,
    override val node: AssignExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<AssignExpr>(parent) {

    var target: ExpressionWidget<*>
    val operator: TokenWidget
    var value: ExpressionWidget<*>
    init {
        target = createTargetWidget(node.target)

        operator = TokenWidget(this, node.operator.asString(), node, {
            AssignExpr.Operator.values().map { it.asString() }
        }) {
            val find = AssignExpr.Operator.values()
                .find { op -> op.asString() == it }!!

            node.modifyCommand(node.operator, find, node::setOperator)
        }

        value = createValueWidget(node.value)

        observeNotNullProperty<Expression>(ObservableProperty.TARGET) {
            target.dispose()
            target = createTargetWidget(it)
            target.moveAbove(operator.widget)
            target.requestLayout()
            target.setFocusOnCreation()
        }
        observeNotNullProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it.asString())
            value.setFocus()
        }
        observeNotNullProperty<Expression>(ObservableProperty.VALUE) {
            value.dispose()
            value = createValueWidget(it)
            value.moveBelow(operator.widget)
            value.requestLayout()
            value.setFocusOnCreation()
        }
    }

    private fun Composite.createTargetWidget(target: Expression) : ExpressionWidget<*> {
        val w = createExpressionWidget(this, target) {
            if (it == null)
                editEvent(null)
            else if(it is NameExpr || it is ArrayAccessExpr || it is FieldAccessExpr)
                node.modifyCommand(node.target, it, node::setTarget)
            else {
                this@AssignExpressionWidget.target.dispose()
                this@AssignExpressionWidget.target =
                    createTargetWidget(node.target)
                this@AssignExpressionWidget.target.moveAbove(operator.widget)
                this@AssignExpressionWidget.target.requestLayout()
            }
        }
        w.head.addKeyEvent(' ', precondition = { node.target is NameExpr}) {
            if(w.head.isAtBeginning)
                editEvent(
                    VariableDeclarationExpr(
                        VariableDeclarator(
                            StaticJavaParser.parseType(Configuration.fillInToken), //Configuration.fillInToken
                            node.target.toString(),
                            node.value.clone()
                        )
                    )
                )
            else {
                val left = w.head.textUntilCursor
                val right = w.head.textAfterCursor
                if(isValidType(left) && isValidSimpleName(right)) {
                    editEvent(
                        VariableDeclarationExpr(
                            VariableDeclarator(
                                StaticJavaParser.parseType(left),
                                right,
                                node.value.clone()
                            )
                        )
                    )
                }
            }
        }
        w.head.addKeyEvent('.', precondition = { node.target is NameExpr}) {
            if(!w.head.isAtBeginning && !w.tail.isAtBeginning) {
                val left = w.head.textUntilCursor
                val right = w.head.textAfterCursor
                if((isValidSimpleName(left) || left == "this") && isValidSimpleName(right)) {
                    val scope = if(left == "this") ThisExpr() else NameExpr(left)
                    editEvent(
                        AssignExpr(FieldAccessExpr(scope, right),
                            node.value.clone(),node.operator)
                    )
                }
            }
        }
        return w;
    }

    private fun Composite.createValueWidget(expression: Expression) =
        createExpressionWidget(this, expression) {
            if(it == null)
                editEvent(hole())
            else
                node.modifyCommand(node.value, it, node::setValue)
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if(firstFlag)
            target.setFocusOnCreation()
        else
            value.setFocusOnCreation()
    }

    override val head: TextWidget
        get() = target.head

    override val tail: TextWidget
        get() = value.tail
}


object AssignmentFeature : StatementFeature<ExpressionStmt, ExpressionStatementWidget>(ExpressionStmt::class.java, ExpressionStatementWidget::class.java) {
    override fun targets(stmt: Statement): Boolean =
        stmt is ExpressionStmt && stmt.expression is AssignExpr
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent('=', precondition = {
            var target = it.trim()
            if (target.isNotEmpty() && assignOperators.any {
                    it.asString().startsWith(target.last())
                }) {
                target = target.dropLast(1)
            }
            tryParse<NameExpr>(target) || tryParse<ArrayAccessExpr>(target) || tryParse<FieldAccessExpr>(
                target
            )
        }) {
            var target = insert.text.trim()
            val operator =
                assignOperators.find { it.asString().startsWith(target.last()) }
                    ?: AssignExpr.Operator.ASSIGN
            if (operator != AssignExpr.Operator.ASSIGN)
                target = target.dropLast(1)
            val stmt = ExpressionStmt(
                AssignExpr(
                    StaticJavaParser.parseExpression(target),
                    Configuration.hole(),
                    operator
                )
            )
            output(stmt)
        }
    }
}