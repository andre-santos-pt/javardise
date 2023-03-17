package pt.iscte.javardise.debugger

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addNote
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.findChild
import pt.iscte.javardise.external.message
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.vm.IVirtualMachine

class RunAction : Action {
    override val name: String
        get() = "Run"

    override val iconPath: String?
        get() = "run.gif"

    override fun isEnabled(facade: Facade): Boolean {
        val member = facade.classWidget?.getMemberOnFocus()
        return member is MethodDeclaration && member.isStatic
    }

    override fun run(
        facade: Facade,
        toggle: Boolean
    ) {
        check(isEnabled(facade))
        val module: IModule = try {
            Java2Strudel().translate(listOf(facade.model!!))
        } catch (e: AssertionError) {
           message("Error", e.message.toString())
            return
        }

        val member = facade.classWidget?.getMemberOnFocus()
        member?.let {
            val procedure = module.procedures.find {
                it.getProperty("JP") == member
            }
            procedure?.let {
                val p = Process(module)
                val iterationCount = mutableMapOf<ILoop, Int>()

                p.vm.addListener(object : IVirtualMachine.IListener {
                    override fun loopIteration(loop: ILoop) {
                        if(iterationCount.containsKey(loop))
                            iterationCount[loop] = iterationCount[loop]!!+1
                        else
                            iterationCount[loop] = 1
                    }
                })

                State.process = p
                p.setup(facade, it)
                if(it.parameters.isEmpty()) {
                    val r = p.run(facade)
                    message("Result", "$r")
                    iterationCount.forEach { e ->
                        facade.classWidget?.findChild {
                            it is NodeWidget<*> && it.node == e.key.getProperty("JP")
                        }?.let {
                            it.addNote(
                                "${e.value} x",
                                ICodeDecoration.Location.LEFT
                            ).show()
                        }
                    }
                }

            }
        }

    }
}