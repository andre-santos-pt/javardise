package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.javardise.external.AddStatementCommand
import pt.iscte.javardise.external.indexOfIdentity
import kotlin.reflect.KFunction1

object Commands {
    val stack = ArrayDeque<Command>()
    val observers = mutableListOf<(Command) -> Unit>()

    fun execute(c: Command) {
        c.run()
        stack.addLast(c)
        observers.forEach {
            it(c)
        }
    }

    fun undo() {
        if (stack.isNotEmpty()) {
            val cmd = stack.removeLast()
            cmd.undo()
            observers.forEach {
                it(cmd)
            }
        }
    }

    fun reset() {
        stack.clear()
        observers.clear()
    }
}


enum class CommandKind {
    ADD, REMOVE, MODIFY
}

interface Command {
    val target: Node
    val kind: CommandKind
    val element: Any?
    fun run()
    fun undo()

    fun asString(): String = "$kind - ${target::class.simpleName} - $element"

}

fun <E: Any> Node.modifyCommand(old: E?, new: E?, setOperation: KFunction1<E?, Node>) {
    if (old != new)
        Commands.execute(object : Command {
            override val target = this@modifyCommand
            override val kind: CommandKind = CommandKind.MODIFY
            override val element: E? = old

            override fun run() {
                setOperation(new)
            }

            override fun undo() {
                setOperation(old)
            }
        })
}

fun <N: Node> NodeList<N>.addCommand(owner: Node, e: N, index: Int = size) {
    Commands.execute(object : Command {
        override val target = owner
        override val kind: CommandKind = CommandKind.ADD
        override val element = e

        override fun run() {
            add(index, e)
        }

        override fun undo() {
            removeAt(index)
        }
    })
}

fun <N: Node> NodeList<N>.removeCommand(owner: Node, e: N) {
        Commands.execute(object : Command {
            override val target = owner
            override val kind: CommandKind = CommandKind.REMOVE
            override val element = e

            var i: Int = -1
            override fun run() {
                i = indexOfIdentity(element)
                removeAt(i)
            }

            override fun undo() {
                add(i, element)
            }
        })
}




abstract class AbstractCommand<E : Node>(
    override val target: Node,
    override val kind: CommandKind,
    override val element: E
) : Command

abstract class AddCommand<E : Node>(target: Node, element: E) :
    AbstractCommand<E>(target, CommandKind.ADD, element)

abstract class ModifyCommand<E : Node>(target: Node, previous: E?) :
    AbstractCommand<E>(target, CommandKind.MODIFY, previous?.clone() as E)




object Clipboard {
    var onCopy: Pair<Node, (Node, Node, Int?) -> Unit>? = null
    // var cut: Boolean = false

    fun copy(node: Node, copy: (Node, Node, Int?) -> Unit) {
        onCopy = Pair(node, copy)
    }

//    fun cut(node: Node, copy: (Node,Node, Int?) -> Unit) {
//        copy(node, copy)
//        cut = true
//    }

    fun paste(block: BlockStmt, index: Int) {
        Commands.execute(AddStatementCommand(onCopy!!.first.clone() as Statement, block, index))
    }
}