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
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

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

fun createInsert(w: SequenceWidget, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(w) { c, s ->
        c.toString().matches(Regex("[a-zA-Z0-9]|\\[|\\]|\\.")) || c == SWT.BS || c == SWT.SPACE
    }

    insert.addKeyEvent(SWT.SPACE, '(', precondition = { it.matches(Regex("if|while")) }) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = if (insert.text == "if")
            IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
        else
            WhileStmt(BooleanLiteralExpr(true), BlockStmt())

        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
        val insertIndex = w.findChildIndex(insert.widget)
        if (insert.text == "else" && insertIndex > 0) {
            val prev = w.children[insertIndex - 1]
            if (prev is IfWidget && !prev.node.hasElseBranch())
                Commands.execute(AddElseBlock((w.children[insertIndex - 1] as IfWidget).node))
        }
    }
    insert.addKeyEvent(SWT.SPACE, ';', precondition = { it == "return" }) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = if (it.character == SWT.SPACE)
            ReturnStmt(NameExpr("expression"))
        else
            ReturnStmt()

        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    // TODO array exp
    insert.addKeyEvent(
        '=',
        precondition = {
            insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ArrayAccessExpr>(it) || tryParse<FieldAccessExpr>(it))
        }) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = ExpressionStmt(AssignExpr(NameExpr(insert.text), NameExpr("exp"), AssignExpr.Operator.ASSIGN))
        Commands.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(
        '(',
        precondition = { insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it)) }) {
        val insertIndex = w.findChildIndex(insert.widget)
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

class IfWidget(
    parent: SequenceWidget,
    override val node: IfStmt,
    override val block: BlockStmt
) :
    StatementWidget<IfStmt>(parent) {

    lateinit var column: Composite
    lateinit var exp: ExpWidget
    lateinit var thenBody: SequenceWidget

    var elseWidget: ElseWidget? = null
    var elseBody: SequenceWidget? = null

    init {
        layout = RowLayout()
        column = column {
            if (node.comment.isPresent)
                CommentWidget(this, node.comment.get())

            row {
                val keyword = TokenWidget(this, "if")
                keyword.addDelete(node, block)
                FixedToken(this, "(")
                exp = ExpWidget(this, node.condition) {
                    Commands.execute(object : Command {
                        val old = node.condition
                        override fun run() {
                            node.condition = it
                        }

                        override fun undo() {
                            node.condition = old
                        }
                    })
                }
                FixedToken(this, ") {")
            }
            thenBody = createSequence(this, node.thenBlock)
            FixedToken(this, "}")
        }

        if (node.hasElseBranch())
            elseWidget = ElseWidget(column, node.elseBlock)

        node.observeProperty<Statement>(ObservableProperty.ELSE_STMT) {
            if (it == null)
                elseWidget?.dispose()
            else
                elseWidget = ElseWidget(column, it)

            requestLayout()
        }
    }

    inner class ElseWidget(parent: Composite, elseStatement: Statement) : Composite(parent, SWT.NONE){
        init {
            layout = FillLayout()
            column {
                row {
                    val keyword = TokenWidget(this, "else")
                    keyword.addKeyEvent(SWT.BS) {
                        Commands.execute(object : Command {
                            override fun run() {
                                node.removeElseStmt()
                            }

                            override fun undo() {
                                node.setElseStmt(elseStatement.clone())
                            }
                        })
                    }

                    FixedToken(this, "{")
                }
                elseBody = createSequence(this, elseStatement as BlockStmt)
                FixedToken(this, "}")
            }
        }
    }

    override fun setFocus(): Boolean = exp.setFocus()
    override fun setFocusOnCreation() {
        setFocus()
    }
}


class WhileWidget(
    parent: SequenceWidget,
    override val node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent) {

    lateinit var keyword: TokenWidget
    lateinit var exp: Id
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = TokenWidget(this, "while")
                FixedToken(this, "(")
                exp = Id(this, node.condition.toString())
                FixedToken(this, ") {")
            }
            body = createSequence(this, node.block)
            FixedToken(this, "}")
        }

        keyword.addDelete(node, block)

        node.register(object : AstObserverAdapter() {
            override fun propertyChange(
                observedNode: Node?,
                property: ObservableProperty?,
                oldValue: Any?,
                newValue: Any?
            ) {
                println(property.toString() + " " + newValue)
            }
        })
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation() {
        exp.setFocus()
    }
}

