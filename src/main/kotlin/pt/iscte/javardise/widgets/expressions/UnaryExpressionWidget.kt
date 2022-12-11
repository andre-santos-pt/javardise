package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

class UnaryExpressionWidget(
    parent: Composite,
    override val node: UnaryExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<UnaryExpr>(parent) {
    var operator: TokenWidget
    var expressionWidget: ExpressionWidget<*>
    val expressionObserver: AstObserver
    val operatorObserver: AstObserver

    init {
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = {
                if (node.isPrefix)
                    unaryOperators.filter { it.isPrefix }.map { it.asString() }
                else
                    unaryOperators.filter { it.isPostfix }.map { it.asString() }
            }) {
            node.modifyCommand(
                node.operator,
                if (node.isPrefix)
                    unaryOperators.filter { it.isPrefix }
                        .find { op -> op.asString() == it }
                else
                    unaryOperators.filter { it.isPostfix }
                        .find { op -> op.asString() == it },
                node::setOperator
            )
        }
        operator.addDeleteListener {
            editEvent(node.expression.clone())
        }

        expressionWidget = drawExpression(this, node.expression)

        operator.addDeleteListener {
            editEvent(null)
        }

        operatorObserver =
            node.observeNotNullProperty<UnaryExpr.Operator>(ObservableProperty.OPERATOR) {
                operator.set(it.asString())
                operator.widget.traverse(SWT.TRAVERSE_TAB_NEXT)
            }

        expressionObserver =
            node.observeNotNullProperty<Expression>(ObservableProperty.EXPRESSION) {
                expressionWidget.dispose()
                drawExpression(this, it)
                tailChanged()
            }


    }

    private fun drawExpression(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        expressionWidget = createExpressionWidget(parent, expression) {
            if(it == null)
                editEvent(NameExpr(Configuration.fillInToken))
            else
                node.modifyCommand(node.expression, it, node::setExpression)
        }
        if (node.isPostfix)
            expressionWidget.moveAbove(operator)
        expressionWidget.requestLayout()
        expressionWidget.setFocusOnCreation()
        return expressionWidget
    }

    override fun dispose() {
        super.dispose()
        node.unregister(expressionObserver)
        node.unregister(operatorObserver)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expressionWidget.setFocus()
    }

    override val tail: TextWidget
        get() = expressionWidget.tail

    override val head: TextWidget
        get() = operator
}

object UnaryExpressionStatementFeature : StatementFeature<ExpressionStmt, ExpressionStatementWidget>(
    ExpressionStmt::class.java,
    ExpressionStatementWidget::class.java
) {
    override fun targets(stmt: Statement): Boolean =
        stmt is ExpressionStmt && stmt.expression is UnaryExpr

    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(';', precondition = {
            tryParse<UnaryExpr>(insert.text) &&
                    (StaticJavaParser.parseExpression(insert.text) as UnaryExpr).operator in unaryOperatorsStatement

        }) {
            output(ExpressionStmt(StaticJavaParser.parseExpression(insert.text)))
        }
    }

}