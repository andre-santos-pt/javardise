package javawidgets

import basewidgets.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite


class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type) {
    private val keyword: TokenWidget
    private val id: Id
    private var body: SequenceWidget

    init {
        layout = FillLayout()
        keyword = Factory.newTokenWidget(firstRow, "class")
        id = Id(firstRow, type.name.id)
        id.addFocusListenerInternal(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                if (id.text.isNotEmpty())
                    Commands.execute(object : ModifyCommand<SimpleName>(type, type.name) {
                        override fun run() {
                            type.name = SimpleName(id.text)
                        }

                        override fun undo() {
                            type.name = element
                        }
                    })
                else
                    id.text = type.name.id
            }
        })
        FixedToken(firstRow, "{")

        body = SequenceWidget(column, 1) { w, e ->
            TextWidget.create(w)
        }
        type.members.forEach {
            createMember(it)
        }
        FixedToken(column, "}")

        type.observeProperty<SimpleName>(ObservableProperty.NAME) {
            id.text = it?.id ?: ""
        }

        type.members.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node
            ) {
                if (type == AstObserver.ListChangeType.ADDITION) {
                    val tail = index == body.children.size
                    val w = createMember(nodeAddedOrRemoved as BodyDeclaration<*>)
                    if (!tail)
                        w.moveAbove(body.findByModelIndex(index))
                } else {
                    body.find(nodeAddedOrRemoved)?.dispose()
                }
                body.requestLayout()
            }
        })

    }

    fun createMember(dec: BodyDeclaration<*>) =
        when (dec) {
            is FieldDeclaration -> FieldWidget(body, dec)
            is MethodDeclaration -> MethodWidget(body, dec)
            is ConstructorDeclaration -> MethodWidget(body, dec)
            else -> TODO("unsupported - $dec")
        }

    override fun setFocusOnCreation() {
        id.setFocus()
    }


    class FieldWidget(parent: Composite, val dec: FieldDeclaration) :
        MemberWidget<FieldDeclaration>(parent, dec) {

        val typeId: Id

        init {
            typeId = Id(firstRow, dec.elementType.toString())
            val name = Id(firstRow, dec.variables[0].name.asString()) // TODO multi var
            FixedToken(firstRow, ";")

            name.addKeyEvent(SWT.BS, precondition = {it.isEmpty()}) {
                Commands.execute(object : Command {
                    override val target: ClassOrInterfaceDeclaration = dec.parentNode.get() as ClassOrInterfaceDeclaration
                    override val kind: CommandKind = CommandKind.REMOVE
                    override val element: Node = dec
                    val index: Int =  target.members.indexOf(dec)
                    override fun run() {
                        dec.remove()
                    }

                    override fun undo() {
                        target.members.add(index, dec.clone())
                    }

                })
                dec.remove()
            }

            // TODO listener
        }

        override fun setFocusOnCreation() {
            typeId.setFocus()
        }

    }
}




