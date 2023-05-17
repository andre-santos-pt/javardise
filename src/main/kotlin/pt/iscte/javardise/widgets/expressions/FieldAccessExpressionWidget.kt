package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.ID
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING
import pt.iscte.javardise.external.isValidSimpleName
import pt.iscte.javardise.nodeText

class FieldAccessExpressionWidget(
    parent: Composite,
    override val node: FieldAccessExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<FieldAccessExpr>(parent) {

    var scopeWidget: ExpressionWidget<*>?
    val dot: FixedToken
    val nameWidget: TextWidget

    init {
        layout = ROW_LAYOUT_H_STRING

        dot = FixedToken(this, ".")
        nameWidget = TextWidget.create(this, nodeText(node.name)) { c, _, _ ->
            c.toString().matches(ID) || c == SWT.BS
        }
        scopeWidget = drawScope(node.scope)

        observeNotNullProperty<Expression>(ObservableProperty.SCOPE) {
            scopeWidget?.dispose()
            scopeWidget = drawScope(it)
        }

        observeNotNullProperty<SimpleName>(ObservableProperty.NAME) {
            nameWidget.text = it.asString()
        }

        fun nameText() =
            if(nameWidget.text.isBlank()) Configuration.fillInToken
            else
                nameWidget.text

        nameWidget.addFocusLostAction({ isValidSimpleName(it) }) {
            node.modifyCommand(node.name, SimpleName(nameText()), node::setName)
        }

        nameWidget.addKeyEvent('.') {
            editEvent(FieldAccessExpr(FieldAccessExpr(node.scope.clone(), nameText()), Configuration.fillInToken))
        }

        nameWidget.addKeyEvent('(') {
            editEvent(MethodCallExpr(node.scope.clone(), nameText()))
        }

        nameWidget.addDeleteEmptyListener {
            editEvent(node.scope.clone())
        }
    }

    private fun drawScope(
        expression: Expression
    ): ExpressionWidget<*> {
        scopeWidget = createExpressionWidget(this, expression) {
            if (it == null)
                editEvent(null)
            else if(it is NameExpr || it is MethodCallExpr || it is ArrayAccessExpr)
                node.modifyCommand(node.scope, it, node::setScope)
        }
        scopeWidget!!.moveAbove(dot.label)
        scopeWidget!!.requestLayout()
        scopeWidget!!.setFocusOnCreation()
        return scopeWidget as ExpressionWidget<*>
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        nameWidget.setFocus()
    }

    override val head: TextWidget
        get() = scopeWidget?.head ?: nameWidget

    override val tail: TextWidget
        get() = nameWidget
}