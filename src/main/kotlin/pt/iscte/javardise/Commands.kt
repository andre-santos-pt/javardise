package pt.iscte.javardise

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.indexOfIdentity
import java.io.File
import java.io.PrintWriter
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

interface ReplaceCommand<E> : Command {
    override val kind: CommandKind get() = CommandKind.MODIFY
    val newElement: E?
}
interface ModifyCommand<E> : ReplaceCommand<E> {
    val setOperation: KFunction1<E, Node>
}

interface CommandStack {
    val stackSize: Int
    val stackTop: Int
    val stackElements: List<Command>
    fun execute(c: Command)
    fun undo()
    fun redo()
    fun addObserver(o: (Command, Boolean) -> Unit)

    fun removeObserver(o: (Command, Boolean) -> Unit)
    fun reset()
    fun <E : Any?> modifyCommand(
        target: Node,
        old: E,
        new: E,
        setOperation: KFunction1<E, Node>
    ): Boolean

    fun <N : Node> addCommand(
        list: NodeList<in N>,
        owner: Node,
        e: N,
        index: Int = list.size
    )

    fun <N : Node> setCommand(
        list: NodeList<in N>,
        owner: Node,
        e: N,
        index: Int
    )

    fun <N : Node> replaceCommand(
        list: NodeList<in N>,
        owner: Node,
        e: N,
        newElement: N
    )

    fun <N : Node> removeCommand(list: NodeList<in N>, owner: Node, e: N)


    open class CommandStackImpl(val workingDir: File?) : CommandStack {
        override val stackSize: Int get() = stack.size
        override val stackElements: List<Command> get() = stack.toList()
        private val stack = ArrayDeque<Command>()

        private var top = -1
        private val observers = mutableListOf<(Command, Boolean) -> Unit>()

        override val stackTop: Int
            get() = top

        override fun execute(c: Command) {
            try {
                c.run()
                while (stack.lastIndex > top)
                    stack.removeLast()
                stack.addLast(c)
                top = stack.lastIndex
                observers.forEach {
                    it(c, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logError(e, c)
            }
        }

        private fun logError(e: Exception, c: Command) {
            val cu = c.target.findCompilationUnit().getOrNull
            workingDir?.let {
                for (i in 1..10) {
                    val f = File(workingDir,"ERROR$i.txt")
                    if (!f.exists()) {
                        val w = PrintWriter(f)
                        cu?.let {
                            val range = c.target.range.getOrNull
                            var src = cu.toString()
                            if(range != null) {
                                val lines = src.lines().toMutableList()
                                lines[range.begin.line-1] = ">>> " + lines[range.begin.line-1]
                                src = lines.joinToString(System.lineSeparator())
                            }
                            w.println(src)
                            w.println("----------------------------------")
                            w.println(c.asString())
                            w.println("----------------------------------")
                        }
                        e.printStackTrace(w)
                        w.close()
                        e.printStackTrace()
                        return;
                    }
                }
            }
            e.printStackTrace()
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
            if (top < stack.lastIndex) {
                val cmd = stack[++top]
                cmd.run()
            }
        }

        override fun addObserver(o: (Command, Boolean) -> Unit) {
            observers.add(o)
        }

        override fun removeObserver(o: (Command, Boolean) -> Unit) {
            observers.remove(o)
        }

        override fun reset() {
            stack.clear()
            observers.clear()
        }

        override fun <E : Any?> modifyCommand(
            target: Node,
            old: E,
            new: E,
            setOperation: KFunction1<E, Node>
        ): Boolean =
            if (old != new) {
                execute(object : ModifyCommand<E> {

                    override val target = target
                    override val kind: CommandKind = CommandKind.MODIFY
                    override val element: E? = old
                    override val newElement: E = new
                    override val setOperation: KFunction1<E, Node>
                        get() = setOperation

                    override fun run() {
                        setOperation(newElement)
                    }

                    override fun undo() {
                        if (old is Node)
                            setOperation(old as E)
                        else
                            setOperation(old)
                    }
                })
                true
            } else
                false


        override fun <N : Node> addCommand(
            list: NodeList<in N>,
            owner: Node,
            e: N,
            index: Int
        ) = execute(object : Command {
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


        override fun <N : Node> removeCommand(
            list: NodeList<in N>,
            owner: Node,
            e: N
        ) {
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

        override fun <N : Node> setCommand(
            list: NodeList<in N>,
            owner: Node,
            e: N,
            index: Int
        ) {
            execute(object : Command {
                override val target = owner
                override val kind: CommandKind = CommandKind.MODIFY
                override val element: N = e

                lateinit var prev: N

                override fun run() {
                    prev = list[index] as N
                    list[index] = e
                }

                override fun undo() {
                    list[index] = prev
                }
            })
        }

        override fun <N : Node> replaceCommand(
            list: NodeList<in N>,
            owner: Node,
            e: N,
            newElement: N
        ) {
            execute(object : ReplaceCommand<N> {
                override val target = owner
                override val element: N = e
                override val newElement: N = newElement

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


    companion object {
        fun create(workingDir: File? = null): CommandStack = CommandStackImpl(workingDir)

        val nullStack: CommandStack = object : CommandStackImpl(null) {
            override fun execute(c: Command) {
                c.run()
            }

            override fun undo() {}

            override fun redo() {}
        }
    }
}


