package javawidgets

import basewidgets.*
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.comments.BlockComment
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
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

fun createWidget(stmt: Statement, parent: Composite, executor: CommandExecutor): StatementWidget<*> =
    when (stmt) {
        is ReturnStmt -> ReturnWidget(parent, stmt, executor)
        is WhileStmt -> WhileWidget(parent, stmt, executor)
        is IfStmt -> IfWidget(parent, stmt, executor)
        is ExpressionStmt ->
            if (stmt.expression is AssignExpr)
                AssignWidget(parent, stmt.expression as AssignExpr, executor)
            else if (stmt.expression is MethodCallExpr)
                CallWidget(parent, stmt.expression as MethodCallExpr, executor)
            else
                throw UnsupportedOperationException()
        else -> throw UnsupportedOperationException("NA $stmt ${stmt::class}")
    }

fun createInsert(w: SequenceWidget, executor: CommandExecutor, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(w) { c, s ->
        c.toString().matches(Regex("[a-zA-Z0-9]|\\[|\\]|\\.")) || c == SWT.BS || c == SWT.SPACE
    }

    insert.addKeyEvent(SWT.SPACE, '(', precondition = { it.matches(Regex("if|while"))}) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = if (insert.text == "if")
            IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
        else
            WhileStmt(BooleanLiteralExpr(true), BlockStmt())

        executor.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent(SWT.SPACE, '{', precondition = {it =="else"}) {
        val insertIndex = w.findChildIndex(insert.widget)
        if (insert.text == "else" && insertIndex > 0) {
            val prev = w.children[insertIndex - 1]
            if (prev is IfWidget && !prev.statement.hasElseBranch())
                executor.execute(AddElseBlock((w.children[insertIndex - 1] as IfWidget).statement))
        }
    }
    insert.addKeyEvent(SWT.SPACE, ';', precondition = {it == "return"}) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = if (it.character == SWT.SPACE)
            ReturnStmt(NameExpr("expression"))
        else
            ReturnStmt()

        executor.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    // TODO array exp
    insert.addKeyEvent('=', precondition = { insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ArrayAccessExpr>(it) || tryParse<FieldAccessExpr>(it)) }) {
        val insertIndex = w.findChildIndex(insert.widget)
        val stmt = ExpressionStmt(AssignExpr(NameExpr(insert.text), NameExpr("exp"), AssignExpr.Operator.ASSIGN))
        executor.execute(AddStatementCommand(stmt, block, insertIndex))
    }

    insert.addKeyEvent('(', precondition = { insert.isAtEnd && (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it)) }) {
        val insertIndex = w.findChildIndex(insert.widget)
        var e: Expression? = null
        try {
            e = StaticJavaParser.parseExpression(insert.text)
        } catch (_: ParseProblemException) {
        }

        if (e != null) {
            val stmt = if(e is NameExpr)
                ExpressionStmt(MethodCallExpr(null, e.name, NodeList()))
//            else if(e is ArrayAccessExpr)
//                ExpressionStmt(MethodCallExpr(e, e.name, NodeList()))
            else
                ExpressionStmt(MethodCallExpr((e as FieldAccessExpr).scope, (e as FieldAccessExpr).nameAsString, NodeList()))
            executor.execute(AddStatementCommand(stmt, block, insertIndex))
        }
    }
    return insert
}



fun populateSequence(seq: SequenceWidget, executor: CommandExecutor, block: BlockStmt) {
    block.statements.forEach {
        createWidget(it, seq, executor)
    }
    block.statements.register(object : ListAddRemoveObserver() {
        override fun elementAdd(index: Int, node: Node) {
            val w = createWidget(node as Statement, seq, executor)
            (w as Composite).moveAbove(seq.children[index])
            seq.requestLayout()
            w.setFocusOnCreation()
        }

        override fun elementRemove(index: Int, node: Node) {
            TODO("Not yet implemented")
        }
    })
}

fun createSequence(parent: Composite, executor: CommandExecutor, block: BlockStmt): SequenceWidget {
    val seq = SequenceWidget(parent, 1) { w, e ->
        createInsert(w, executor, block)
    }
    populateSequence(seq, executor, block)
    return seq
}

interface StatementWidget<T> {
    val statement: T

    fun setFocusOnCreation()
}

