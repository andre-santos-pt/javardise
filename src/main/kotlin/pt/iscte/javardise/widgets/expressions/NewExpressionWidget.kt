package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Commands
import pt.iscte.javardise.Factory
import pt.iscte.javardise.ModifyCommand
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

// TODO multi level arrays
class NewExpressionWidget(
    parent: Composite,
    override val node: ArrayCreationExpr
) : ExpressionWidget<ArrayCreationExpr>(parent) {

    val levelWidgets = mutableListOf<ExpressionWidget<*>>()
    val closeBrackets = mutableListOf<TokenWidget>()

    init {
        layout = ROW_LAYOUT_H_SHRINK
        if(node.initializer.isPresent) {
            println("init ${node.initializer.get()}")
        }
        else {
            Factory.newKeywordWidget(this, "new")
            val id = SimpleTypeWidget(this, node.elementType) {
                it.asString()
            }
            id.addFocusLostAction(::isValidType) {
                Commands.execute(object :
                    ModifyCommand<Type>(node, node.elementType) {
                    override fun run() {
                        node.elementType = StaticJavaParser.parseType(id.text)
                    }

                    override fun undo() {
                        node.elementType = element
                    }
                })
            }
            node.levels.forEachIndexed { i, level ->
                FixedToken(this, "[")
                closeBrackets.add(TokenWidget(this, "]"))
                if (level.dimension.isPresent) {
                    createExpLevel(i, level.dimension.get())
                }
            }

            node.observeProperty<Type>(ObservableProperty.ELEMENT_TYPE) {
                id.set(it!!.asString())
            }
            node.levels.observeList(object : ListObserver<ArrayCreationLevel> {
                override fun elementAdd(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    node: ArrayCreationLevel
                ) {
                    TODO("Not yet implemented")
                }

                override fun elementRemove(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    node: ArrayCreationLevel
                ) {
                    TODO("Not yet implemented")
                }

                override fun elementReplace(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    old: ArrayCreationLevel,
                    new: ArrayCreationLevel
                ) {
                    levelWidgets[index].dispose()
                    levelWidgets[index] = createExpLevel(
                        index,
                        new.dimension.get()
                    )
                    levelWidgets[index].requestLayout()
                }

            })
        }
    }

    private fun createExpLevel(
        index: Int,
        expression: Expression
    ): ExpressionWidget<*> {
        val w = createExpressionWidget(this, expression) {
            Commands.execute(object :
                ModifyCommand<ArrayCreationLevel>(node, node.levels[index]) {
                override fun run() {
                    node.levels[index] = ArrayCreationLevel(it)
                }

                override fun undo() {
                    node.levels[index] = element
                }
            })
        }
        levelWidgets.add(index, w)
        w.moveAbove(closeBrackets[index].widget)
        return w
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        levelWidgets.first().setFocus()
    }

    override val tail: TextWidget
        get() = closeBrackets.last()
}