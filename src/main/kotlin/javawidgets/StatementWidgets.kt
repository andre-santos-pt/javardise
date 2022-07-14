package javawidgets

import basewidgets.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

fun createWidget(stmt: Statement, parent: Composite, executor: CommandExecutor): ControlWidget<*> =
    when (stmt) {
        is ReturnStmt -> ReturnWidget(parent, stmt, executor)
        is WhileStmt -> WhileWidget(parent, stmt, executor)
        is IfStmt -> IfWidget(parent, stmt, executor)
        is ExpressionStmt ->
            if (stmt.expression is AssignExpr)
                AssignWidget(parent, stmt.expression as AssignExpr, executor)
            else
                throw UnsupportedOperationException()
        else -> throw UnsupportedOperationException("NA $stmt ${stmt::class}")
    }

fun createInsert(w: SequenceWidget, executor: CommandExecutor, block: BlockStmt): TextWidget {
    val insert = TextWidget.create(w) { c, s ->
        c.toString().matches(Regex("[a-zA-Z]")) || c == SWT.BS || c == SWT.SPACE
    }

    insert.addKeyEvent(SWT.SPACE) {
        if (!insert.text.matches(Regex("if|else|while|return")))
            return@addKeyEvent
        val insertIndex =  w.findChildIndex(insert.widget)

        if(insert.text == "else") {
            if (insertIndex > 0 && w.children[insertIndex - 1] is IfWidget)
                executor.execute(AddElseBlock((w.children[insertIndex - 1] as IfWidget).stmt))
        }
        else {
            val stmt = when (insert.text) {
                "if" -> IfStmt(BooleanLiteralExpr(true), BlockStmt(), null)
                "while" -> WhileStmt(BooleanLiteralExpr(true), BlockStmt())
                "return" -> ReturnStmt()
                else -> TODO()
            }
            executor.execute(AddStatementCommand(stmt, block, insertIndex))
        }
    }
    insert.addKeyEvent('=') {
        val insertIndex =  w.findChildIndex(insert.widget)
        executor.execute(AddStatementCommand(ExpressionStmt(AssignExpr(NameExpr(insert.text),NameExpr("exp"),AssignExpr.Operator.ASSIGN)), block, insertIndex))
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

interface ControlWidget<T> {
    val stmt: T

    fun setFocusOnCreation()
}

class IfWidget(parent: Composite, override val stmt: IfStmt, val executor: CommandExecutor) :
    Composite(parent, SWT.NONE), ControlWidget<IfStmt> {

    lateinit var exp: Id
    lateinit var thenBody: SequenceWidget
    lateinit var elseBody: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                TokenWidget(this, "if")
                FixedToken(this, "(")
                exp = Id(this, stmt.condition.toString())
                FixedToken(this, ") {")
            }
            thenBody = createSequence(this, executor, stmt.thenBlock)
            FixedToken(this, "}")

            if (stmt.hasElseBranch()) {
                row {
                    TokenWidget(this, "else")
                    FixedToken(this, "{")
                }
                elseBody = createSequence(this, executor, stmt.elseBlock)
                FixedToken(this, "}")
            }
        }
    }

    override fun setFocus(): Boolean = exp.setFocus()
    override fun setFocusOnCreation() {
        setFocus()
    }
}


class WhileWidget(parent: Composite, override val stmt: WhileStmt, val executor: CommandExecutor) :
    Composite(parent, SWT.NONE), ControlWidget<WhileStmt> {

    lateinit var keyword: TokenWidget
    lateinit var exp: Id
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = TokenWidget(this, "while")
                FixedToken(this, "(")
                exp = Id(this, stmt.condition.toString())
                FixedToken(this, ") {")
            }
            body = createSequence(this, executor, stmt.block)
            FixedToken(this, "}")
        }

        stmt.register(object : AstObserverAdapter() {
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

class AssignWidget(parent: Composite, override val stmt: AssignExpr, executor: CommandExecutor) :
    Composite(parent, SWT.NONE), ControlWidget<AssignExpr> {
    lateinit var target : Id
    lateinit var value : Id
    init {
        layout = FillLayout()
        row {
            target = Id(this, stmt.target.toString())
            FixedToken(this, "=")
            value = Id(this, stmt.value.toString())
        }
    }

    override fun setFocus(): Boolean {
        return target.setFocus()
    }

    override fun setFocusOnCreation() {
        value.setFocus()
    }
}

class ReturnWidget(parent: Composite, override val stmt: ReturnStmt, executor: CommandExecutor) :
    Composite(parent, SWT.NONE), ControlWidget<ReturnStmt> {
    lateinit var keyword: TokenWidget
    var exp: Id? = null

    init {
        layout = FillLayout()
        row {
            keyword = TokenWidget(this, "return")
            if(stmt.expression.isPresent)
                 exp = Id(this, stmt.expression.get().toString())
            FixedToken(this, ";")
        }
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }

    override fun setFocusOnCreation() {
        exp?.setFocus()
    }
}