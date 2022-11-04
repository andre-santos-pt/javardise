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
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.isChildOf
import pt.iscte.javardise.external.row

abstract class MemberWidget<T : NodeWithModifiers<*>>(
    parent: Composite,
    override val node: T,
    validModifiers: List<Modifier.Keyword> = emptyList(),
    style: Int = SWT.NONE,
    override val configuration: Configuration
) : Composite(parent, style), NodeWidget<T>, ConfigurationRoot {
    val modifiers = mutableListOf<TokenWidget>()

    val column: Composite
    lateinit var firstRow: Composite

    abstract val name: TextWidget

    private val filterModifiers = {
        validModifiers.filter {
            !node.modifiers.map { it.keyword }.contains(it)
        }.map { it.asString() }
    }

    override val control: Control
        get() = this

    init {
        layout = ROW_LAYOUT_H_SHRINK
        font = configuration.font
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
                            val index =
                                modifiers.indexOfFirst { it.text == mod.keyword.asString() }
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

    private fun createModifierToken(
        parent: Composite,
        modifier: Modifier
    ): TokenWidget {
        val mod = newKeywordWidget(
            parent,
            modifier.keyword.asString(),
            filterModifiers
        ) { token ->
            Commands.execute(object : Command {
                override val target: Node = node as Node
                override val kind: CommandKind = CommandKind.MODIFY
                override val element =
                    Modifier(Modifier.Keyword.valueOf(token.uppercase()))
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
        mod.widget.data = modifier
        //mod.addDeleteListener(it)
        mod.addInsertModifier(modifier)
        return mod
    }


    private fun TokenWidget.addDeleteListener(modifier: Modifier) {
        val modifierString = modifier.keyword.asString()
        addDeleteEmptyListener {
            Commands.execute(object : Command {
                override val target = node as BodyDeclaration<*>
                override val kind = CommandKind.REMOVE
                override val element = modifier

                val index =
                    node.modifiers.indexOfFirst { it.keyword.asString() == modifierString }

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

                val index =
                    if (atModifier == null) 0 else node.modifiers.indexOf(
                        atModifier
                    )

                override fun run() {
                    node.modifiers.add(index, element)
                }

                override fun undo() {
                    node.modifiers.remove(element)
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


