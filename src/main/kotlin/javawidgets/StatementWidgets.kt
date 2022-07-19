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
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
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
                AssignWidget(parent, stmt.expression as AssignExpr)
            else if (stmt.expression is MethodCallExpr)
                CallWidget(parent, stmt.expression as MethodCallExpr)
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
            if(prev != null)
                (w as Composite).moveAbove(prev)
            seq.requestLayout()
            w.setFocusOnCreation()
        }

        override fun elementRemove(list: NodeList<Statement>, index: Int, node: Statement) {
            seq.find(node)?.dispose()
            seq.requestLayout()
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


abstract class StatementWidget<T : Statement>(parent: SequenceWidget) : NodeWidget<T>(parent) {

    abstract val block: BlockStmt

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
    var elseBody: SequenceWidget? = null

    init {
        layout = RowLayout()
        column = column {
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
            createElse(node.elseBlock)

        node.observeProperty<Statement>(ObservableProperty.ELSE_STMT) {
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
        elseBody = createSequence(column, elseStatement as BlockStmt)
        FixedToken(column, "}")
        requestLayout()

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

class CallWidget(parent: Composite, override val node: MethodCallExpr) :
    NodeWidget<MethodCallExpr>(parent) {
    lateinit var target: Id
    lateinit var value: Id

    init {
        layout = FillLayout()
        row {
            if (node.scope.isPresent) {
                target = Id(this, node.scope.get().toString())
                FixedToken(this, ".")
            }
            value = Id(this, node.name.asString())
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

class AssignWidget(parent: Composite, override val node: AssignExpr) :
    NodeWidget<AssignExpr>(parent) {
    lateinit var target: Id
    lateinit var expression: ExpWidget

    init {
        layout = FillLayout()
        row {
            target = Id(this, node.target.toString())
            FixedToken(this, "=")
            expression = ExpWidget(this, node.value) {
                Commands.execute(object : Command {
                    val old = node.value
                    override fun run() {
                        node.value = it
                    }

                    override fun undo() {
                        node.value = old
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