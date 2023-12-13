package pt.iscte.javardise.debugger

import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addMark
import pt.iscte.javardise.basewidgets.addTextbox
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.findChild
import pt.iscte.javardise.external.message
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.ProcedureExecution

object State {
    var process: Process? = null
}

class Process(module: IModule) {

    val vm = IVirtualMachine.create()
    var current: ProcedureExecution? = null

    var ipMark: ICodeDecoration<*>? = null
    val paramBoxes: MutableList<ICodeDecoration<Text>> = mutableListOf()

    init {
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(
                a: IVariableAssignment,
                value: IValue
            ) {
                println("${a.target.id} = $value")
            }
        })
    }

    private fun updateMark(control: Composite) {
        ipMark?.delete()
        val next = current?.instructionPointer
        next?.let {

            val w = control.findChild {
                it is NodeWidget<*> && it.node === next.getProperty("JP")
            }
            ipMark = w?.addMark(
                Display.getDefault().getSystemColor(SWT.COLOR_BLUE)
            )
            ipMark?.show()
        }
    }

    private fun clearParams() {
        paramBoxes.forEach {
            it.delete()
        }
        paramBoxes.clear()
    }

    var procedure: IProcedure? = null

    fun setup(editor: CodeEditor, procedure: IProcedure) {
        this.procedure = procedure

        clearParams()

        procedure.parameters.forEach { p ->
            val w = editor.classOnFocus?.findChild {
                it is NodeWidget<*> && it.node == p
            }
            val mark = w?.addTextbox("   ", ICodeDecoration.Location.TOP)
            mark?.show()
            paramBoxes.add(mark!!)
        }
        if(paramBoxes.isNotEmpty())
            paramBoxes.last().control.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if(e.character == SWT.CR)
                        message("Result: " + run(editor))
                }
            })
    }


    fun setup2(methodWidget: MethodWidget, procedure: IProcedure) {
        this.procedure = procedure

        clearParams()

        procedure.parameters.forEach { p ->
            methodWidget.findChild {
                it is NodeWidget<*> && it.node === p.getProperty("JP")
            }?.let {
                val mark = it.addTextbox("   ", ICodeDecoration.Location.TOP)
                mark.show()
                paramBoxes.add(mark)
            }
        }
    }

    fun start(control: Composite) {
        check(procedure != null)
        val args = paramBoxes.map { vm.getValue(it.control.text.trim()) }
        current = vm.debug(procedure!!, *args.toTypedArray())
        updateMark(control)
    }

    fun run(editor: CodeEditor): IValue? {
        check(procedure != null)
        val args = paramBoxes.map { vm.getValue(it.control.text.trim()) }
        return vm.execute(procedure!!, *args.toTypedArray())
    }


    fun step(control: Composite) {
        current?.let { exec ->
            println(exec.instructionPointer)
            exec.step()
            updateMark(control)
            if (exec.isOver()) {
                message("Result", "${exec.returnValue}")
                stop()
            }
        }
    }

    fun stop() {
        ipMark?.delete()
       //clearParams()
        current = null
        procedure = null
    }

}