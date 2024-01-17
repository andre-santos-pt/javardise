package pt.iscte.javardise.autocorrect

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.UnsolvedSymbolException
import pt.iscte.javardise.*
import pt.iscte.javardise.external.getOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors
import kotlin.math.max

data class ScopeId(val id: String, val type: Type) {
    enum class Type {
        CLASS, METHOD, FIELD, VARIABLE
    }
}

fun Node.findClass(): ClassOrInterfaceDeclaration? =
    if (parentNode.getOrNull is ClassOrInterfaceDeclaration)
        parentNode.get() as ClassOrInterfaceDeclaration
    else if (parentNode.isPresent)
        parentNode.get().findClass()
    else
        null

fun Node.findMethod(): MethodDeclaration? =
    if (parentNode.getOrNull is MethodDeclaration)
        parentNode.get() as MethodDeclaration
    else if (parentNode.isPresent)
        parentNode.get().findMethod()
    else
        null

fun MethodDeclaration.scope(): List<ScopeId> {
    val list = mutableListOf<ScopeId>()
    (parentNode.getOrNull as? ClassOrInterfaceDeclaration)?.let { c ->
        list.addAll(c.methods.map { ScopeId(it.nameAsString, ScopeId.Type.METHOD) })
        list.addAll(c.fields.flatMap { it.variables }.map { ScopeId(it.nameAsString, ScopeId.Type.FIELD) })
    }

    parameters.mapTo(list) { ScopeId(it.nameAsString, ScopeId.Type.VARIABLE) }
    walk {
        if (it is VariableDeclarationExpr)
            list.addAll(it.variables.map { ScopeId(it.nameAsString, ScopeId.Type.VARIABLE) })
    }
    return list
}

fun findSimilarity(x: String, y: String): Double {
    val maxLength = max(x.length, y.length)
    return if (maxLength > 0) {
        (maxLength * 1.0 - getLevenshteinDistance(x.lowercase(), y.lowercase())) / maxLength * 1.0
    } else 1.0
}

fun findOption(n: NameExpr): ScopeId? = n.findMethod()?.scope()?.maxBy {
    findSimilarity(it.id, n.nameAsString)
}

class AutoCorrectHandler(val stack: CommandStack, val types: Set<String>) {
    val THRESHOLD = .6

