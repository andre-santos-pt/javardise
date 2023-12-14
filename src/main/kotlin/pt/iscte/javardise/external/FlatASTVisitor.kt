package pt.iscte.javardise.external

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.nodeTypes.NodeWithStatements
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults
import pt.iscte.javardise.external.getOrNull

class FlatASTVisitor : VoidVisitorWithDefaults<MutableList<Node>>() {
    override fun defaultAction(n: Node, list: MutableList<Node>) {
        list.add(n)
        if (n is NodeWithAnnotations<*>)
            n.annotations.forEach { it.accept(this, list) }

        if (n is NodeWithModifiers<*>)
            n.modifiers.forEach { it.accept(this, list) }

        when (n) {
            is ClassOrInterfaceDeclaration -> {
                n.name.accept(this, list)
                n.members.forEach { it.accept(this, list) }
            }

            is MethodDeclaration -> {
                n.type.accept(this, list)
                n.name.accept(this, list)
                n.parameters.forEach { it.accept(this, list) }
                n.body.getOrNull?.accept(this, list)
            }

            is ConstructorDeclaration -> {
                n.name.accept(this, list)
                n.parameters.forEach { it.accept(this, list) }
                n.body.accept(this, list)
            }
            is WhileStmt -> {
                n.condition.accept(this, list)
                n.body.accept(this, list)
            }

            is DoStmt -> {
                n.body.accept(this, list)
                n.condition.accept(this, list)
            }

            is ForStmt -> {
                n.initialization.forEach { it.accept(this, list) }
                n.compare.getOrNull?.accept(this, list)
                n.update.forEach { it.accept(this, list) }
                n.body.accept(this, list)
            }

            is ForEachStmt -> {
                n.variable.accept(this, list)
                n.iterable.accept(this, list)
                n.body.accept(this, list)
            }

            is NodeWithStatements<*> -> {
                n.statements.forEach { it.accept(this, list) }
            }

            is IfStmt -> {
                n.condition.accept(this, list)
                n.thenStmt.accept(this, list)
                n.elseStmt.getOrNull?.accept(this, list)
            }

            is AssignExpr -> {
                n.target.accept(this, list)
                n.value.accept(this, list)
            }

            else -> n.childNodes.forEach { it.accept(this, list) }
        }
    }
}
