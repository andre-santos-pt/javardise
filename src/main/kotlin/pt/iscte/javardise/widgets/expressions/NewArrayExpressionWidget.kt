package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ListObserver
import pt.iscte.javardise.external.isValidType
import pt.iscte.javardise.external.observeList
import pt.iscte.javardise.external.observeProperty

// TODO multi level arrays
class NewArrayExpressionWidget(
    parent: Composite,
    override val node: ArrayCreationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<ArrayCreationExpr>(parent) {

    val levelWidgets = mutableListOf<LevelWidget>()
    val keyword: TokenWidget

    data class LevelWidget(val open: FixedToken, var expression: Control, val close: TokenWidget) {
        fun dispose() {
            open.dispose()
            expression.dispose()
            close.dispose()
        }

        fun moveExpression() {
            expression.moveAbove(close.widget)
        }

        fun moveAbove(c: Control) {
            open.moveAbove(c)
            expression.moveBelow(open.label)
            close.widget.moveBelow(expression)
        }

        fun setFocus() {
            expression.setFocus()
        }
    }
    init {
        if(node.initializer.isPresent) {
            TODO("array init ${node.initializer.get()}")
        }
        else {
            keyword = newKeywordWidget(this, "new")
            val id = SimpleTypeWidget(this, node.elementType)
            id.addFocusLostAction(::isValidType) {
                node.modifyCommand(node.elementType, StaticJavaParser.parseType(id.text), node::setElementType)
            }
            node.levels.forEachIndexed { i, level ->
                val open = FixedToken(this, "[")
                val explevel = if(level.dimension.isPresent)
                    createExpLevel(i, level.dimension.get())
                else
                    TextWidget.create(this, " ").widget
                val close = TokenWidget(this, "]")
                close.addDeleteListener {
                    if(node.levels.size > 1)
                        node.levels.removeCommand(node, level)
                }
                close.addKeyEvent('[') {
                    node.levels.addCommand(node,ArrayCreationLevel(0), i+1)
                }

                levelWidgets.add(LevelWidget(open, explevel, close))
            }

            node.observeProperty<Type>(ObservableProperty.ELEMENT_TYPE) {
                id.set(it?.asString() ?: "??")
            }
            node.levels.observeList(object : ListObserver<ArrayCreationLevel> {
                override fun elementAdd(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    n: ArrayCreationLevel
                ) {
                    val open = FixedToken(this@NewArrayExpressionWidget, "[")
                    val exp = createExpLevel(index, n.dimension.get())
                    val close = TokenWidget(this@NewArrayExpressionWidget, "]")
                    close.addDeleteListener {
                        if(node.levels.size > 1)
                            node.levels.removeCommand(node, n)  // TODO BUG Index -1 out of bounds for length 3
                    }
                    val level = LevelWidget(open, exp, close)
                    if(index != node.levels.size)
                        level.moveAbove(levelWidgets[index].open.label)
                    levelWidgets.add(index, level)
                    level.expression.requestLayout()
                    level.expression.setFocus()
                }

                override fun elementRemove(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    node: ArrayCreationLevel
                ) {
                    levelWidgets[index].dispose()
                    levelWidgets.removeAt(index)
                    setFocus()
                    requestLayout()
                }

                override fun elementReplace(
                    list: NodeList<ArrayCreationLevel>,
                    index: Int,
                    old: ArrayCreationLevel,
                    new: ArrayCreationLevel
                ) {
                    levelWidgets[index].expression.dispose()
                    levelWidgets[index].expression = createExpLevel(
                        index,
                        new.dimension.get()
                    )
                    levelWidgets[index].moveExpression()
                    levelWidgets[index].expression.requestLayout()
                }

            })
        }
    }

    private fun createExpLevel(
        index: Int,
        expression: Expression
    ): ExpressionWidget<*> {
        val w = createExpressionWidget(this, expression) {
            node.levels.changeCommand(node, ArrayCreationLevel(it), index )
            //node.modifyCommand(node.levels[index], ArrayCreationLevel(it), node.levels::set)
//            Commands.execute(object :
//                ModifyCommand<ArrayCreationLevel>(node, node.levels[index]) {
//                override fun run() {
//                    node.levels[index] = ArrayCreationLevel(it)
//                }
//
//                override fun undo() {
//                    node.levels[index] = element
//                }
//            })
        }
        return w
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        levelWidgets.first().setFocus()
    }

    override val head: TextWidget
        get() = keyword
    override val tail: TextWidget
        get() = levelWidgets.last().close
}