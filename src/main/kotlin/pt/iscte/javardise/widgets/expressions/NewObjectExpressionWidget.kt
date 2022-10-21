package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Commands
import pt.iscte.javardise.Factory
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.isValidClassType

// anonymous class body
class NewObjectExpressionWidget(
    parent: Composite,
    override val node: ObjectCreationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ObjectCreationExpr>(parent) {

    val id: TextWidget
    val args: ArgumentListWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        if (node.scope.isPresent) {
            TODO()
        }

        Factory.newKeywordWidget(this, "new")
        id = SimpleTypeWidget(this, node.type) {
            it.asString()
        }
        id.addFocusLostAction(::isValidClassType) {
            Commands.execute(object :
                ModifyCommand<ClassOrInterfaceType>(node, node.type) {
                override fun run() {
                    node.type =
                        StaticJavaParser.parseClassOrInterfaceType(id.text)
                }

                override fun undo() {
                    node.type = element
                }
            })
        }

        args = ArgumentListWidget(this, "(", ")", node, node.arguments)
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val tail: TextWidget
        get() = args.closeBracket
}