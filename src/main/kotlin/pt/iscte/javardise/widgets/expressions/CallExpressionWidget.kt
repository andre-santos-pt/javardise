package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.Id
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING
import pt.iscte.javardise.external.isValidSimpleName
import pt.iscte.javardise.external.observeProperty

class CallExpressionWidget(
    parent: Composite,
    override val node: MethodCallExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<MethodCallExpr>(parent) {

    private var target: Id? = null
    private var methodName: Id
    private val args: ArgumentListWidget

    init {
        layout = ROW_LAYOUT_H_STRING
        if (node.scope.isPresent) {
            target = SimpleNameWidget(this, node.scope.get()) { it.toString() }
            target!!.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                //action = createDeleteEvent(node, block))
            }
            FixedToken(this, ".")
        }
        methodName = SimpleNameWidget(this, node.name) { it.asString() }
        methodName.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(node.name, SimpleName(methodName.text), node::setName)
        }
        args = ArgumentListWidget(this, "(", ")", node, node.arguments)

        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            methodName.set(it?.asString() ?: node.name.asString())
        }
    }

    override val tail: TextWidget
        get() = args.closeBracket

    override fun setFocus(): Boolean {
        return methodName.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        args.setFocus()
    }
}