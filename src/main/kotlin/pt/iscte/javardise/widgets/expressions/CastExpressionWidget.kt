package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.CastExpr
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
import pt.iscte.javardise.Id
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

class CastExpressionWidget(
    parent: Composite,
    override val node: CastExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<CastExpr>(parent) {
    var expressionWidget: ExpressionWidget<*>

    val leftBracket: TokenWidget
    val type: Id
    val rightBracket: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        leftBracket = TokenWidget(this, "(")
        type = SimpleTypeWidget(this, node.type)
        rightBracket = TokenWidget(this, ")")

        expressionWidget = drawExpression(this, node.expression)

        // TODO take care of precedence?
        observeNotNullProperty<Expression>(ObservableProperty.EXPRESSION) {
            expressionWidget.dispose()
            drawExpression(this, it)
            tailChanged()
        }

        rightBracket.addDeleteListener {
            editEvent(node.expression)
        }
    }

    private fun drawExpression(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        expressionWidget = createExpressionWidget(parent, expression) {
            if (it == null)
                editEvent(NameExpr(Configuration.fillInToken))
            else
                node.modifyCommand(node.expression, it, node::setExpression)
        }
        expressionWidget.requestLayout()
        expressionWidget.setFocusOnCreation()
        return expressionWidget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expressionWidget.setFocus()
    }

    override val tail: TextWidget
        get() = expressionWidget.tail

    override val head: TextWidget
        get() = leftBracket
}

