package javawidgets

import basewidgets.TextWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

abstract class MemberWidget<T : NodeWithModifiers<*>>(
    parent: Composite,
    override val node: T,
    validModifiers: List<Modifier.Keyword> = emptyList(),
    style: Int = SWT.NONE
) : NodeWidget<NodeWithModifiers<*>>(parent, style) {
    val modifiers = mutableListOf<TokenWidget>()

    val column: Composite
    lateinit var firstRow: Composite

    private val filterModifiers = {
        validModifiers.filter { !node.modifiers.map { it.keyword  }.contains(it) }.map { it.asString() }
    }

    init {
        layout = FillLayout()
        column = column {
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

    private fun createModifierToken(parent: Composite, modifier: Modifier): TokenWidget {
        val mod = Factory.newTokenWidget(parent, modifier.keyword.asString(), filterModifiers) { token ->
            Commands.execute(object : Command {
                override val target: Node = node as Node
                override val kind: CommandKind = CommandKind.MODIFY
                override val element = Modifier(Modifier.Keyword.valueOf(token.uppercase()))
                val index = node.modifiers.indexOf(modifier)
                override fun run() {
                    node.modifiers[index] = element
                }

                override fun undo() {
                    node.modifiers[index] = modifier
                }

            })
        }
        mod.addKeyEvent(SWT.BS) {
            Commands.execute(object : Command {
                val index = parent.children.indexOf(mod.widget)

                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = node.modifiers[index]


                override fun run() {
                    node.modifiers.removeAt(index)
                }

                override fun undo() {
                    node.modifiers.add(index, modifier.clone())
                }
            })
        }
        //mod.addDeleteListener(it)
        mod.addInsertModifier(modifier)
        return mod
    }


    private fun TokenWidget.addDeleteListener(modifier: Modifier) {
        val modifierString = modifier.keyword.asString()
        addDeleteListener {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = modifier

                val index = node.modifiers.indexOfFirst { it.keyword.asString() == modifierString }

                override fun run() {
                    node.modifiers.removeAt(index)
                }

                override fun undo() {
                    node.modifiers.add(index, modifier.clone())
                }
            })
        }
    }

    internal fun TextWidget.addInsertModifier(atModifier: Modifier? = null) {
        addKeyEvent(SWT.SPACE, precondition = { this.isAtBeginning }) {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.ADD
                override val element = Modifier(Modifier.Keyword.PUBLIC)

                val index = if(atModifier == null) 0 else node.modifiers.indexOf(atModifier)

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


