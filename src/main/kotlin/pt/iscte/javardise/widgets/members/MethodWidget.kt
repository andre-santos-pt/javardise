package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.SequenceContainer
import pt.iscte.javardise.widgets.statements.StatementWidget
import pt.iscte.javardise.widgets.statements.addEmptyStatement

class MethodWidget(
    parent: Composite,
    val dec: CallableDeclaration<*>,
    configuration: Configuration = DefaultConfigurationSingleton,
    validModifiers: List<List<Modifier.Keyword>> = configuration.methodModifiers,
    override val commandStack: CommandStack = CommandStack.create(),
    freezeSignature: Boolean = false
) :
    MemberWidget<CallableDeclaration<*>>(
        parent, dec, configuration, validModifiers
    ),
    SequenceContainer<CallableDeclaration<*>>,
    ConfigurationRoot {

    override var type: Id? = null
    override val name: Id

    override var bodyWidget: SequenceWidget? = null

    override val body: BlockStmt? =
        if (dec is MethodDeclaration) if (dec.body.isPresent) dec.body.get() else null
        else (dec as ConstructorDeclaration).body

    val paramsWidget: ParamListWidget

    override val closingBracket: TokenWidget

    private val observers = mutableListOf<(Node?, Any?) -> Unit>()

    fun addObserver(action: (Node?, Any?) -> Unit) {
        observers.add(action)
    }

    val focusListener = { event: Event ->
            if ((event.widget as Control).isChildOf(this@MethodWidget)) {
                val w = (event.widget as Control).findAncestor<NodeWidget<*>>()
                val s = (w as Control).findAncestor<StatementWidget<*>>()
                observers.forEach {
                    val n = w?.node as? Node
                    it(n, s?.node)
                }
            }
    }

    init {
        layout = RowLayout()
        (layout as RowLayout).marginTop = 10

        if (node.isMethodDeclaration) {
            type = SimpleTypeWidget(
                firstRow,
                (node as MethodDeclaration).type
            )

            type!!.addFocusLostAction(::isValidType) {
                node.modifyCommand(node.typeAsString, it, node::setType)
            }
            configureInsert(type!!)
        }

        name = SimpleNameWidget(firstRow, node)

        name.setCopySource(node)
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(node.nameAsString, it, node::setName)
        }

        observeProperty<Type>(ObservableProperty.TYPE) {
            type?.set(it?.asString())
        }

        observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it?.asString())
        }


        if (node.isConstructorDeclaration) {
            name.setReadOnly()
            name.setToolTip("Constructor name is not editable. Renaming the class modifies constructors accordingly.")
        }
        FixedToken(firstRow, "(")
        paramsWidget = ParamListWidget(firstRow, node.parameters)
        FixedToken(firstRow, ")")

        if (body != null) {
            bodyWidget = createBlockSequence(column, body)
            val openBracket = TokenWidget(firstRow, "{")
            openBracket.addEmptyStatement(this, body)
            closingBracket = TokenWidget(column, "}")
        } else
            closingBracket = TokenWidget(firstRow, ";")

        observeProperty<BlockStmt>(ObservableProperty.BODY) {
            bodyWidget?.dispose()
            if(it != null) {
                bodyWidget = createBlockSequence(column, it)
                bodyWidget?.moveAbove(closingBracket)
                this@MethodWidget.requestLayout()
            }
        }

        if(freezeSignature)
            firstRow.enabled = false

        Display.getDefault().addFilter(SWT.FocusIn, focusListener)

        addDisposeListener {
            Display.getDefault().removeFilter(SWT.FocusIn, focusListener)
        }
    }

    inner class ParamListWidget(
        parent: Composite,
        val parameters: NodeList<Parameter>
    ) :
        Composite(parent, SWT.NONE) {
        var insert: TextWidget? = null

        init {
            layout = ROW_LAYOUT_H_SHRINK
            font = parent.font
            background = parent.background
            foreground = parent.foreground
            if (parameters.isEmpty())
                createInsert()

            addParams()

            observeListUntilDispose(parameters, object : ListObserver<Parameter> {
                override fun elementAdd(
                    list: NodeList<Parameter>,
                    index: Int,
                    node: Parameter
                ) {
                    val p = ParamWidget(this@ParamListWidget, index, node)
                    if (index == 0 && list.isEmpty()) {
                        //ParamWidget(this@ParamListWidget, index, node)
                    } else if (index == list.size) {
                        val c = FixedToken(this@ParamListWidget, ",")
                        c.moveAbove(p)
                    } else {
                        val n =
                            children.find { it is ParamWidget && it.node == list[index] }
                        n?.let {
                            p.moveAbove(n)
                            val c = FixedToken(this@ParamListWidget, ",")
                            c.moveAbove(n)
                        }
                    }
                    insert?.delete()
                    p.setFocusOnCreation(list.isEmpty())
                    p.requestLayout()
                }

                override fun elementRemove(
                    list: NodeList<Parameter>,
                    index: Int,
                    node: Parameter
                ) {
                    val i =
                        children.indexOfFirst { it is ParamWidget && it.node == node }
                    if (i != -1) {
                        children[i].dispose()

                        // comma
                        if (i == 0 && list.size > 1)
                            children[i].dispose()
                        else if (i != 0)
                            children[i - 1].dispose()
                    }
                    if (parameters.size == 1) {
                        createInsert()
                        insert?.setFocus()
                    } else if (index == 0) {
                        children[0].setFocus()
                    } else {
                        val prev =
                            children.indexOfFirst { it is ParamWidget && it.node == list[index - 1] }
                        (children[prev] as ParamWidget).setFocusOnCreation(false)
                    }
                    requestLayout()
                }

                override fun elementReplace(
                    list: NodeList<Parameter>,
                    index: Int,
                    old: Parameter,
                    new: Parameter
                ) {
                    TODO("parameter replacement")
                }

            })
        }

        private fun createInsert() {
            val newInsert = TextWidget.create(this, " ") { c, s, _ ->
                c.toString().matches(TYPE_CHARS) || c == SWT.BS
            }
            newInsert.addKeyEvent(
                SWT.SPACE,
                precondition = { isValidType(it) }) {
                parameters.addCommand(
                    node,
                    Parameter(
                        StaticJavaParser.parseType(newInsert.text),
                        SimpleName("parameter")
                    )
                )
            }
            newInsert.addFocusLostAction {
                newInsert.clear(" ")
            }
            insert = newInsert
        }


        private fun addParams() {
            parameters.forEachIndexed { index, parameter ->
                if (index != 0)
                    FixedToken(this, ",")

                ParamWidget(this, index, parameter)
            }
        }

        inner class ParamWidget(
            parent: Composite,
            val index: Int,
            override val node: Parameter
        ) :
            Composite(parent, SWT.NONE), NodeWidget<Parameter> {
            val type: Id
            val name: Id

            init {
                layout = ROW_LAYOUT_H_SHRINK
                font = parent.font
                background = parent.background
                foreground = parent.foreground
                type = SimpleTypeWidget(this, node.type)

                type.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    parameters.removeCommand(
                        this@MethodWidget.node,
                        node
                    ) // TODO BUG Index -1 out of bounds for length 1
                }

                type.addFocusLostAction(::isValidType) {
                    node.modifyCommand(node.typeAsString, it, node::setType)
                }

                name = SimpleNameWidget(this, node)
                name.addKeyEvent(',') {
                    parameters.addCommand(
                        this@MethodWidget.node,
                        Parameter(
                            StaticJavaParser.parseType("type"),
                            SimpleName("parameter")
                        )
                    )
                }
                name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    parameters.removeCommand(this@MethodWidget.node, node)
                }

                name.addFocusLostAction(::isValidSimpleName) {
                    node.modifyCommand(
                        node.name,
                        SimpleName(name.text),
                        node::setName
                    )
                }

                observeNotNullProperty<Type>(ObservableProperty.TYPE, target = node) {
                    type.set(it.asString())
                }
                observeNotNullProperty<SimpleName>(ObservableProperty.NAME, target = node) {
                    name.set(it.asString())
                }
            }

            override fun setFocusOnCreation(firstFlag: Boolean) {
                if (firstFlag)
                    name.setFocus()
                else
                    type.setFocus()
            }

            override val control: Control
                get() = this
        }
    }


    override fun setFocus(): Boolean {
        return name.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        name.setFocus()
    }

    fun focusParameters() = paramsWidget.setFocus()
}

