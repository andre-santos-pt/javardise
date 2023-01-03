package pt.iscte.javardise.debugger

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addMark
import pt.iscte.javardise.basewidgets.addTextbox
import pt.iscte.javardise.editor.Facade
import pt.iscte.javardise.external.findChild
import pt.iscte.javardise.external.message
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IStatement
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.ProcedureExecution
import pt.iscte.strudel.vm.impl.VirtualMachine

object Process {

    var current: ProcedureExecution? = null

    var ipMark: ICodeDecoration<*>? = null
    val paramBoxes: MutableList<ICodeDecoration<Text>> = mutableListOf()

    private fun updateMark(facade: Facade) {
        ipMark?.delete()
        val next = current?.ip?.element
        next?.let {

            val w = facade.classWidget?.findChild {
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

    var module: IModule? = null
    var procedure: IProcedure? = null

    fun setup(facade: Facade, module: IModule, procedure: IProcedure) {
        this.module = module
        this.procedure = procedure

        clearParams()

        procedure.parameters.forEach { p ->
            val w = facade.classWidget?.findChild {
                it is NodeWidget<*> && it.node === p.getProperty("JP")
            }
            val mark = w?.addTextbox("   ", ICodeDecoration.Location.TOP)
            mark?.show()
            paramBoxes.add(mark!!)
        }
    }

    fun start(facade: Facade) {
        check(module != null)
        check(procedure != null)

        val vm = VirtualMachine(module!!, 5, 1000, 40)
        vm.addListener(object : IVirtualMachine.IListener {
            override fun statementExecution(s: IStatement) {
                println(s.getProperty("JP"))
            }

            override fun variableAssignment(
                a: IVariableAssignment,
                value: IValue
            ) {
                println("${a.target.id} = $value")
            }
        })
        val args = paramBoxes.map { vm.getValue(it.control.text.trim()) }
        current = vm.debug(procedure!!, *args.toTypedArray())
        updateMark(facade)
    }

    fun run(facade: Facade): IValue? {
        check(module != null)
        check(procedure != null)

        val vm = VirtualMachine(module!!, 5, 1000, 40)
        val args = paramBoxes.map { vm.getValue(it.control.text.trim()) }
        return vm.execute(procedure!!, *args.toTypedArray())
    }

    fun step(facade: Facade) {
        current?.let { exec ->
            println(exec.ip)
            exec.step()
            updateMark(facade)
            if (exec.isOver())
                message("Result","${exec.returnValue}")
        }
    }

    fun stop() {
        ipMark?.delete()
       clearParams()
        current = null
        module = null
        procedure = null
    }

}