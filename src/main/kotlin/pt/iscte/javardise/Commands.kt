package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.javardise.external.indexOfIdentity
import kotlin.reflect.KFunction1

enum class CommandKind {
    ADD, REMOVE, MODIFY, MOVE
}

interface Command {
    val target: Node
    val kind: CommandKind
    val element: Any?
    fun run()
    fun undo()
    fun asString(): String = "$kind - ${target::class.simpleName} - $element"
}

interface CommandStack {
    val stackSize: Int
    val stackTop: Int
    val stackElements: List<Command>
    fun execute(c: Command)
    fun undo()
    fun redo()
    fun addObserver(o: (Command, Boolean) -> Unit)
    fun reset()
    fun <E: Any?> modifyCommand(target: Node, old: E, new: E, setOperation: KFunction1<E, Node>) : Boolean
    fun <N: Node> addCommand(list: NodeList<in N>, owner: Node, e: N, index: Int = list.size)
    fun <N: Node> removeCommand(list: NodeList<in N>, owner: Node, e: N)
    fun <N: Node> changeCommand(list: NodeList<in N>, owner: Node, e: N, index: Int)

    fun <N: Node> replaceCommand(list: NodeList<in N>, owner: Node, e: N, newElement: N) {

    }
    companion object {
        fun create(): CommandStack = CommandStackImpl()
    }

    // TODO NUllStack not well designed
   object NullStack : CommandStack {
       override val stackSize: Int = 0
        override val stackTop: Int
            get() = TODO("Not yet implemented")
        override val stackElements: List<Command> = emptyList()

       override fun execute(c: Command) { }

       override fun undo() { }

        override fun redo() {
            TODO("Not yet implemented")
        }
       override fun addObserver(o: (Command, Boolean) -> Unit) { }

       override fun reset() { }

       override fun <E> modifyCommand(
           target: Node,
           old: E,
           new: E,
           setOperation: KFunction1<E, Node>
       ): Boolean {
           return false;
       }

       override fun <N : Node> addCommand(
           list: NodeList<in N>,
           owner: Node,
           e: N,
           index: Int
       ) {
       }

       override fun <N : Node> removeCommand(
           list: NodeList<in N>,
           owner: Node,
           e: N
       ) {
       }

       override fun <N : Node> changeCommand(
           list: NodeList<in N>,
           owner: Node,
           e: N,
           index: Int
       ) {
       }

   }
}

private class CommandStackImpl : CommandStack {
    override val stackSize: Int get() = stack.size
    override val stackElements: List<Command> get() = stack.toList()
    private val stack = ArrayDeque<Command>()

    private var top = -1
    private val observers = mutableListOf<(Command, Boolean) -> Unit>()

    override val stackTop: Int
        get() = top
    override fun execute(c: Command) {
        c.run()
        while(stack.lastIndex > top)
            stack.removeLast()
        stack.addLast(c)
        top = stack.lastIndex
        observers.forEach {
            it(c, true)
        }
    }

    override fun undo() {
        if (top >= 0) {
            val cmd = stack[top]
            cmd.undo()
            top--
            observers.forEach {
                it(cmd, false)
            }
        }
    }

    override fun redo() {
        if(top < stack.lastIndex) {
            val cmd = stack[++top]
            cmd.run()
        }
    }

    override fun addObserver(o: (Command, Boolean) -> Unit) {
        observers.add(o)
    }

    override fun reset() {
        stack.clear()
        observers.clear()
    }

    override fun <E: Any?> modifyCommand(target:Node, old: E, new: E, setOperation: KFunction1<E, Node>): Boolean {
        return if (old != new) {
            execute(object : Command {
                override val target = target
                override val kind: CommandKind = CommandKind.MODIFY
                override val element: E? = old

                override fun run() {
                    setOperation(new)
                }

                override fun undo() {
                    if (old is Node)
                        setOperation(old as E)
                    else
                        setOperation(old)
                }
            })
            true
        }
        else
            false
    }


    override fun <N: Node> addCommand(list: NodeList<in N>, owner: Node, e: N, index: Int) {
        execute(object : Command {
            override val target = owner
            override val kind: CommandKind = CommandKind.ADD
            override val element = e

            override fun run() {
                list.add(index, element)
            }

            override fun undo() {
                list.removeAt(index)
            }
        })
    }

    override fun <N: Node> removeCommand(list: NodeList<in N>, owner: Node, e: N) {
        execute(object : Command {
            override val target = owner
            override val kind: CommandKind = CommandKind.REMOVE
            override val element = e

            var index: Int = list.indexOfIdentity(element)
            override fun run() {
                list.removeAt(index)
            }

            override fun undo() {
                list.add(index, element)
            }
        })
    }

    override fun <N: Node> changeCommand(list: NodeList<in N>, owner: Node, e: N, index: Int) {
        execute(object : Command {
            override val target = owner
            override val kind: CommandKind = CommandKind.MODIFY
            override val element: N = e

            override fun run() {
                list[index] = e
            }

            override fun undo() {
                list[index] = element
            }
        })
    }

    override fun <N: Node> replaceCommand(list: NodeList<in N>, owner: Node, e: N, newElement: N) {
        execute(object : Command {
            override val target = owner
            override val kind: CommandKind = CommandKind.MODIFY
            override val element: N = e

            val index = list.indexOfIdentity(element)

            override fun run() {
                list[index] = newElement
            }

            override fun undo() {
                list[index] = element
            }
        })
    }
}



