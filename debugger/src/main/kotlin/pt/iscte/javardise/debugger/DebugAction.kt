package pt.iscte.javardise.debugger

import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolBar
import org.eclipse.swt.widgets.ToolItem
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addNote
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.strudel.java.Strudel2Java
import pt.iscte.strudel.javaparser.translate
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine

class DebugAction : Action {
    override val name: String
        get() = "debug"

    override val iconPath: String
        get() = "debug.png"

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
            System.err.println(e.message)
            return
        }
        println(Strudel2Java().translate(module))
        val member = facade.classWidget?.getMemberOnFocus()
        member?.let {
            module.procedures.find {
                it.getProperty("JP") == member
            }?.let {
                val shell = methodShell(module, member as MethodDeclaration, it)
                shell.open()
            }
        }

    }
}

private fun methodShell(module: IModule, model: MethodDeclaration, procedure: IProcedure): Shell {
    val process = Process(module)

    val shell = Shell(Display.getDefault())
    shell.layout = FillLayout()
    shell.text = "Debug: ${model.nameAsString}(...)"

    shell.column {
        val bar = ToolBar(this, SWT.NONE)
        val startItem = ToolItem(bar, SWT.PUSH)
        startItem.image = Image(Display.getDefault(), DebugAction::class.java.classLoader.getResourceAsStream("resume.gif")) // TODO dispose

        val stepItem = ToolItem(bar, SWT.PUSH)
        stepItem.image = Image(Display.getDefault(), DebugAction::class.java.classLoader.getResourceAsStream("stepinto.gif")) // TODO dispose

        fill {
            process.vm.addListener(object : IVirtualMachine.IListener {
                override fun arrayAllocated(
                    ref: IReference,
                    dimensions: IntArray
                ) {
                    label(ref.target.toString())
                    requestLayout()

                }
            })
        }

        row {
            val methodWidget = scrollable {
                MethodWidget(it, model)
            }
            //methodWidget.enabled = false
            process.setup2(methodWidget, procedure)

            startItem.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    process.start(methodWidget)
                }
            })

            stepItem.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    process.step(methodWidget)
                }
            })
        }


    }



    // add dispose listener
    //
    shell.pack()

    return shell
}