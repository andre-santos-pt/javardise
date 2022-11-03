package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import pt.iscte.javardise.external.indexOfIdentity
import kotlin.reflect.KFunction1

object Commands {
    val stackSize: Int get() = stack.size
    val stackElements: List<Command> get() = stack.toList()

    private val stack = ArrayDeque<Command>()
    private val observers = mutableListOf<(Command) -> Unit>()

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

    fun addObserver(o: (Command) -> Unit) {
        observers.add(o)
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
                if(old is Node)
                    setOperation(old.clone() as E)
                else
                    setOperation(old)
            }
        })
}

fun <N: Node> NodeList<in N>.addCommand(owner: Node, e: N, index: Int = size) {
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

fun <N: Node> NodeList<in N>.changeCommand(owner: Node, e: N, index: Int = size) {
    Commands.execute(object : Command {
        override val target = owner
        override val kind: CommandKind = CommandKind.MODIFY
        override val element: Node = e

        override fun run() {
            set(index, e)
        }

        override fun undo() {
            set(index, element.clone() as N)
        }
    })
}

fun <N: Node> NodeList<in N>.removeCommand(owner: Node, e: N) {
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
                add(i, element.clone() as N)
            }
        })
}
