package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

class BracketsExpressionWidget(
    parent: Composite,
    override val node: EnclosedExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<EnclosedExpr>(parent) {
    val leftBracket: TokenWidget
    val rightBracket: TokenWidget

    var expressionWidget: ExpressionWidget<*>

    init {
        leftBracket = TokenWidget(this, "(")
        rightBracket = TokenWidget(this, ")")
        expressionWidget = drawExpression(this, node.inner)

        observeNotNullProperty<Expression>(ObservableProperty.INNER) {
            expressionWidget.dispose()
            drawExpression(this, it)
        }

        leftBracket.addDeleteListener {
            editEvent(node.inner.clone())
        }

        rightBracket.addDeleteListener {
            editEvent(node.inner.clone())
        }

        rightBracket.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.stateMask == Configuration.maskKey && e.keyCode == SWT.ARROW_RIGHT) {
                    println("->")
                    val parentExp = node.parentNode.get()
                    if (parentExp is BinaryExpr && parentExp.left == node) {
                        (parent as ExpressionWidget<Expression>).editEvent(
                            EnclosedExpr(
                                BinaryExpr(
                                    node.inner.clone(),
                                    parentExp.right.clone(),
                                    parentExp.operator
                                )
                            )
                        )
                    }
                } else if (e.stateMask == Configuration.maskKey && e.keyCode == SWT.ARROW_LEFT) {
                    println("<-")
                    if (node.inner is BinaryExpr) {
                        val binner = node.inner as BinaryExpr
                        editEvent(
                            BinaryExpr(
                                EnclosedExpr(binner.left.clone()),
                                binner.right.clone(), binner.operator
                            )
                        )
                    }
                }
                else if(e.character == SWT.SPACE && isValidType(expressionWidget.head.text)) {
                    editEvent(CastExpr(StaticJavaParser.parseType(expressionWidget.head.text),Configuration.hole()))
                }
            }
        })
    }

    private fun drawExpression(
        parent: Composite,
        expression: Expression
    ): ExpressionWidget<*> {
        expressionWidget = createExpressionWidget(parent, expression) {
            if (it == null)
                editEvent(Configuration.hole())
            else
                node.modifyCommand(node.inner, it, node::setInner)
        }
        expressionWidget.moveAbove(rightBracket)
        expressionWidget.requestLayout()
        expressionWidget.setFocusOnCreation()
        return expressionWidget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expressionWidget.setFocusOnCreation(firstFlag)
    }

    override val head: TextWidget
        get() = leftBracket

    override val tail: TextWidget
        get() = rightBracket
}