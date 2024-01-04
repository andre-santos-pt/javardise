package pt.iscte.javardise.widgets.members

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.stmt.ExpressionStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import kotlin.math.min

abstract class MemberWidget<N : Node>(
    parent: Composite,
    final override val node: N,
    final override val configuration: Configuration,
    val validModifiers: List<List<Modifier.Keyword>> = emptyList()
) : ObserverWidget<N>(parent) {
    val member = node as NodeWithModifiers<N>
    val modifiers = mutableListOf<TokenWidget>()

    val column: Composite

    lateinit var firstRow: Composite
    abstract val type: TextWidget?
    abstract val name: TextWidget
    override val control: Control
        get() = this

    inner class ModifierList(parent: Composite) : Composite(parent, SWT.NONE) {

        init {
            layout = ROW_LAYOUT_H_SHRINK
            font = parent.font
            background = parent.background
            foreground = parent.foreground
            addModifiers(member.modifiers)

            observeListUntilDispose(
                member.modifiers,
                object : ListAnyModificationObserverPost<Modifier> {
                    override fun listModification(
                        postList: NodeList<Modifier>,
                        indexChanged: Int
                    ) {
                        refresh(postList, indexChanged)
                    }
                })
        }

        private fun addModifiers(modifiers: NodeList<Modifier>) {
            modifiers  //.filter { validModifiers.contains(it.keyword) }
                .forEach {
                    createModifierToken(this, it)
                }
        }

        private fun refresh(postList: NodeList<Modifier>, indexChanged: Int) {
            children.forEach { it.dispose() }
            addModifiers(postList)
            requestLayout()
            if (indexChanged < postList.size)
                children[indexChanged].setFocus()
            else
                traverse(SWT.TRAVERSE_TAB_NEXT)
        }


        override fun setFocus(): Boolean {
            if(children.isEmpty())
                return false
            else
                return children.first().setFocus()
        }
    }

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor
        column = column {
            firstRow = row {
                ModifierList(this)
            }
        }
    }

    protected fun configureInsert(
        insertModifier: TextWidget,
        filter: (Modifier.Keyword) -> Boolean = { true }
    ) {
        insertModifier.addKeyEvent(
            SWT.BS,
            precondition = { insertModifier.isAtBeginning }) {
            if (member.modifiers.isNotEmpty())
                member.modifiers.removeCommand(
                    node,
                    member.modifiers.last()
                )
        }
        insertModifier.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == SWT.SPACE) {
                    addMenu(insertModifier.widget,
                        validModifiers.flatten()
                            .filter { filter(it) }
                            .filter { !member.modifiers.contains(Modifier(it)) }
                            .map { it.asString() }
                            .distinct())
                    insertModifier.widget.menu.setLocation(
                        insertModifier.widget.toDisplay(
                            0,
                            20
                        )
                    )
                    insertModifier.widget.menu.isVisible = true
                }
            }
        })

    }


    private fun addMenu(widget: Control, alternatives: List<String>) {
        val menu = Menu(widget)
        for (t in alternatives) {
            val item = MenuItem(menu, SWT.NONE)
            item.text = t
            item.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    val kw = Modifier.Keyword.valueOf(item.text.uppercase())
                    val mod = Modifier(kw)
                    commandStack.execute(object : Command {
                        override val target: Node = node
                        override val kind: CommandKind = CommandKind.MODIFY
                        override val element: Any? = null

                        lateinit var toRemove: List<Modifier>

                        override fun run() {
                            toRemove = validModifiers
                                .filter { it.contains(kw) }
                                .map { it.filter { it != kw } }
                                .flatten()
                                .distinct()
                                .map { Modifier(it) }

                            toRemove.forEach {
                                member.modifiers.remove(it)
                            }
                            val i = min(
                                member.modifiers.size,
                                validModifiers.indexOfFirst { it.contains(kw) })
                            member.modifiers.add(i, mod)
                        }

                        override fun undo() {
                            member.modifiers.remove(mod)
                            member.modifiers.addAll(toRemove)
                        }
                    })
                }
            })
        }
        widget.menu = menu
    }

    private fun List<List<Modifier.Keyword>>.findCompatible(existing: List<Modifier>) =
        find { it.none { k -> existing.contains(Modifier(k)) } }?.first()

    private fun List<List<Modifier.Keyword>>.findMutuallyExclusive(existing: Modifier) =
        find { it.any { k -> Modifier(k) == existing } }
            ?.filter { Modifier(it) != existing }
            ?: emptyList()


    private fun createModifierToken(
        parent: Composite,
        modifier: Modifier
    ): TokenWidget {
        val mod = newKeywordWidget(
            parent,
            modifier.keyword.asString(),
            modifier,
            {
                validModifiers.findMutuallyExclusive(modifier)

                    .map { it.asString() }
            }
        ) { token ->
            member.modifiers.setCommand(
                member as Node,
                Modifier(Modifier.Keyword.valueOf(token.uppercase())),
                member.modifiers.indexOf(modifier)
            )
        }
        mod.addKeyEvent(SWT.BS) {
            member.modifiers.removeCommand(member as Node, modifier)
        }
        mod.widget.data = modifier
        // mod.addInsertModifier(modifier)
        return mod
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


class UnsupportedMemberWidget<T : Node>(parent: Composite, node: T, configuration: Configuration) : MemberWidget<T>(parent, node, configuration) {
    val widget: TokenWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        widget = TokenWidget(this, node.toString())
        widget.widget.font = configuration.font
        widget.widget.foreground = parent.foreground
        widget.widget.background = configuration.fillInColor
        widget.setToolTip("Unsupported")
        widget
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }

    override val type: TextWidget? = null
    override val name: TextWidget
        get() = widget

    override val control: Control
        get() = this
}