class ForEachWidget(parent: SequenceWidget, override val node: ForEachStmt, override val block: BlockStmt) :
    StatementWidget<ForEachStmt>(parent) {

    lateinit var keyword: TokenWidget
    lateinit var iterable: ExpWidget
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = TokenWidget(this, "for")
                FixedToken(this, "(")

                FixedToken(this, ":")
                iterable = ExpWidget(this, node.iterable) {

                }
                FixedToken(this, ") {")
            }
            body = createSequence(this, node.asBlockStmt())
            FixedToken(this, "}")
        }
    }

    override fun setFocusOnCreation() {
        iterable.setFocus()
    }

}

class CallWidget(parent: SequenceWidget, override val node: ExpressionStmt, override val block: BlockStmt) :
    StatementWidget<ExpressionStmt>(parent) {
    lateinit var target: Id
    lateinit var value: Id

    init {
        require(node.expression is MethodCallExpr)
        val call = node.expression as MethodCallExpr

        layout = FillLayout()
        row {
            if (call.scope.isPresent) {
                target = Id(this, call.scope.get().toString())
                target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
                FixedToken(this, ".")
            }
            value = Id(this, call.name.asString())
            FixedToken(this, "(")
            // TODO args
            FixedToken(this, ")")
            FixedToken(this, ";")
        }
    }

    override fun setFocus(): Boolean {
        return value.setFocus()
    }

    override fun setFocusOnCreation() {
        value.setFocus()
    }
}

class AssignWidget(
    parent: SequenceWidget,
    override val node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent) {
    lateinit var target: Id
    lateinit var expression: ExpWidget

    init {
        require(node.expression is AssignExpr)
        val assignment = node.expression as AssignExpr

        layout = FillLayout()
        row {
            target = Id(this, assignment.target.toString())
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
            FixedToken(this, "=")
            expression = ExpWidget(this, assignment.value) {
                Commands.execute(object : Command {
                    val old = assignment.value
                    override fun run() {
                        assignment.value = it
                    }

                    override fun undo() {
                        assignment.value = old
                    }
                })
            }
            FixedToken(this, ";")
        }

        node.observeProperty<Expression>(ObservableProperty.TARGET) {
            TODO()
        }
        node.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            TODO()
        }
        node.observeProperty<Expression>(ObservableProperty.VALUE) {
            expression.update(it!!)
        }
    }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation() {
        expression.setFocus()
    }
}

// TODO empty return
class ReturnWidget(parent: SequenceWidget, override val node: ReturnStmt, override val block: BlockStmt) :
    StatementWidget<ReturnStmt>(parent) {
    lateinit var keyword: TokenWidget
    var exp: ExpWidget? = null
    lateinit var semiColon: FixedToken

    init {
        layout = FillLayout()
        row {
            keyword = TokenWidget(this, "return")
            // BUG widget disposed
            keyword.addDelete(node, block)

            if (node.expression.isPresent)
                exp = createExpWidget(node.expression.get())
            semiColon = FixedToken(this, ";")
        }
        keyword.addKeyEvent(' ') {
            Commands.execute(object : Command {
                override fun run() {
                    node.setExpression(NameExpr("expression"))
                }

                override fun undo() {
                    node.removeExpression()
                }
            })
        }


        // BUG update exp not working visual
        node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it != null && exp == null) {
                exp = createExpWidget(it)
                exp!!.textWidget.moveAboveInternal(semiColon.label)
                requestLayout()
                exp!!.setFocus()
            }
            exp?.update(it!!)
        }
    }

    private fun Composite.createExpWidget(exp: Expression): ExpWidget {
        val w = ExpWidget(this, exp) {
            Commands.execute(object : Command {
                val old = if (node.expression.isPresent) node.expression.get() else null
                override fun run() {
                    node.setExpression(it)
                }

                override fun undo() {
                    node.setExpression(old)
                }
            })
        }
        return w
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }

    override fun setFocusOnCreation() {
        exp?.setFocus()
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

class ExpWidget(parent: Composite, var expression: Expression, editEvent: (Expression) -> Unit)
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
                        val noparse = NameExpr(NOPARSE).clone()
                        noparse.addOrphanComment(BlockComment(textWidget.text))
                        editEvent(noparse)
                    }
                }
            }
        })
    }

    fun update(e: Expression) {
        expression = e
        textWidget.widget.text = expression.toString()
    }

    fun setFocus(): Boolean = textWidget.setFocus()

}