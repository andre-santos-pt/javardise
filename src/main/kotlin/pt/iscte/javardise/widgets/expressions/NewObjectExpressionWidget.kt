package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.ClassOrInterfaceType
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.isValidClassType

// anonymous class body
class NewObjectExpressionWidget(
    parent: Composite,
    override val node: ObjectCreationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ObjectCreationExpr>(parent) {

    val id: TextWidget
    val args: ExpressionListWidget<Expression, ObjectCreationExpr>

    val keyword: TokenWidget

    init {
        if (node.scope.isPresent) {
            TODO("new Obj scope")
        }

        keyword = newKeywordWidget(this, "new")
        id = SimpleTypeWidget(this, node.type)
        id.addFocusLostAction(::isValidClassType) {
            node.modifyCommand(node.typeAsString, it, node::setType)
        }

        args = ExpressionListWidget(this, "(", ")", this, node.arguments) {
            editEvent(Configuration.hole())
        }

        observeNotNullProperty<ClassOrInterfaceType>(ObservableProperty.TYPE) {
            id.text = it.nameAsString
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val head: TextWidget
        get() = keyword

    override val tail: TextWidget
        get() = args.closeBracket
}