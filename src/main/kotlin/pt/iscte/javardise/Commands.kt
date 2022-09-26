package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.javardise.widgets.AddStatementCommand

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
    val element: Node
    fun run()
    fun undo()

    fun asString(): String = "$kind - ${target::class.simpleName}"

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