package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.ObserverWidget
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.empty
import pt.iscte.javardise.external.indexOfIdentity

abstract class StatementWidget<T : Statement>(
    parent: SequenceWidget,
    override val node: T
) : ObserverWidget<T>(parent) {

    abstract val parentBlock: BlockStmt

    open val keyword: TextWidget? = null

    abstract val tail: TextWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor
    }

    override val control: Control
        get() = this

    fun TokenWidget.addDelete(node: Statement, block: BlockStmt) =
        addKeyEvent(SWT.BS) {
            block.statements.replaceCommand(block, node, block.empty())
        }
}

internal fun TextWidget.addEmptyStatement(
    nodeWidget: NodeWidget<*>,
    block: BlockStmt,
    location: Statement? = null,
    after: Boolean = true
) {

    addKeyEvent(SWT.CR) {
        with(nodeWidget) {
            if (location == null)
                block.statements.addCommand(block, block.empty(), 0)
            else
                block.statements.addCommand(
                    block,
                    block.empty(),
                    block.statements.indexOfIdentity(location) + if (after) 1 else 0
                )
        }
    }
}


abstract class StatementFeature<M : Statement, W : NodeWidget<*>>(val modelClass: Class<M>, val widgetClass: Class<W>) {
    init {
        val paramTypes = widgetClass.constructors[0].parameterTypes
        require(paramTypes.size == 3)
        require(paramTypes[0] == SequenceWidget::class.java)
        require(Statement::class.java.isAssignableFrom(paramTypes[1]))
        require(paramTypes[2] == BlockStmt::class.java)
    }

    fun create(parent: SequenceWidget, stmt: Statement, block: BlockStmt): NodeWidget<M> =
        widgetClass.constructors[0].newInstance(parent, stmt, block) as NodeWidget<M>

    open fun targets(stmt: Statement) = modelClass.isInstance(stmt)

    abstract fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    )
}


fun SequenceWidget.findIndexByModel(control: Control): Int {
    var i = 0
    for (c in children) {
        if (c === control) return i
        if (c is NodeWidget<*>) i++

    }
    check(false)
    return -1
}

fun <T : Node> SequenceWidget.find(predicate: (T) -> Boolean): NodeWidget<T>? =
    children.find { predicate((it as NodeWidget<T>).node) } as NodeWidget<T>

fun <T : Node> SequenceWidget.find(e: T): NodeWidget<T>? =
    children.find { it is NodeWidget<*> && it.node === e } as NodeWidget<T>?

fun SequenceWidget.findByModelIndex(index: Int): NodeWidget<*>? {
    var i = 0
    for (c in children) if (c is NodeWidget<*>) if (i == index) return c
    else i++
    return null
}


class UnsupportedStatementWidget<T : Statement>(parent: SequenceWidget,
                                       override val parentBlock: BlockStmt,
                                       override val node: T) :
    StatementWidget<T>(parent, node) {

    val widget: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        widget = TokenWidget(this, node.toString())
        widget.widget.font = configuration.font
        widget.widget.foreground = parent.foreground
        widget.widget.background = configuration.fillInColor
        widget.setToolTip("Unsupported")
        widget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
    override val tail: TextWidget
        get() = widget

    override val control: Control
        get() = this
}



