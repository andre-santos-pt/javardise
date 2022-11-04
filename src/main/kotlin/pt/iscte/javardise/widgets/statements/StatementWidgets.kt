package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.Commands
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK

abstract class StatementWidget<T : Statement>(
    parent: SequenceWidget,
    override val node: T
) :
    Composite(parent, SWT.NONE), NodeWidget<T> {

    abstract val block: BlockStmt

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor
        if (node.comment.isPresent) CommentWidget(this, node)
    }

    override val control: Control
        get() = this

}


abstract class StatementFeature<M: Statement, W: NodeWidget<*>>(val modelClass: Class<M>, val widgetClass: Class<W>) {
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


fun TokenWidget.addDelete(node: Statement, block: BlockStmt) =
    addKeyEvent(SWT.BS, action = createDeleteEvent(node, block))

fun createDeleteEvent(node: Statement, block: BlockStmt) =
    { keyEvent: KeyEvent ->
        Commands.execute(object : Command {
            val index = block.statements.indexOf(node)
            override val target: Node = block
            override val kind = CommandKind.REMOVE
            override val element: Node = node

            override fun run() {
                block.statements.remove(node)
            }

            override fun undo() {
                // BUG statements list, after parent removal, is not the same (EXC: Widget is disposed)
                // possible solution: locate by indexing
                block.statements.add(index, node.clone())
            }
        })
    }


internal fun TokenWidget.addInsert(
    member: Control?,
    body: SequenceWidget,
    after: Boolean
) {
    fun TextWidget.addInsert(
        member: Control?,
        body: SequenceWidget,
        after: Boolean
    ) {
        addKeyEvent(SWT.CR) {
            val w = if (member == null)
                body.insertBeginning()
            else if (after)
                body.insertLineAfter(member)
            else
                body.insertLineAt(member)
            w.addInsert(w.widget, body, true)
        }
    }

    addKeyEvent(SWT.CR) {
        val w = if (member == null) {
            body.insertBeginning()
        }
        else if (after) {
            body.insertLineAfter(member)
        }
        else
            body.insertLineAt(member)
        w.addInsert(w.widget, body, true)
    }
}

