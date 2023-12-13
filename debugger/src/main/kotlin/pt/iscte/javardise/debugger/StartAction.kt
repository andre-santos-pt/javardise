package pt.iscte.javardise.debugger

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addNote
import pt.iscte.javardise.basewidgets.addTextbox
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.external.findChild
import pt.iscte.javardise.external.message
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.*


fun ClassWidget.findJP(n: Node) =
    findChild { it is NodeWidget<*> && it.node === n}

fun ClassWidget.findJP(e: IProgramElement) =
    findChild { it is NodeWidget<*> && it.node === e.getProperty("JP")}


class StartAction : Action {
    override val name: String
        get() = "start"

    override val iconPath: String
        get() = "resume.gif"

    override val toggle: Boolean
        get() = true

    val boxes =
        mutableMapOf<MethodDeclaration, MutableList<ICodeDecoration<Text>>>()

    val decorations = mutableListOf<ICodeDecoration<*>>()


    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle && editor.classOnFocus != null) {
            val classWidget = editor.classOnFocus!!
            val m = classWidget.getMemberOnFocus()
            if (m is MethodDeclaration) {
                m.parameters.forEach { p ->
                    classWidget.findJP(p)?.let {
                        val t = it.addTextbox(
                            "",
                            ICodeDecoration.Location.TOP
                        )
                        t.control.addModifyListener {
                            (it.widget as Control).pack()
                            (it.widget as Control).requestLayout()
                        }
                        if (!boxes.containsKey(m))
                            boxes[m] = mutableListOf()
                        boxes[m]!!.add(t)
                    }
                }
                boxes.forEach { entry ->
                    entry.value.forEach { it.show() }
                    entry.value.forEach { dec ->
                        dec.control.addKeyListener(object : KeyAdapter() {
                            override fun keyPressed(e: KeyEvent) {
                                if (e.character == SWT.CR) {
                                    val module: IModule? = try {
                                        Java2Strudel().translate(listOf(editor.classOnFocus!!.node))
                                    } catch (e: AssertionError) {
                                        message("Error", e.message.toString())
                                        null
                                    }
                                    module?.let {
                                        decorations.forEach {
                                            it.delete()
                                        }
                                        decorations.clear()
                                        val vm = IVirtualMachine.create()
                                        vm.addListener(object :
                                            IVirtualMachine.IListener {
                                            override fun returnCall(
                                                s: IReturn,
                                                v: IValue?
                                            ) {
                                                classWidget.findJP(s)?.let {
                                                    if (v != null)
                                                        decorations.add(
                                                            it.addNote(
                                                                "$v",
                                                                ICodeDecoration.Location.RIGHT
                                                            )
                                                        )
                                                }
                                            }

                                            override fun executionError(e: RuntimeError) {
                                                if(e.sourceElement == null)
                                                    message("Error", e.message)
                                                else
                                                    classWidget.findJP(e.sourceElement!!)?.let {
                                                        val errNote = it.addNote(e.message,ICodeDecoration.Location.RIGHT).apply {
                                                            control.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED)
                                                        }
                                                        errNote.show()
                                                        if(e is ArrayIndexError) {
                                                            classWidget.findJP(e.indexExpression)?.let {
                                                                it.addNote(e.invalidIndex.toString(), ICodeDecoration.Location.BOTTOM).apply {
                                                                    control.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED)
                                                                }.show()
                                                            }
                                                        }
                                                    }
                                            }
                                        })
                                        val varTrack = vm.addVariableTracker()
                                        val proc = it.findProcedures { p ->
                                            p.getProperty("JP") === m
                                        }.first()
                                        vm.execute(proc,
                                            *entry.value.map {
                                                if(it.control.text.trim().matches(Regex("\\{(([0-9]+)(,[0-9]+)*)?\\}")))
                                                    vm.allocateArrayOf(INT, *it.control.text.trim().drop(1).dropLast(1).split(",").filter { it.isEmpty() }.map { it.toInt() }.toTypedArray())
                                                else
                                                    vm.getValue(it.control.text) }
                                                .toTypedArray())

                                        proc.localVariables.forEach { v ->
                                            classWidget.findJP(v)?.let {
                                                val hist = varTrack[v]
                                                val text = hist.toString()
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
                                        //message("Result", r.toString())
                                    }
                                }
                            }
                        })
                    }
                }
            }
        } else {
            boxes.forEach {
                it.value.forEach { it.delete() }
                boxes.clear()
            }
        }
    }
}