    fun checkCommand(command: Command) {
        // field declaration added (check type)
        if (command.kind == CommandKind.ADD && command.element is FieldDeclaration) {
            val variable = (command.element as FieldDeclaration).variables[0]
            handleVarDecType(variable)
        }

        // method declaration added (check type)
        else if (command.kind == CommandKind.ADD && command.element is MethodDeclaration) {
            handleMethodDecType(command.element as MethodDeclaration)
        } else if (command is ReplaceCommand<*> && command.newElement is ExpressionStmt) {
            val stmt = (command.newElement as ExpressionStmt).expression
            when (stmt) {
                // variable declaration added (check type)
                is VariableDeclarationExpr -> {
                    val variable =
                        ((command.newElement as ExpressionStmt).expression as VariableDeclarationExpr).variables[0]
                    handleVarDecType(variable)
                }

                // assign added (check id)
                is AssignExpr -> handleAssignTarget(stmt)

                // call statement added (check id)
                is MethodCallExpr -> handledCallScopeAndId(stmt)
            }
        }
        else if (command is ModifyCommand<*>)
            when (command.newElement) {
                // variable reference was renamed
                is NameExpr -> handleVarRefRename(command as ModifyCommand<NameExpr>)

                is SimpleName -> {
                    // method reference was renamed
                    if (command.target is MethodCallExpr) {
                        val methodCall = command.target as MethodCallExpr
                        if (methodCall.scope.isPresent) {
                            try {
                                val scopeType = methodCall.scope.get().calculateResolvedType()
                                // TODO results are cached
                                val lookup = scopeType.asReferenceType().allMethods.map { it.name }
                                if (methodCall.nameAsString !in lookup) {
                                    val match = lookup.maxBy {
                                        findSimilarity(it, methodCall.nameAsString)
                                    }
                                    if (findSimilarity(match, methodCall.nameAsString) > THRESHOLD) {
                                        stack.execute(
                                            AutoCorrectIdRename(
                                                command as ModifyCommand<SimpleName>,
                                                SimpleName(match)
                                            )
                                        )
                                    }
                                }
                            } catch (e: UnsolvedSymbolException) {
                                e.printStackTrace()
                            }
                        } else {
                            val methodName = methodCall.nameAsString
                            val lookup = command.target.findClass()?.methods?.map { it.nameAsString } ?: emptyList()
                            if (methodName !in lookup) {
                                val match = lookup.maxBy {
                                    findSimilarity(it, methodName)
                                }
                                if (findSimilarity(match, methodName) > THRESHOLD) {
                                    stack.execute(
                                        AutoCorrectIdRename(
                                            command as ModifyCommand<SimpleName>,
                                            SimpleName(match)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // type was renamed (field, method, and local var declaration)
                is Type -> handleTypeRefRename(command as ModifyCommand<Type>)

                is MethodCallExpr -> TODO()

                is BinaryExpr -> TODO()

                is UnaryExpr -> TODO()
            }
    }

    private fun handleAssignTarget(assign: AssignExpr) {
        when (assign.target) {
            is NameExpr -> {
                try {
                    assign.target.calculateResolvedType()
                } catch (e: UnsolvedSymbolException) {
                    val match = findOption(assign.target as NameExpr)
                    if (match != null && findSimilarity(
                            match.id,
                            (assign.target as NameExpr).nameAsString
                        ) > THRESHOLD
                    )
                        stack.execute(AutoCorrectAssignId(assign, NameExpr(match.id)))
                }
            }
        }
    }

    private fun handledCallScopeAndId(call: MethodCallExpr) {
        if (call.scope.isPresent && call.scope.get() is NameExpr) {
            val scope = call.scope.get() as NameExpr
            try {
                scope.calculateResolvedType()
            } catch (e: UnsolvedSymbolException) {
                val match = findOption(scope)
                if (match != null && findSimilarity(
                        match.id,
                        scope.nameAsString
                    ) > THRESHOLD
                )
                    stack.execute(AutoCorrectCallScopeId(call, NameExpr(match.id)))
            }
        }

        if (call.scope.isPresent) {
            try {
                val scopeType = call.scope.get().calculateResolvedType()
                // TODO results are cached
                val lookup = scopeType.asReferenceType().allMethods.map { it.name }
                if (call.nameAsString !in lookup) {
                    val match = lookup.maxBy {
                        findSimilarity(it, call.nameAsString)
                    }
                    if (findSimilarity(match, call.nameAsString) > THRESHOLD) {
                        stack.execute(AutoCorrectCallId(call, SimpleName(match)))
                    }
                }
            } catch (e: UnsolvedSymbolException) {

            }
        } else {
            val methodName = call.nameAsString
            val lookup = call.findClass()?.methods?.map { it.nameAsString } ?: emptyList()
            if (methodName !in lookup) {
                val match = lookup.maxBy {
                    findSimilarity(it, methodName)
                }
                if (findSimilarity(match, methodName) > THRESHOLD) {
                    stack.execute(
                        AutoCorrectCallId(call, SimpleName(match))
                    )
                }
            }
        }
    }

    private fun handleVarDecType(
        variable: VariableDeclarator
    ) {
        try {
            variable.type.resolve()
        } catch (e: UnsolvedSymbolException) {
            val varName = variable.type.toString()
            val match = types.maxBy { findSimilarity(it, varName) }
            if (findSimilarity(match, varName) > THRESHOLD)
                stack.execute(
                    AutoCorrectVarType(
                        variable,
                        StaticJavaParser.parseType(match)
                    )
                )
        }
    }

    private fun handleMethodDecType(method: MethodDeclaration) {
        try {
            method.type.resolve()
        } catch (e: UnsolvedSymbolException) {
            val typeName = method.type.toString()
            val match = types.maxBy { findSimilarity(it, typeName) }
            if (findSimilarity(match, typeName) > THRESHOLD)
                stack.execute(
                    AutoCorrectMethodType(
                        method,
                        StaticJavaParser.parseType(match)
                    )
                )
        }
    }

    private fun handleTypeRefRename(command: ModifyCommand<Type>) {
        try {
            (command.newElement as Type).resolve()

        } catch (e: UnsolvedSymbolException) {
            val match = types.maxBy { findSimilarity(it, command.newElement.toString()) }
            if (findSimilarity(match, command.newElement.toString()) > THRESHOLD)
                stack.execute(AutoCorrectIdRename(command, StaticJavaParser.parseType(match)))
        }
    }

    private fun handleVarRefRename(command: ModifyCommand<NameExpr>) {
        try {
            (command.newElement as NameExpr).calculateResolvedType()
        } catch (e: UnsolvedSymbolException) {
            val match = findOption(command.newElement as NameExpr)
            if (match != null && findSimilarity(
                    match.id,
                    (command.newElement as NameExpr).nameAsString
                ) > THRESHOLD
            )
                stack.execute(AutoCorrectIdRename(command, NameExpr(match.id)))
        }
    }
}



class TypeFinder {
    fun findAllClassesUsingClassLoader(packageName: String): Set<Class<*>> {
        val stream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream(packageName.replace("[.]".toRegex(), "/"))
        val reader = BufferedReader(InputStreamReader(stream))
        return reader.lines()
            .filter { line: String -> line.endsWith(".class") }
            .map { javaClass }
            .collect(Collectors.toSet())
    }

    init {
//            val reflections = Reflections(ConfigurationBuilder().forPackage("java.lang"))
//            val lang = reflections.getAll(Scanners.SubTypes)
//            val classpath = ClassPath.from(String::class.java.classLoader); // scans the class path used by classloader
//            for (classInfo in classpath.getTopLevelClasses("java.io")) {
//                println(classInfo)
//            }

        //println(findAllClassesUsingClassLoader("java.lang"))

    }


}