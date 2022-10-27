package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.CommentWidget

abstract class StatementWidget<T : Statement>(
    parent: SequenceWidget,
    override val node: T
) :
    Composite(parent, SWT.NONE), NodeWidget<T> {

    abstract val block: BlockStmt

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = parent.font
        if (node.comment.isPresent) CommentWidget(this, node)
    }
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




fun addWidget(stmt: Statement, block: BlockStmt, parent: SequenceWidget): NodeWidget<*> {
    val statementFeature = Configuration.statementFeatures.find { it.targets(stmt) }
    if(statementFeature != null)
        return statementFeature.create(parent, stmt, block)
    else
        throw UnsupportedOperationException("NA $stmt ${stmt::class}")
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

fun createInsert(seq: SequenceWidget, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(seq) { c, s ->
        c.toString().matches(Regex("\\w|\\[|]|\\.|\\+|-|\\*|/|%"))
                || c == SWT.SPACE && !s.endsWith(SWT.SPACE)
                || c == SWT.BS
    }

//    val insert = EmptyStatement(seq, EmptyStmt(), block) { c, s ->
//                c.toString().matches(Regex("\\w|\\[|]|\\.|\\+|-|\\*|/|%"))
//                || c == SWT.SPACE && !s.endsWith(SWT.SPACE)
//                || c == SWT.BS
//    }

    fun insert(stmt: Statement) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        insert.delete()
        block.statements.addCommand(block.parentNode.get(), stmt, insertIndex)
    }

    Configuration.statementFeatures.forEach {
        it.configureInsert(insert, ::insert)
    }

    insert.addFocusLostAction {
        insert.clear()
    }

//    insert.addKeyListenerInternal(object : KeyAdapter() {
//        override fun keyPressed(e: KeyEvent) {
//            if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'v'.code)) {
//                Clipboard.paste(block, seq.findIndexByModel(insert.widget))
//            }
//        }
//    })
    return insert
}


fun createSequence(parent: Composite, block: BlockStmt): SequenceWidget {
    val seq = SequenceWidget(parent, Configuration.tabLength) { w, e ->
        createInsert(w, block)
    }
    populateSequence(seq, block)
    return seq
}

fun populateSequence(seq: SequenceWidget, block: BlockStmt) {
    block.statements.forEach {
        addWidget(it, block, seq)
    }
    block.statements.register(object : ListAddRemoveObserver<Statement>() {
        override fun elementAdd(
            list: NodeList<Statement>,
            index: Int,
            node: Statement
        ) {
            val prev = seq.findByModelIndex(index) as? Control
            val w = addWidget(node, block, seq)
            if (prev != null) (w as Composite).moveAbove(prev) // bug with comments?
            seq.requestLayout()
            w.setFocusOnCreation()
        }

        override fun elementRemove(
            list: NodeList<Statement>,
            index: Int,
            node: Statement
        ) {
            val control = seq.find(node) as? Control
            val index = seq.children.indexOf(control)

            control?.dispose()
            seq.requestLayout()

            val childrenLen = seq.children.size
            if (index < childrenLen) seq.children[index].setFocus()
            else if (index - 1 in 0 until childrenLen) seq.children[index - 1].setFocus()
            else seq.parent.setFocus()
        }

        override fun elementReplace(
            list: NodeList<Statement>,
            index: Int,
            old: Statement,
            new: Statement
        ) {
            TODO("Not yet implemented")
        }
    })
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

