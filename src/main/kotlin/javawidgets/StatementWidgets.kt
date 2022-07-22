package javawidgets

import basewidgets.*
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label

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
            if (stmt.expression is AssignExpr)
                AssignWidget(parent, stmt, block)
            else if (stmt.expression is MethodCallExpr)
                CallWidget(parent, stmt, block)
            else
                throw UnsupportedOperationException()
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

fun createInsert(w: SequenceWidget, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(w) { c, s ->
        c.toString().matches(Regex("[a-zA-Z0-9]|\\[|\\]|\\.")) || c == SWT.BS || c == SWT.SPACE
    }

    insert.addKeyEvent(SWT.SPACE, '(', precondition = { it.matches(Regex("if|while")) }) {
        val insertIndex = w.findIndexByModel(insert.widget)
        //val insertIndex = w.findChildIndex(insert.widget)
        val stmt = if (insert.text == "if")
            IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
        else
            WhileStmt(BooleanLiteralExpr(true), BlockStmt())
        insert.delete()
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
        val insertIndex = w.findIndexByModel(insert.widget)
        if (insert.text == "else" && insertIndex > 0) {
            val prev = w.children[insertIndex - 1]
            if (prev is IfWidget && !prev.node.hasElseBranch()) {
                insert.delete()
                Commands.execute(AddElseBlock((w.children[insertIndex - 1] as IfWidget).node))
            }
        }
    }
    insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
        val insertIndex = w.findIndexByModel(insert.widget)
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
            insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ArrayAccessExpr>(it) || tryParse<FieldAccessExpr>(it))
        }) {
        val insertIndex = w.findIndexByModel(insert.widget)
        val stmt = ExpressionStmt(AssignExpr(NameExpr(insert.text), NameExpr("exp"), AssignExpr.Operator.ASSIGN))
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(
        '(',
        precondition = { insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it)) }) {
        val insertIndex = w.findIndexByModel(insert.widget)
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

    insert.addFocusListenerInternal(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent) {
            insert.clear()
        }
    })
    return insert
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



fun createSequence(parent: Composite, block: BlockStmt): SequenceWidget {
    val seq = SequenceWidget(parent, 1) { w, e ->
        createInsert(w, block)
    }
    populateSequence(seq, block)
    return seq
}


class CommentWidget(parent: Composite, comment: Comment) {
    init {
        val label = Label(parent, SWT.NONE)
        label.text = "//" + comment.content
        label.foreground = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW)
    }
}

abstract class StatementWidget<T : Statement>(parent: SequenceWidget) : NodeWidget<T>(parent) {

    abstract val block: BlockStmt

    init {

    }
}


fun TokenWidget.addDelete(node: Statement, block: BlockStmt) =
    addKeyEvent(SWT.BS, action = createDeleteEvent(node, block))

fun createDeleteEvent(node: Statement, block: BlockStmt) = { keyEvent: KeyEvent ->
    Commands.execute(object : Command {
        val index = block.statements.indexOf(node)

        override fun run() {
            block.statements.remove(node)
        }

        override fun undo() {
            block.statements.add(index, node.clone())
        }
    })
}

val NOPARSE = "\$NOPARSE"

// TODO observe expression change
class ExpWidget(val parent: Composite, var expression: Expression, editEvent: (Expression) -> Unit)
//    : Composite(parent, SWT.NONE)
{

    val textWidget: TextWidget

    init {
        val noparse = expression is NameExpr && (expression as NameExpr).name.asString() == NOPARSE
        val text = if (noparse)
            if (expression.orphanComments.isNotEmpty()) expression.orphanComments[0].content else ""
        else
            expression.toString()

        textWidget = TextWidget.create(parent, text) { c, s ->
            c.toString().matches(Regex("[a-zA-Z\\d\\[\\]\\.\"'\\+\\-\\*\\\\=!\\(\\)]")) || c == SWT.BS || c == SWT.SPACE
        }
        if (noparse)
            textWidget.widget.background = Display.getDefault().getSystemColor(SWT.COLOR_RED)

        textWidget.addFocusListenerInternal(object : FocusAdapter() {
            var existingText: String? = null

            override fun focusGained(e: FocusEvent?) {
                existingText = textWidget.text
            }

            override fun focusLost(e: FocusEvent?) {
                if (textWidget.text != existingText) {
                    try {
                        expression = StaticJavaParser.parseExpression(textWidget.text)
                        textWidget.widget.background = null
                        editEvent(expression!!)
                    } catch (_: ParseProblemException) {
                        textWidget.widget.background = Display.getDefault().getSystemColor(SWT.COLOR_RED)
                        val noparse = NameExpr(NOPARSE)
                        noparse.addOrphanComment(BlockComment(textWidget.text))
                        editEvent(noparse)
                    }
                }
            }
        })
    }

    fun update(e: Expression?) {
        expression = e ?: NameExpr("expression")
        textWidget.widget.text = expression.toString()
    }

    fun setFocus(): Boolean = textWidget.setFocus()

}