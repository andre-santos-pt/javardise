package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.ObjectCreationExpr
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Factory
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.isValidClassType
import pt.iscte.javardise.modifyCommand

// anonymous class body
class NewObjectExpressionWidget(
    parent: Composite,
    override val node: ObjectCreationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ObjectCreationExpr>(parent) {

    val id: TextWidget
    val args: ArgumentListWidget<Expression>

    init {
        if (node.scope.isPresent) {
            TODO("new Obj scope")
        }

        Factory.newKeywordWidget(this, "new")
        id = SimpleTypeWidget(this, node.type) {
            it.asString()
        }
        id.addFocusLostAction(::isValidClassType) {
            node.modifyCommand(node.typeAsString, it, node::setType)
        }

        args = ArgumentListWidget(this, "(", ")", node, node.arguments)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val tail: TextWidget
        get() = args.closeBracket
}