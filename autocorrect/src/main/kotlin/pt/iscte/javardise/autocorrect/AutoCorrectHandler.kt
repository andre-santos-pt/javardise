package pt.iscte.javardise.autocorrect

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithType
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.UnsolvedSymbolException
import pt.iscte.javardise.*


class AutoCorrectHandler(val stack: CommandStack, val types: Set<String>) {

    val autocorrect = AutoCorrectLookup()

    fun checkCommand(command: Command) {
        if (command.kind == CommandKind.ADD) {
            // method declaration added (check type)
            // parameter added (check type)
            if (command.element is NodeWithType<*, *>)
                handleType(command.element as NodeWithType<Node, Type>)

            // field declaration added (check type)
            else if (command.element is FieldDeclaration)
                handleType((command.element as FieldDeclaration).variables[0] as NodeWithType<Node, Type>)
        }

        // expression statement added
        else if (command is ReplaceCommand<*> && command.newElement is ExpressionStmt) {
            val exprStmt = (command.newElement as ExpressionStmt).expression
            when (exprStmt) {
                // variable declaration added (check type)
                is VariableDeclarationExpr -> {
                    val variable =
                        ((command.newElement as ExpressionStmt).expression as VariableDeclarationExpr).variables[0]
                    handleType(variable as NodeWithType<Node, Type>)
                }

                // assign added (check id)
                is AssignExpr -> handleAssignTarget(exprStmt)

                // call statement added (check id)
                is MethodCallExpr -> handledCallScopeAndId(exprStmt)
            }
        }

        // element modified
        else if (command is ModifyCommand<*>)
            when (command.newElement) {
                // variable reference was renamed
                is NameExpr -> handleVarRefRename(command as ModifyCommand<NameExpr>)

                is SimpleName ->
                    // method reference was renamed
                    if (command.target is MethodCallExpr) handleMethodCallRename(command)

                // type was renamed (field, method, and local var declaration)
                is Type -> handleTypeRefRename(command as ModifyCommand<Type>)

                is MethodCallExpr -> handledCallScopeAndId(command.newElement as MethodCallExpr)
            }
    }

    private fun handleMethodCallRename(command: Command) {
        val methodCall = command.target as MethodCallExpr
        if (methodCall.scope.isPresent) {
            try {
                val scopeType = methodCall.scope.get().calculateResolvedType()
                // TODO results are cached
                val lookup = scopeType.asReferenceType().allMethods.map { it.name }
                autocorrect.match(methodCall.nameAsString, lookup)?.let {
                    stack.execute(AutoCorrectIdRename(command as ModifyCommand<SimpleName>, SimpleName(it)))
                }
            } catch (_: UnsolvedSymbolException) {

            }
        } else {
            val methodName = methodCall.nameAsString
            val lookup = methodCall.findClass()?.methods?.map { it.nameAsString } ?: emptyList()
            autocorrect.match(methodName, lookup)?.let {
                stack.execute(AutoCorrectIdRename(command as ModifyCommand<SimpleName>, SimpleName(it)))
            }
        }
    }

    private fun handleAssignTarget(assign: AssignExpr) {
        when (assign.target) {
            is NameExpr -> {
                try {
                    assign.target.calculateResolvedType()
                } catch (e: UnsolvedSymbolException) {
                    autocorrect.findOption(assign.target as NameExpr)?.let {
                        stack.execute(AutoCorrectAssignId(assign, NameExpr(it.id)))
                    }
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
                autocorrect.findOption(scope)?.let {
                    stack.execute(AutoCorrectCallScopeId(call, NameExpr(it.id)))
                }
            }
        }

        if (call.scope.isPresent) {
            try {
                val scopeType = call.scope.get().calculateResolvedType()
                // TODO results are cached
                val lookup = scopeType.asReferenceType().allMethods.map { it.name }
                autocorrect.match(call.nameAsString, lookup)?.let {
                    stack.execute(AutoCorrectCallId(call, SimpleName(it)))
                }
            } catch (e: UnsolvedSymbolException) {

            }
        } else {
            val methodName = call.nameAsString
            val lookup = call.findClass()?.methods?.map { it.nameAsString } ?: emptyList()
            if (methodName !in lookup) {
                autocorrect.match(methodName, lookup)?.let {
                    stack.execute(
                        AutoCorrectCallId(call, SimpleName(it))
                    )
                }
            }
        }
    }


    private fun handleType(node: NodeWithType<Node,Type>) {
        try {
            node.type.resolve()
        } catch (e: UnsolvedSymbolException) {
            val varName = node.type.toString()
            autocorrect.match(varName, types)?.let {
                stack.execute(AutoCorrectType(node as Node, StaticJavaParser.parseType(it), node::setType))
            }
        }
    }


    private fun handleTypeRefRename(command: ModifyCommand<Type>) {
        try {
            (command.newElement as Type).resolve()
        } catch (e: UnsolvedSymbolException) {
            val typeName = command.newElement.toString()
            autocorrect.match(typeName, types)?.let {
                stack.execute(AutoCorrectIdRename(command, StaticJavaParser.parseType(it)))
            }
        }
    }

    private fun handleVarRefRename(command: ModifyCommand<NameExpr>) {
        try {
            (command.newElement as NameExpr).calculateResolvedType()
        } catch (e: UnsolvedSymbolException) {
            autocorrect.findOption(command.newElement as NameExpr)?.let {
                stack.execute(AutoCorrectIdRename(command, NameExpr(it.id)))
            }
        }
    }
}

