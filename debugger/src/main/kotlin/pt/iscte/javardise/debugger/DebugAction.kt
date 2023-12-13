package pt.iscte.javardise.debugger

import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolBar
import org.eclipse.swt.widgets.ToolItem
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine

class DebugAction : Action {
    override val name: String
        get() = "debug"

    override val iconPath: String
        get() = "debug.png"

    override fun isEnabled(editor: CodeEditor): Boolean {
        val member = editor.classOnFocus?.getMemberOnFocus()
        return member is MethodDeclaration && member.isStatic
    }

    override fun run(
       editor: CodeEditor,
        toggle: Boolean
    ) {
        val module: IModule = try {
            Java2Strudel().translate(listOf(editor.classOnFocus?.node!!))
        } catch (e: AssertionError) {
            System.err.println(e.message)
            return
        }
        val member = editor.classOnFocus?.getMemberOnFocus()
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
                    ref: IReference<IArray>
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