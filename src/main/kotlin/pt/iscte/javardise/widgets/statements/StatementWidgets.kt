package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.widgets.statements.*
abstract class StatementWidget<T : Statement>(parent: SequenceWidget, override val node: T) : NodeWidget<T>(parent) {

    abstract val block: BlockStmt

    init {
        if (node.comment.isPresent)
            CommentWidget(parent, node)
    }

    fun TextWidget.setCopySource() {
        addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'c'.code)) {
                    Clipboard.copy(node) { node, dest, index ->
                        if (index != null)
                            (dest as BlockStmt).statements.add(index, node as Statement)
                    }
                }
//                else if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'x'.code)) {
//                    Clipboard.cut(node) { node, dest, index ->
//                        if (index != null)
//                            (dest as BlockStmt).statements.add(index, node as Statement)
//                    }
//                }
            }
        })
    }

    fun TextWidget.setMoveSource() {
        addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if ((e.stateMask == SWT.ALT) && (e.keyCode == SWT.ARROW_UP)) {
                    val index = block.statements.indexOf(node)
                    if (index == 0) {
                        if (block.parentNode.isPresent)
                            println(block.parentNode.get())
                    } else {
                        block.statements.remove(node)
                        block.statements.add(index - 1, node)
                    }
                    e.doit = false
                } else if ((e.stateMask == SWT.ALT) && (e.keyCode == SWT.ARROW_DOWN)) {
                    val index = block.statements.indexOf(node)
                    if (index == block.statements.lastIndex) {

                        println("last")
                    } else {
                        block.statements.remove(node)
                        block.statements.add(index + 1, node)
                    }
                    e.doit = false
                }
            }
        })
    }
}


fun addWidget(
    stmt: Statement,
    block: BlockStmt,
    parent: SequenceWidget
): NodeWidget<*> =
    when (stmt) {
        is ReturnStmt -> ReturnWidget(parent, stmt, block)
        is IfStmt -> IfWidget(parent, stmt, block)
        is WhileStmt -> WhileWidget(parent, stmt, block)
        is ExpressionStmt ->
            if (stmt.expression is VariableDeclarationExpr)
                VariableWidget(parent, stmt, block)
            else if (stmt.expression is AssignExpr)
                AssignWidget(parent, stmt, block)
            else if (stmt.expression is MethodCallExpr)
                CallWidget(parent, stmt, block)
            else
                throw UnsupportedOperationException("NA $stmt ${stmt::class}")
        else -> throw UnsupportedOperationException("NA $stmt ${stmt::class}")
    }

fun SequenceWidget.findIndexByModel(control: Control): Int {
    var i = 0
    for (c in children) {
        if (c === control)
            return i
        if (c is NodeWidget<*>)
            i++

    }
    check(false)
    return -1
}

fun createInsert(seq: SequenceWidget, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(seq) { c, s ->
        c.toString().matches(Regex("[a-zA-Z0-9]|\\[|\\]|\\.")) || c == SWT.BS
    }

    insert.addKeyEvent(SWT.SPACE, '(', precondition = { it.matches(Regex("if|while")) }) {
        val keyword = insert.text
        val insertIndex = seq.findIndexByModel(insert.widget)
        insert.delete()
        val stmt = if (keyword == "if")
            IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
        else
            WhileStmt(BooleanLiteralExpr(true), BlockStmt())
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        if (insert.text == "else" && insertIndex > 0) {
            val prev = seq.children[insertIndex - 1]
            if (prev is IfWidget && !prev.node.hasElseBranch()) {
                insert.delete()
                Commands.execute(AddElseBlock((seq.children[insertIndex - 1] as IfWidget).node))
            }
        }
    }
    insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val stmt = if (it.character == SWT.SPACE)
            ReturnStmt(NameExpr("expression"))
        else
            ReturnStmt()
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    // TODO array exp
    insert.addKeyEvent(
        '=',
        precondition = {
            insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ArrayAccessExpr>(it) || tryParse<FieldAccessExpr>(
                it
            ))
        }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val stmt = ExpressionStmt(AssignExpr(NameExpr(insert.text), NameExpr("exp"), AssignExpr.Operator.ASSIGN))
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(
        '(',
        precondition = { insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it)) }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        var e: Expression? = null
        try {
            e = StaticJavaParser.parseExpression(insert.text)
        } catch (_: ParseProblemException) {
        }

        if (e != null) {
            val stmt = if (e is NameExpr)
                ExpressionStmt(MethodCallExpr(null, e.name, NodeList()))
//            else if(e is ArrayAccessExpr)
//                ExpressionStmt(MethodCallExpr(e, e.name, NodeList()))
            else
                ExpressionStmt(
                    MethodCallExpr(
                        (e as FieldAccessExpr).scope,
                        (e as FieldAccessExpr).nameAsString,
                        NodeList()
                    )
                )
            Commands.execute(AddStatementCommand(stmt, block, insertIndex))
        }
    }

    insert.addFocusLostAction {
        insert.clear()
    }

    insert.addKeyListenerInternal(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'v'.code)) {
                Clipboard.paste(block, seq.findIndexByModel(insert.widget))
            }
        }
    })
    return insert
}


fun createSequence(parent: Composite, block: BlockStmt): SequenceWidget {
    val seq = SequenceWidget(parent, 1) { w, e ->
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
        override fun elementAdd(list: NodeList<Statement>, index: Int, node: Statement) {
            val prev = seq.findByModelIndex(index)
            val w = addWidget(node, block, seq)
            if (prev != null)
                (w as Composite).moveAbove(prev)
            seq.requestLayout()
            w.setFocusOnCreation()
        }

        override fun elementRemove(list: NodeList<Statement>, index: Int, node: Statement) {
            val next = seq.findByModelIndex(index + 1)
            seq.find(node)?.dispose()
            seq.requestLayout()
            next?.setFocus()
        }
    })
}

fun <T : Node> SequenceWidget.find(predicate: (T) -> Boolean): NodeWidget<T>? =
    children.find { predicate((it as NodeWidget<T>).node) } as NodeWidget<T>

fun <T : Node> SequenceWidget.find(e: T): NodeWidget<T>? =
    children.find { it is NodeWidget<*> && it.node === e } as NodeWidget<T>?

fun SequenceWidget.findByModelIndex(index: Int): NodeWidget<*>? {
    var i = 0
    for (c in children)
        if (c is NodeWidget<*>)
            if (i == index)
                return c
            else
                i++
    return null
}


fun TokenWidget.addDelete(node: Statement, block: BlockStmt) =
    addKeyEvent(SWT.BS, action = createDeleteEvent(node, block))

fun createDeleteEvent(node: Statement, block: BlockStmt) = { keyEvent: KeyEvent ->
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