class IfWidget(parent: Composite, override val statement: IfStmt, val executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<IfStmt> {

    lateinit var column: Composite
    lateinit var exp: ExpWidget
    lateinit var thenBody: SequenceWidget
    var elseBody: SequenceWidget? = null

    init {
        layout = RowLayout()
        column = column {
            row {
                TokenWidget(this, "if")
                FixedToken(this, "(")
                exp = ExpWidget(this, statement.condition) {
                    executor.execute(object : Command {
                        val old = statement.condition
                        override fun run() {
                            statement.condition = it
                        }

                        override fun undo() {
                            statement.condition = old
                        }
                    })
                }
                FixedToken(this, ") {")
            }
            thenBody = createSequence(this, executor, statement.thenBlock)
            FixedToken(this, "}")
        }

        if (statement.hasElseBranch())
            createElse(statement.elseBlock)

        statement.observeProperty<Statement>(ObservableProperty.ELSE_STMT) {
            if (it == null)
                elseBody?.dispose()
            else
                createElse(it)
        }
    }

    private fun Composite.createElse(elseStatement: Statement) {
        column.row {
            TokenWidget(this, "else")
            FixedToken(this, "{")
        }
        elseBody = createSequence(column, executor, elseStatement as BlockStmt)
        FixedToken(column, "}")
        requestLayout()

    }

    override fun setFocus(): Boolean = exp.setFocus()
    override fun setFocusOnCreation() {
        setFocus()
    }
}


class WhileWidget(parent: Composite, override val statement: WhileStmt, val executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<WhileStmt> {

    lateinit var keyword: TokenWidget
    lateinit var exp: Id
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = TokenWidget(this, "while")
                FixedToken(this, "(")
                exp = Id(this, statement.condition.toString())
                FixedToken(this, ") {")
            }
            body = createSequence(this, executor, statement.block)
            FixedToken(this, "}")
        }

        statement.register(object : AstObserverAdapter() {
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

class ForEachWidget(parent: Composite, override val statement: ForEachStmt, val executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<ForEachStmt> {

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
                iterable = ExpWidget(this, statement.iterable) {

                }
                FixedToken(this, ") {")
            }
            body = createSequence(this, executor, statement.asBlockStmt())
            FixedToken(this, "}")
        }
    }

    override fun setFocusOnCreation() {
        iterable.setFocus()
    }

}

class CallWidget(parent: Composite, override val statement: MethodCallExpr, executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<MethodCallExpr> {
    lateinit var target: Id
    lateinit var value: Id

    init {
        layout = FillLayout()
        row {
            if(statement.scope.isPresent) {
                target = Id(this, statement.scope.get().toString())
                FixedToken(this, ".")
            }
            value = Id(this, statement.name.asString())
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

class AssignWidget(parent: Composite, override val statement: AssignExpr, executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<AssignExpr> {
    lateinit var target: Id
    lateinit var expression: ExpWidget

    init {
        layout = FillLayout()
        row {
            target = Id(this, statement.target.toString())
            FixedToken(this, "=")
            expression = ExpWidget(this, statement.value) {
                executor.execute(object : Command {
                    val old = statement.value
                    override fun run() {
                        statement.value = it
                    }

                    override fun undo() {
                        statement.value = old
                    }
                })
            }
            FixedToken(this, ";")
        }

        statement.observeProperty<Expression>(ObservableProperty.TARGET) {
            TODO()
        }
        statement.observeProperty<AssignExpr.Operator>(ObservableProperty.OPERATOR) {
            TODO()
        }
        statement.observeProperty<Expression>(ObservableProperty.VALUE) {
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
class ReturnWidget(parent: Composite, override val statement: ReturnStmt, executor: CommandExecutor) :
    Composite(parent, SWT.NONE), StatementWidget<ReturnStmt> {
    lateinit var keyword: TokenWidget
    var exp: ExpWidget? = null
    lateinit var semiColon: FixedToken

    init {
        layout = FillLayout()
        row {
            keyword = TokenWidget(this, "return")
            if (statement.expression.isPresent)
                exp = createExpWidget(statement.expression.get(), executor)
            semiColon = FixedToken(this, ";")
        }
        keyword.addKeyEvent(' ') {
            executor.execute(object : Command {
                override fun run() {
                    statement.setExpression(NameExpr("expression"))
                }

                override fun undo() {
                    statement.removeExpression()
                }
            })
        }
        statement.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it != null && exp == null) {
                exp = createExpWidget(it, executor)
                exp!!.textWidget.moveAboveInternal(semiColon.label)
                requestLayout()
                exp!!.setFocus()
            }

            exp?.update(it!!)
        }
    }

    private fun Composite.createExpWidget(exp: Expression, executor: CommandExecutor): ExpWidget {
        val w = ExpWidget(this, exp) {
            executor.execute(object : Command {
                val old = if (statement.expression.isPresent) statement.expression.get() else null
                override fun run() {
                    statement.setExpression(it)
                }

                override fun undo() {
                    statement.setExpression(old)
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
            c.toString().matches(Regex("[a-zA-Z\\d\\[\\]\\.\"'\\+\\-\\*\\\\=!]")) || c == SWT.BS || c == SWT.SPACE
        }
        if (noparse)
            textWidget.widget.background = Display.getDefault().getSystemColor(SWT.COLOR_RED)

        textWidget.addFocusListenerInternal(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
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
        })
    }

    fun update(e: Expression) {
        expression = e
        textWidget.widget.text = expression.toString()
    }

    fun setFocus(): Boolean = textWidget.setFocus()

}