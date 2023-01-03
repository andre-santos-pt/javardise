package pt.iscte.javardise.debugger

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.message
import pt.iscte.javardise.external.shell
import pt.iscte.strudel.java.Strudel2Java
import pt.iscte.strudel.javaparser.translate
import pt.iscte.strudel.model.IModule

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
            translate(listOf(facade.model!!))
        } catch (e: AssertionError) {
           message("Error", e.message.toString())
            return
        }
        println(Strudel2Java().translate(module))

        val member = facade.classWidget?.getMemberOnFocus()
        member?.let {
            val procedure = module.procedures.find {
                it.getProperty("JP") == member
            }
            procedure?.let {
                Process.setup(facade, module, it)
            }
        }

    }
}