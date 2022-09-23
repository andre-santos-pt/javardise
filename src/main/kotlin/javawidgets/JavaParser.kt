package javawidgets

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
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

fun loadCompilationUnit(file: File): CompilationUnit =
    StaticJavaParser.parse(file).apply {
        performTransformations(this)
    }

fun loadCompilationUnit(src: String): CompilationUnit =
    StaticJavaParser.parse(src).apply {
        performTransformations(this)
    }

fun loadMethod(src: String): MethodDeclaration =
    StaticJavaParser.parseMethodDeclaration(src).apply {
       substituteControlBlocks(this)
    }

private fun performTransformations(model: CompilationUnit) {
    model.types.forEach {
        substituteControlBlocks(it)
        expandFieldDeclarations(it)
    }
}

fun substituteControlBlocks(node: Node) {
    //model.types.forEach {
    node.accept(object : VoidVisitorAdapter<Any>() {
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
    //}
}

fun expandFieldDeclarations(type: TypeDeclaration<*>) {

    val changes = mutableListOf<Runnable>()
    type.accept(object : VoidVisitorAdapter<Any>() {
        override fun visit(dec: FieldDeclaration, arg: Any?) {
            dec.variables.drop(1).forEach { v ->
                changes.add {
                    val varDec = VariableDeclarator(dec.elementType.clone(), v.name.clone())
                    if (v.initializer.isPresent)
                        varDec.setInitializer(v.initializer.get().clone())
                    type.members.addAfter(FieldDeclaration(NodeList(dec.modifiers), varDec), dec)
                    dec.variables.remove(v)
                }
            }
            super.visit(dec, arg)
        }

    }, null)
    changes.forEach { it.run() }

}

/*
    Returs the single public class, or otherwise the first declared class (if exists)
 */
fun CompilationUnit.findMainClass(): ClassOrInterfaceDeclaration? =
    if (types.isEmpty()) null
    else types
        .filterIsInstance<ClassOrInterfaceDeclaration>()
        .find { it.isPublic }
        ?: if (types[0] is ClassOrInterfaceDeclaration) types[0] as ClassOrInterfaceDeclaration else null

interface ListObserver<T : Node> {
    fun elementAdd(list: NodeList<T>, index: Int, node: T)
    fun elementRemove(list: NodeList<T>, index: Int, node: T)
}

fun <T : Node> NodeList<T>.observeList(observer: ListObserver<T>) {
    register(object : ListAddRemoveObserver<T>() {
        override fun elementAdd(list: NodeList<T>, index: Int, node: T) {
            observer.elementAdd(list, index, node)
        }

        override fun elementRemove(list: NodeList<T>, index: Int, node: T) {
            observer.elementRemove(list, index, node)
        }
    })
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


class AddMemberCommand(val member: BodyDeclaration<*>, val type: ClassOrInterfaceDeclaration, val index: Int) :
    Command {
    override val kind: CommandKind = CommandKind.ADD
    override val target = type
    override val element = member

    override fun run() {
        type.members.add(index, member)
    }

    override fun undo() {
        type.members.remove(member)
    }
}

class AddStatementCommand(val stmt: Statement, val block: BlockStmt, val index: Int) : Command {
    override val kind: CommandKind = CommandKind.ADD
    override val target = block
    override val element = stmt

    override fun run() {
        block.addStatement(index, stmt)
    }

    override fun undo() {
        block.remove(stmt)
    }
}

class RemoveStatementCommand(val stmt: Statement, val block: BlockStmt) : Command {
    override val kind: CommandKind = CommandKind.REMOVE
    override val target = block
    override val element = stmt

    val index = block.statements.indexOf(stmt)

    override fun run() {
        block.statements.remove(stmt)
    }

    override fun undo() {
        block.statements.add(index, element.clone())
    }
}

class AddElseBlock(override val target: IfStmt) : Command {
    override val kind = CommandKind.ADD
    override val element: Statement = BlockStmt()

    override fun run() {
        target.setElseStmt(element)
    }

    override fun undo() {
        target.setElseStmt(null)
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

fun tryParseType(type: String): Boolean =
    try {
        StaticJavaParser.parseType(type)
        true
    } catch (_: ParseProblemException) {
        false
    }

fun tryParseExpression(exp: String): Boolean =
    try {
        StaticJavaParser.parseExpression<Expression>(exp)
        true
    } catch (_: ParseProblemException) {
        false
    }
