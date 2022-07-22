package javawidgets

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
    style: Int = SWT.NONE
) : NodeWidget<NodeWithModifiers<*>>(parent, style) {
    val modifiers = mutableListOf<TokenWidget>()

    lateinit var column: Composite
    lateinit var firstRow: Composite

    init {
        layout = FillLayout()
        column = column(true) {
            firstRow = row {
                node.modifiers.forEach {
                    val mod = Factory.newTokenWidget(this, it.keyword.asString())
                    mod.addDeleteListener(node, it)
                    modifiers.add(mod)
                }

                node.modifiers.register(object : AstObserverAdapter() {
                    override fun listChange(
                        observedNode: NodeList<*>,
                        type: AstObserver.ListChangeType,
                        index: Int,
                        nodeAddedOrRemoved: Node
                    ) {
                        val mod = nodeAddedOrRemoved as Modifier
                        if (type == AstObserver.ListChangeType.ADDITION) {
                            val w = Factory.newTokenWidget(firstRow, mod.keyword.asString())
                            w.addDeleteListener(node, mod)
                            // BUG  should move to same place
                            w.moveAboveInternal(if (modifiers.isEmpty()) firstRow.children[0] else modifiers.last().widget)
                        } else {
                            val index = modifiers.indexOfFirst { it.text == mod.keyword.asString() }
                            if (index != -1) {
                                modifiers[index].dispose()
                                modifiers.removeAt(index)
                                if (index < modifiers.size)
                                    modifiers[index].setFocus()
                                else
                                    firstRow.children[index]
                            }
                        }
                        requestLayout()
                    }
                })
            }
        }
    }


    private fun TokenWidget.addDeleteListener(node: NodeWithModifiers<*>, modifier: Modifier) {
        addDeleteListener {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = modifier

                override fun run() {
                    node.modifiers.remove(modifier)
                }

                override fun undo() {
                    node.modifiers.add(modifier.clone())
                }
            })
        }
    }
}