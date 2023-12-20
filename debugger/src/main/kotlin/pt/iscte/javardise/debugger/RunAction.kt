package pt.iscte.javardise.debugger

import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addNote
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.findChild
import pt.iscte.javardise.external.message
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.addVariableTracker

class RunAction : Action {
    override val name: String
        get() = "Run"

    override val iconPath: String?
        get() = "run.gif"

    override fun isEnabled(editor: CodeEditor): Boolean {
        val member = editor.classOnFocus?.getMemberOnFocus()
        return member is MethodDeclaration && member.isStatic
    }

    val decorations = mutableListOf<ICodeDecoration<*>>()

    fun List<IValue>.isStepper() =
        all { it.type == INT } && map { it.toInt() }.isStepping()

    private fun List<Int>.isStepping(): Boolean {
        for(i in 0 until lastIndex)
            if(get(i) + 1 != get(i+1))
                return false

        return true
    }


    override fun run(
        editor: CodeEditor, toogle: Boolean
    ) {
        Display.getDefault().focusControl?.let {
            it.traverse(SWT.TRAVERSE_TAB_NEXT)
        }
        val module: IModule = try {
            Java2Strudel().translate(listOf(editor.classOnFocus?.node!!))
        } catch (e: AssertionError) {
            message("Error", e.message.toString())
            return
        }

        decorations.forEach { it.delete() }
        decorations.clear()

        val member = editor.classOnFocus?.getMemberOnFocus()
        member?.let {
            val procedure = module.procedures.find {
                it.id == "main"
            }
            procedure?.let {
                val p = Process(module)
                val iterationCount = mutableMapOf<ILoop, Int>()

                p.vm.addListener(object : IVirtualMachine.IListener {
                    override fun loopIteration(loop: ILoop) {
                        if (iterationCount.containsKey(loop))
                            iterationCount[loop] = iterationCount[loop]!! + 1
                        else
                            iterationCount[loop] = 1
                    }
                })

                p.vm.addListener(object : IVirtualMachine.IListener {
                    override fun procedureEnd(procedure: IProcedure, args: List<IValue>, result: IValue?) {

                        message("!Result", "$result")
                    }
                })

                val varTrack = p.vm.addVariableTracker()

                State.process = p
                p.setup(editor, it)
                if (it.parameters.isEmpty()) {
                    val r = p.run(editor)
//                    message("Result", "$r")
                    iterationCount.forEach { e ->
                        editor.classOnFocus?.findChild {
                            it is NodeWidget<*> && it.node == e.key.getProperty(
                                "JP"
                            )
                        }?.let {
                            decorations.add(
                                it.addNote(
                                    "${e.value}x",
                                    ICodeDecoration.Location.LEFT_BOTTOM
                                )
                            )
                        }
                    }
                    it.variables.forEach { v ->
                        editor.classOnFocus?.findChild {
                            it is NodeWidget<*> && it.node == v.getProperty("JP")
                        }?.let {
                            val hist = varTrack[v]!!
                            val text = if(hist.isStepper())
                                "[${hist.first()}...${hist.last()}]"
                            else
                                hist.toString()
                            decorations.add(
                                it.addNote(
                                   text,
                                    ICodeDecoration.Location.RIGHT
                                )
                            )
                        }

                    }
                    decorations.forEach {
                        it.show()
                    }
                }

            }
        }

    }
}

