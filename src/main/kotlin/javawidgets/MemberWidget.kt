package javawidgets

import basewidgets.SequenceWidget
import basewidgets.TextWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

abstract class MemberWidget<T : NodeWithModifiers<*>>(
    parent: Composite,
    override val node: T,
    validModifiers: List<String> = emptyList(),
    style: Int = SWT.NONE
) : NodeWidget<NodeWithModifiers<*>>(parent, style) {
    val modifiers = mutableListOf<TokenWidget>()

    lateinit var column: Composite
    lateinit var firstRow: Composite

    val filterModifiers = {
        validModifiers.filter { !node.modifiers.map { it.keyword.asString() }.contains(it) }
    }

    init {
        layout = FillLayout()
        column = column(true) {
            firstRow = row {
                node.modifiers.forEach {
                    val token = createModifierToken(this, it)
                    modifiers.add(token)
                }

                node.modifiers.register(object : AstObserverAdapter() {
                    override fun listReplacement(
                        observedNode: NodeList<*>?,
                        index: Int,
                        oldNode: Node?,
                        newNode: Node?
                    ) {
                        println("rp $newNode")
                        modifiers.find { it.text == (oldNode as Modifier).keyword.asString() }?.text =
                            (newNode as Modifier).keyword.asString()
                    }

                    override fun listChange(
                        observedNode: NodeList<*>,
                        type: AstObserver.ListChangeType,
                        index: Int,
                        nodeAddedOrRemoved: Node
                    ) {
                        val mod = nodeAddedOrRemoved as Modifier
                        if (type == AstObserver.ListChangeType.ADDITION) {
                            val token = createModifierToken(this@row, mod)

                            if (modifiers.isEmpty())
                                token.moveAboveInternal(firstRow.children[0])
                            else if (index == modifiers.size)
                                token.moveBelowInternal(modifiers.last().widget)
                            else
                                token.moveAboveInternal(modifiers[index].widget)
                            modifiers.add(token)
                            token.setFocus()
                        } else {
                            val index = modifiers.indexOfFirst { it.text == mod.keyword.asString() }
                            if (index != -1) {
                                modifiers[index].dispose()
                                modifiers.removeAt(index)
                                if (index < modifiers.size)
                                    modifiers[index].setFocus()
                                else
                                    firstRow.children[index].setFocus()
                            }
                        }
                        requestLayout()
                    }
                })
            }
        }
    }

    private fun createModifierToken(parent: Composite, it: Modifier): TokenWidget {
        val mod = Factory.newTokenWidget(parent, it.keyword.asString(), filterModifiers) { token ->
            Commands.execute(object : Command {
                override val target: Node = node as Node
                override val kind: CommandKind = CommandKind.MODIFY
                override val element = Modifier(Modifier.Keyword.valueOf(token.uppercase()))
                val index = node.modifiers.indexOf(it)
                override fun run() {
                    node.modifiers[index] = element
                }

                override fun undo() {
                    node.modifiers[index] = it
                }

            })
        }
        mod.addDeleteListener(it)
        mod.addSpaceInsert(it)
        return mod
    }


    private fun TokenWidget.addDeleteListener(modifier: Modifier) {
        addDeleteListener {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = modifier

                val index = node.modifiers.indexOf(modifier)

                override fun run() {
                    node.modifiers.remove(modifier)
                }

                override fun undo() {
                    node.modifiers.add(index, modifier.clone())
                }
            })
        }


    }

    private fun TokenWidget.addSpaceInsert(modifier: Modifier) {
        addKeyEvent(SWT.SPACE) {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.ADD
                override val element = Modifier(Modifier.Keyword.PUBLIC)

                val index = node.modifiers.indexOf(modifier)

                override fun run() {
                    node.modifiers.add(index, element)
                }

                override fun undo() {
                    node.modifiers.remove(element)
                }
            })
        }
    }


}


internal fun Composite.addInsert(
    member: MemberWidget<*>?,
    body: SequenceWidget,
    node: ClassOrInterfaceDeclaration,
    after: Boolean
): TextWidget {
    val w = TextWidget.create(this)
    w.addKeyEvent(SWT.CR) {
        val insert = if (member == null)
            body.insertBeginning()
        else if (after)
            body.insertLineAfter(member)
        else
            body.insertLineAt(member)
    }
    w.widget.background = Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA)
    return w
}

