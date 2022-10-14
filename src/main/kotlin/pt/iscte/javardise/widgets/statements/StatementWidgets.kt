package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*

abstract class StatementWidget<T : Statement>(parent: SequenceWidget, override val node: T) :
    Composite(parent, SWT.NONE), NodeWidget<T> {

    abstract val block: BlockStmt

    init {
        if (node.comment.isPresent) CommentWidget(parent, node)
    }

    fun TextWidget.setCopySource() {
        addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'c'.code)) {
                    Clipboard.copy(node) { node, dest, index ->
                        if (index != null) (dest as BlockStmt).statements.add(index, node as Statement)
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
                        if (block.parentNode.isPresent) println(block.parentNode.get())
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
    stmt: Statement, block: BlockStmt, parent: SequenceWidget
): NodeWidget<*> = when (stmt) {
    is ReturnStmt -> ReturnWidget(parent, stmt, block)
    is IfStmt -> IfWidget(parent, stmt, block)
    is WhileStmt -> WhileWidget(parent, stmt, block)
    is ExpressionStmt -> when(stmt.expression) {
        is VariableDeclarationExpr -> VariableWidget(parent, stmt, block)
        is AssignExpr -> AssignWidget(parent, stmt, block)
        else -> ExpressionStatementWidget(parent, stmt, block)
    }
    else -> throw UnsupportedOperationException("NA $stmt ${stmt::class}")
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
        c.toString().matches(Regex("\\w|\\[|]|\\.|\\s|\\+|-")) || c == SWT.BS
    }

    insert.addKeyEvent(SWT.SPACE, '(', precondition = { it.matches(Regex("if|while")) }) {
        val keyword = insert.text
        val insertIndex = seq.findIndexByModel(insert.widget)
        insert.delete()
        val stmt = if (keyword == "if") IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
        else WhileStmt(BooleanLiteralExpr(true), BlockStmt())
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
        val seqIndex = seq.children.indexOf(insert.widget)
        if (insert.text == "else" && seqIndex > 0) {
            val prev = seq.children[seqIndex - 1]
            if (prev is IfWidget && !prev.node.hasElseBranch()) {
                insert.delete()
                Commands.execute(AddElseBlock(prev.node))
            }
        }
    }
    insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val stmt = if (it.character == SWT.SPACE) ReturnStmt(NameExpr("expression"))
        else ReturnStmt()
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent('=', precondition = {
        insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ArrayAccessExpr>(it) || tryParse<FieldAccessExpr>(it))
    }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val stmt = ExpressionStmt(AssignExpr(NameExpr(insert.text), NameExpr("exp"), AssignExpr.Operator.ASSIGN))
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(';', precondition = {
        insert.isAtEnd && it.split(Regex("\\s+")).size == 2 && tryParseType(it.split(Regex("\\s+"))[0]) && tryParse<NameExpr>(
            it.split(Regex("\\s+"))[1]
        )
    }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val split = insert.text.split(Regex("\\s+"))
        val stmt = ExpressionStmt(VariableDeclarationExpr(StaticJavaParser.parseType(split[0]), split[1]))
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent('=', precondition = {
        insert.isAtEnd && it.split(Regex("\\s+")).size == 2 && tryParseType(it.split(Regex("\\s+"))[0]) && tryParse<NameExpr>(
            it.split(Regex("\\s+"))[1]
        )
    }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val split = insert.text.split(Regex("\\s+"))
        val dec = VariableDeclarator(StaticJavaParser.parseType(split[0]), split[1], NameExpr("expression"))
        val stmt = ExpressionStmt(VariableDeclarationExpr(dec))
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent('(',
        precondition = { insert.isAtEnd &&
                (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it) || tryParse<ArrayAccessExpr>(it)) }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        var e: Expression? = null
        try {
            e = StaticJavaParser.parseExpression(insert.text)
        } catch (_: ParseProblemException) {
        }

        if (e != null) {
            val stmt = if (e is NameExpr) ExpressionStmt(MethodCallExpr(null, e.name, NodeList()))
//            else if(e is ArrayAccessExpr)
//                ExpressionStmt(MethodCallExpr(e, e.name, NodeList()))
            else ExpressionStmt(
                MethodCallExpr((e as FieldAccessExpr).scope, e.nameAsString, NodeList())
            )
            insert.delete()
            Commands.execute(AddStatementCommand(stmt, block, insertIndex))
        }
    }

    insert.addKeyEvent(';',precondition = {insert.isAtEnd && tryParseExpression(it) }) {
        val insertIndex = seq.findIndexByModel(insert.widget)
        val stmt = ExpressionStmt(StaticJavaParser.parseExpression(insert.text))
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
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
            val prev = seq.findByModelIndex(index) as? Control
            val w = addWidget(node, block, seq)
            if (prev != null) (w as Composite).moveAbove(prev) // bug with comments?
            seq.requestLayout()
            w.setFocusOnCreation()
        }

        override fun elementRemove(list: NodeList<Statement>, index: Int, node: Statement) {
            val control = seq.find(node) as? Control
            val index = seq.children.indexOf(control)

            control?.dispose()
            seq.requestLayout()

            val childrenLen = seq.children.size
            if (index < childrenLen) seq.children[index].setFocus()
            else if (index - 1 in 0 until childrenLen) seq.children[index - 1].setFocus()
            else seq.parent.setFocus()
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


