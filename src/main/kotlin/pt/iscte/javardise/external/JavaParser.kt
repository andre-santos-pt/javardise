package pt.iscte.javardise.external

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.Observable
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.util.*

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
    node.accept(object : VoidVisitorAdapter<Any>() {


        override fun visit(n: IfStmt, arg: Any?) {
            if (n.thenStmt !is BlockStmt)
                n.thenStmt = if (n.thenStmt == null) BlockStmt() else BlockStmt(NodeList(n.thenStmt))

            if (n.hasElseBranch())
                if (n.elseStmt.get() !is BlockStmt)
                    n.setElseStmt(BlockStmt(NodeList(n.elseStmt.get())))
            super.visit(n, arg)
        }

        override fun visit(n: WhileStmt, arg: Any?) {
            if (n.body !is BlockStmt)
                n.body = if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body))
            super.visit(n, arg)
        }

        override fun visit(n: DoStmt, arg: Any?) {
            if (n.body !is BlockStmt)
                n.body = if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body))
            super.visit(n, arg)
        }

        override fun visit(n: ForStmt, arg: Any?) {
            if (n.body !is BlockStmt)
                n.body = if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body))

            if(!n.compare.isPresent)
                n.setCompare(BooleanLiteralExpr(true))

            super.visit(n, arg)
        }

        override fun visit(n: ForEachStmt, arg: Any?) {
            if (n.body !is BlockStmt)
                n.body = if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body))

            super.visit(n, arg)
        }
    }, null)
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
    Returns the single public class, or otherwise the first declared class
    (if exists)
 */
fun CompilationUnit.findMainClass(): ClassOrInterfaceDeclaration? =
    if (types.isEmpty()) null
    else types
        .filterIsInstance<ClassOrInterfaceDeclaration>()
        .find { it.isPublic }
        ?: if (types[0] is ClassOrInterfaceDeclaration) types[0] as ClassOrInterfaceDeclaration else null

interface ListObserver<T : Node> {
    fun elementAdd(list: NodeList<T>, index: Int, node: T) {}
    fun elementRemove(list: NodeList<T>, index: Int, node: T) {}
    fun elementReplace(list: NodeList<T>, index: Int, old: T, new: T) {}
}

fun <T : Node> NodeList<T>.observeList(observer: ListObserver<T>) {
    register(object : ListAddRemoveObserver<T>() {
        override fun elementAdd(list: NodeList<T>, index: Int, node: T) {
            observer.elementAdd(list, index, node)
        }

        override fun elementRemove(list: NodeList<T>, index: Int, node: T) {
            observer.elementRemove(list, index, node)
        }

        override fun elementReplace(list: NodeList<T>, index: Int, old: T,
                                    new: T) {
            observer.elementReplace(list, index, old, new)
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

    override fun listReplacement(
        observedNode: NodeList<*>,
        index: Int,
        oldNode: Node?,
        newNode: Node?
    ) {
        elementReplace(observedNode as NodeList<T>, index, oldNode as T,
            newNode as T)
    }

    abstract fun elementAdd(list: NodeList<T>, index: Int, node: T)

    abstract fun elementRemove(list: NodeList<T>, index: Int, node: T)

    abstract fun elementReplace(list: NodeList<T>, index: Int, old: T, new: T)
}

val <T> Optional<T>.getOrNull: T? get() = if(isPresent) get() else null

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

fun <T> Observable.observeNotNullProperty(prop: ObservableProperty, event: (T) -> Unit): AstObserver {
    val obs = object : AstObserverAdapter() {
        override fun propertyChange(observedNode: Node, property: ObservableProperty, oldValue: Any?, newValue: Any?) {
            if (property == prop)
                event(newValue as T)
        }
    }
    register(obs)
    return obs
}



fun <T : Node> NodeList<T>.indexOfIdentity(e: T): Int {
    for (i in 0..lastIndex)
        if (get(i) === e)
            return i
    return -1
}

inline fun <reified E : Expression> tryParse(exp: String): Boolean {
    try {
        val e = StaticJavaParser.parseExpression<E>(exp)
        return e is E
    } catch (_: ParseProblemException) {
        return false
    }
}

fun isValidType(type: String): Boolean =
    try {
        StaticJavaParser.parseType(type)
        true
    } catch (_: ParseProblemException) {
        false
    }

fun isValidClassType(type: String): Boolean =
    try {
        StaticJavaParser.parseClassOrInterfaceType(type)
        true
    } catch (_: ParseProblemException) {
        false
    }



fun isValidSimpleName(name: String): Boolean =
    try {
        StaticJavaParser.parseSimpleName(name)
        true
    } catch (_: ParseProblemException) {
        false
    }

val binaryOperators : List<BinaryExpr.Operator> = BinaryExpr.Operator.values().toList()

val unaryOperators : List<UnaryExpr.Operator> = UnaryExpr.Operator.values().toList()

val unaryOperatorsStatement : List<UnaryExpr.Operator> = listOf (
    UnaryExpr.Operator.POSTFIX_INCREMENT, UnaryExpr.Operator.POSTFIX_DECREMENT,
    UnaryExpr.Operator.PREFIX_INCREMENT, UnaryExpr.Operator.PREFIX_DECREMENT
)

val assignOperators : List<AssignExpr.Operator> = AssignExpr.Operator.values().toList()