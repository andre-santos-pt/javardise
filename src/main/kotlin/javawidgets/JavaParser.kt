package javawidgets

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.Observable
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File

val IfStmt.thenBlock: BlockStmt get() = thenStmt as BlockStmt
val IfStmt.elseBlock: BlockStmt get() = elseStmt.get() as BlockStmt
val WhileStmt.block: BlockStmt get() = body as BlockStmt

fun loadModel(file: File): CompilationUnit {
    val model = StaticJavaParser.parse(file)
    substituteControlBlocks(model)
    return model
}

fun substituteControlBlocks(model: CompilationUnit) {
    model.types.forEach {
        it.accept(object : VoidVisitorAdapter<Any>() {
            override fun visit(n: WhileStmt, arg: Any?) {
                if (n.body !is BlockStmt)
                    n.body = if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body))
                super.visit(n, arg)
            }

            override fun visit(n: IfStmt, arg: Any?) {
                if (n.thenStmt !is BlockStmt)
                    n.thenStmt = if (n.thenStmt == null) BlockStmt() else BlockStmt(NodeList(n.thenStmt))

                if (n.hasElseBranch())
                    if (n.elseStmt.get() !is BlockStmt)
                        n.setElseStmt(BlockStmt(NodeList(n.elseStmt.get())))
                super.visit(n, arg)
            }

            // TODO DO, FOR, FOR EACH
        }, null)
    }
}


abstract class ListAddRemoveObserver<T : Node> : AstObserverAdapter() {
    override fun listChange(
        observedNode: NodeList<*>,
        type: AstObserver.ListChangeType,
        index: Int,
        nodeAddedOrRemoved: Node
    ) {
        if (type == AstObserver.ListChangeType.ADDITION)
            elementAdd(observedNode as NodeList<T>, index, nodeAddedOrRemoved as T)
        else
            elementRemove(observedNode as NodeList<T>, index, nodeAddedOrRemoved as T)
    }

    abstract fun elementAdd(list: NodeList<T>, index: Int, node: T)

    abstract fun elementRemove(list: NodeList<T>, index: Int, node: T)
}

abstract class PropertyObserver<T>(val prop: ObservableProperty) : AstObserverAdapter() {
    override fun propertyChange(observedNode: Node?, property: ObservableProperty, oldValue: Any?, newValue: Any?) {
        if (property == prop)
            modified(oldValue as T, newValue as T)
    }

    abstract fun modified(oldValue: T?, newValue: T?)
}

fun <T> Observable.observeProperty(prop: ObservableProperty, event: (T?) -> Unit): AstObserver {
    val obs = object : PropertyObserver<T>(prop) {
        override fun modified(oldValue: T?, newValue: T?) {
            event(newValue)
        }
    }
    register(obs)
    return obs
}


class AddStatementCommand(val stmt: Statement, val block: BlockStmt, val index: Int) : Command {
    override fun run() {
        block.addStatement(index, stmt)
    }

    override fun undo() {
        block.remove(stmt)
    }
}

class AddElseBlock(val ifStmt: IfStmt) : Command {
    override fun run() {
        ifStmt.setElseStmt(BlockStmt())
    }

    override fun undo() {
        ifStmt.setElseStmt(null)
    }
}

fun <E : Expression> tryParse(exp: String): Boolean {
    try {
        val e = StaticJavaParser.parseExpression<E>(exp)
        return e is E
    } catch (_: ParseProblemException) {
        return false
    }
}