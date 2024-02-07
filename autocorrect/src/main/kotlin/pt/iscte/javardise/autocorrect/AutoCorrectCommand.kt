package pt.iscte.javardise.autocorrect

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithName
import com.github.javaparser.ast.nodeTypes.NodeWithType
import com.github.javaparser.ast.type.Type
import pt.iscte.javardise.ModifyCommand
import kotlin.reflect.KFunction1

sealed interface AutoCorrectCommand

class AutoCorrectIdRename<T>(private val command: ModifyCommand<T>, override val newElement: T) :
    ModifyCommand<T> by command, AutoCorrectCommand {
    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element as T)
    }
}

class AutoCorrectVarType(override val target: VariableDeclarator, override val newElement: Type) :
    ModifyCommand<Type>, AutoCorrectCommand {
    override val element: Type = target.type
    override val setOperation: KFunction1<Type, Node> = target::setType

    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}

class AutoCorrectType(override val target: Node, override val newElement: Type, override val setOperation: KFunction1<Type, Node>) :
    ModifyCommand<Type>, AutoCorrectCommand {
    override val element: Type = (target as NodeWithType<*,*>).type
   // override val setOperation: KFunction1<Type, Node> = target::setType

    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}



class AutoCorrectMethodType(override val target: MethodDeclaration, override val newElement: Type) :
    ModifyCommand<Type>, AutoCorrectCommand
{
    override val element: Type = target.type

    override val setOperation: KFunction1<Type, Node> = target::setType
    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}

class AutoCorrectCallScopeId(override val target: MethodCallExpr, override val newElement: NameExpr) :
    ModifyCommand<NameExpr>, AutoCorrectCommand
{
    override val element: NameExpr = target.scope.get() as NameExpr

    override val setOperation: KFunction1<NameExpr, Node> = target::setScope
    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}

class AutoCorrectCallId(override val target: MethodCallExpr, override val newElement: SimpleName) :
    ModifyCommand<SimpleName>, AutoCorrectCommand
{
    override val element: SimpleName = target.name

    override val setOperation: KFunction1<SimpleName, Node> = target::setName
    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}

class AutoCorrectAssignId(override val target: AssignExpr, override val newElement: NameExpr) :
    ModifyCommand<NameExpr>, AutoCorrectCommand
{
    override val element: NameExpr = target.target as NameExpr

    override val setOperation: KFunction1<NameExpr, Node> = target::setTarget
    override fun run() {
        setOperation(newElement)
    }

    override fun undo() {
        setOperation(element)
    }
}