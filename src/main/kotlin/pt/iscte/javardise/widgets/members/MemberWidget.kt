package pt.iscte.javardise.widgets.members

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.stmt.ExpressionStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

abstract class MemberWidget<N: Node>(
    parent: Composite,
    final override val node: N,
    validModifiers: List<Modifier.Keyword> = emptyList(),
    final override val configuration: Configuration
) : ObserverWidget<N>(parent) {
    val member = node as  NodeWithModifiers<N>
    val modifiers = mutableListOf<TokenWidget>()

    val column: Composite
    lateinit var firstRow: Composite

    abstract val type: TextWidget?

    abstract val name: TextWidget

    private val filterModifiers = {
        validModifiers.filter {
            !member.modifiers.map { it.keyword }.contains(it)
        }.map { it.asString() }
    }

    override val control: Control
        get() = this

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor
        column = column {
            firstRow = row {
                member.modifiers.filter { validModifiers.contains(it.keyword) }.forEach {
                    val token = createModifierToken(this, it)
                    modifiers.add(token)
                }

                observeListUntilDispose(member.modifiers, object : ListObserver<Modifier> {
                    override fun elementAdd(
                        list: NodeList<Modifier>,
                        index: Int,
                        modifier: Modifier
                    ) {
                        val token = createModifierToken(this@row, modifier)

                        if (modifiers.isEmpty())
                            token.moveAboveInternal(firstRow.children[0])
                        else if (index == modifiers.size)
                            token.moveBelowInternal(modifiers.last().widget)
                        else
                            token.moveAboveInternal(modifiers[index].widget)
                        modifiers.add(token)
                        token.setFocus()
                        requestLayout()
                    }

                    override fun elementRemove(
                        list: NodeList<Modifier>,
                        index: Int,
                        modifier: Modifier
                    ) {
                        val index =
                            modifiers.indexOfFirst { it.text == modifier.keyword.asString() }
                        if (index != -1) {
                            modifiers[index].dispose()
                            modifiers.removeAt(index)
                            if (index < modifiers.size)
                                modifiers[index].setFocus()
                            else
                                firstRow.children[index].setFocus()
                            requestLayout()
                        }
                    }

                    override fun elementReplace(
                        list: NodeList<Modifier>,
                        index: Int,
                        old: Modifier,
                        new: Modifier
                    ) {
                        modifiers.find { it.text == old.keyword.asString() }
                            ?.text = new.keyword.asString()
                    }
                })
            }
        }
    }

    private fun createModifierToken(
        parent: Composite,
        modifier: Modifier
    ): TokenWidget {
        val mod = newKeywordWidget(
            parent,
            modifier.keyword.asString(),
            filterModifiers
        ) { token ->
            commandStack.execute(object : Command {
                override val target: Node = node as Node
                override val kind: CommandKind = CommandKind.MODIFY
                override val element =
                    Modifier(Modifier.Keyword.valueOf(token.uppercase()))
                val index = member.modifiers.indexOf(modifier)
                override fun run() {
                    member.modifiers[index] = element
                }

                override fun undo() {
                    member.modifiers[index] = modifier
                }

            })
        }
        mod.addKeyEvent(SWT.BS) {
            commandStack.execute(object : Command {
                val index = parent.children.indexOf(mod.widget)

                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = member.modifiers[index]


                override fun run() {
                    member.modifiers.removeAt(index)
                }

                override fun undo() {
                    member.modifiers.add(index, modifier.clone())
                }
            })
        }
        mod.widget.data = modifier
        //mod.addDeleteListener(it)
        mod.addInsertModifier(modifier)
        return mod
    }


    private fun TokenWidget.addDeleteListener(modifier: Modifier) {
        val modifierString = modifier.keyword.asString()
        addDeleteEmptyListener {
            commandStack.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = modifier

                val index =
                    member.modifiers.indexOfFirst { it.keyword.asString() == modifierString }

                override fun run() {
                    member.modifiers.removeAt(index)
                }

                override fun undo() {
                    member.modifiers.add(index, modifier.clone())
                }
            })
        }
    }

    internal fun TextWidget.addInsertModifier(atModifier: Modifier? = null) {
        addKeyEvent(SWT.SPACE, precondition = { this.isAtBeginning }) {
            commandStack.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.ADD
                override val element = Modifier(Modifier.Keyword.PUBLIC)

                val index =
                    if (atModifier == null) 0 else member.modifiers.indexOf(
                        atModifier
                    )

                override fun run() {
                    member.modifiers.add(index, element)
                }

                override fun undo() {
                    member.modifiers.remove(element)
                }
            })
        }
    }

    fun getChildNodeOnFocus(): Node? {
        val onFocus = Display.getDefault().focusControl
        return if (onFocus.isChildOf(this)) {
            val w = onFocus.findAncestor<NodeWidget<*>>()
            var n = w?.node as? Node
            if (n is ExpressionStmt)
                n = n.expression
            n
        } else null
    }



    fun getChildOnFocus(): Text? {
        val onFocus = Display.getDefault().focusControl
        return if (onFocus is Text && onFocus.isChildOf(this))
            onFocus
        else null
    }
}


