package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.modifyCommand

class AssignExpressionWidget(
    parent: Composite,
    override val node: AssignExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<AssignExpr>(parent) {

    var target: ExpressionWidget<*>
    val operator: TokenWidget
    var value: ExpressionWidget<*>


    init {
        layout = ROW_LAYOUT_H_SHRINK

        target = createTargetWidget(node.target)
        //target.widget.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))

        operator = TokenWidget(this, node.operator.asString(), {
            AssignExpr.Operator.values().map { it.asString() }
        }) {
            val find = AssignExpr.Operator.values()
                .find { op -> op.asString() == it }!!

            node.modifyCommand(node.operator, find, node::setOperator)
        }

        value = createValueWidget(node.value)

//        if (this.parent is SequenceWidget)
//            TokenWidget(this, ";").addInsert(this@AssignWidget, this@AssignWidget.parent as SequenceWidget, true)

        node.observeProperty<Expression>(ObservableProperty.TARGET) {
            target.dispose()
            target = createTargetWidget(it!!)
            target.moveAbove(operator.widget)
            target.requestLayout()
            target.setFocus()
        }
        node.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "??")
            value.setFocus()
        }
        node.observeProperty<Expression>(ObservableProperty.VALUE) {
            value.dispose()
            value = createValueWidget(it!!)
            value.moveBelow(operator.widget)
            value.requestLayout()
            value.setFocusOnCreation()
        }
    }

    private fun Composite.createTargetWidget(target: Expression) =
        createExpressionWidget(this, target) {
            if(it == null)
                editEvent(null)
            else node.modifyCommand(node.target, it, node::setTarget)
        }

    private fun Composite.createValueWidget(expression: Expression) =
        createExpressionWidget(this, expression) {
            if(it == null)
                editEvent(null)
            else
                node.modifyCommand(node.value, it, node::setValue)
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        value.setFocus()
    }

    override val tail: TextWidget
        get() = value.tail
}