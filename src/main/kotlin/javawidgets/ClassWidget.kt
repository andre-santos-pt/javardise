package javawidgets

import basewidgets.FixedToken
import basewidgets.Id
import basewidgets.SequenceWidget
import basewidgets.TextWidget
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

class ClassWidget(parent: Composite, t: TypeDeclaration<*>, val executor: CommandExecutor) : Composite(parent, SWT.NONE) {
    private lateinit var id: Id
    private lateinit var body: SequenceWidget

    init {
        layout = FillLayout()

        column {
            row {
                FixedToken(this, "class")
                id = Id(this, t.name.id)
                id.addFocusListenerInternal(object : FocusAdapter() {
                    override fun focusLost(e: FocusEvent?) {
                        if (id.text.isNotEmpty())
                            executor.execute(object : Command {
                                val existingName = t.name
                                override fun run() {
                                    t.name = SimpleName(id.text)
                                }
                                override fun undo() {
                                    t.name = existingName
                                }
                            })
                        else
                            id.text = t.name.id
                    }
                })
                FixedToken(this, "{")
            }
            body = SequenceWidget(this, 1) { w, e ->
                TextWidget.create(w)
            }
            t.members.forEach {
                createMember(it)
            }
            FixedToken(this, "}")
        }

        t.register(object : AstObserverAdapter() {
            override fun propertyChange(
                observedNode: Node,
                property: ObservableProperty,
                oldValue: Any,
                newValue: Any
            ) {
                id.text = (newValue as SimpleName).id
            }
        })

        t.members.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node
            ) {
                if (type == AstObserver.ListChangeType.ADDITION) {
                    val tail = index == body.children.size
                    val w = createMember(nodeAddedOrRemoved as BodyDeclaration<*>)
                    if(!tail)
                        w.moveAbove(body.children[index])
                } else {
                    TODO("removal")
                }
                body.requestLayout()
            }
        })

    }

    fun createMember(dec: BodyDeclaration<*>) =
        when(dec) {
            is FieldDeclaration -> FieldWidget(body, dec, executor)
            is MethodDeclaration -> MethodWidget(body, dec, executor)
            is ConstructorDeclaration -> MethodWidget(body, dec, executor)
            else -> TODO("unsupported - $dec")
        }

}