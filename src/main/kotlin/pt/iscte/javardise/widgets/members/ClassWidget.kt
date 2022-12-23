package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.SequenceContainer
import pt.iscte.javardise.widgets.statements.find
import pt.iscte.javardise.widgets.statements.findByModelIndex
import pt.iscte.javardise.widgets.statements.findIndexByModel


// TODO implements, extends

val MODIFIERS = "(${
    Modifier.Keyword.values().joinToString(separator = "|") { it.asString() }
})"
val TYPE = Regex("$ID(<$ID(,$ID)*>)?(\\[\\])*")

val MEMBER_REGEX = Regex(
    "($MODIFIERS\\s+)*$TYPE\\s+$ID"
)


fun matchModifier(keyword: String) =
    Modifier(Modifier.Keyword.valueOf(keyword.uppercase()))


// TODO require compliant model
open class ClassWidget(
    parent: Composite,
    dec: ClassOrInterfaceDeclaration,
    configuration: Configuration = DefaultConfigurationSingleton,
    validModifiers: List<List<Modifier.Keyword>> = configuration.classModifiers,
    override val commandStack: CommandStack = CommandStack.create(),
    val staticClass: Boolean = false,
) :
    MemberWidget<ClassOrInterfaceDeclaration>(
        parent, dec, configuration, validModifiers
    ), SequenceContainer<ClassOrInterfaceDeclaration>,
    ConfigurationRoot {

    private val keyword: TokenWidget

    override val type: TextWidget? = null
    final override val name: Id

    final override val bodyWidget: SequenceWidget
    final override val closingBracket: TokenWidget

    override val body: BlockStmt? = null

    private val modelFocusObservers =
        mutableListOf<(BodyDeclaration<*>?, Node?) -> Unit>()

    private val widgetFocusObservers = mutableListOf<(Control) -> Unit>()

    private val focusListenerGlobal = { event: Event ->
        val control = event.widget as Control
        if (control.isChildOf(this@ClassWidget)) {
            widgetFocusObservers.forEach {
                it(control)
            }
            val memberWidget = control.findNode<BodyDeclaration<*>>()
            val nodeWidget = control.findNode<Node>()
            modelFocusObservers.forEach {
                it(memberWidget, nodeWidget)
            }
        }
    }

    fun getMemberOnFocus(): BodyDeclaration<*>? {
        val onFocus = Display.getDefault().focusControl
        return onFocus?.findNode()
    }

    fun focus(member: BodyDeclaration<*>) {
        require(node.members.contains(member))
        findChild(member)?.setFocus()
    }


    fun setAutoScroll() {
        require(parent is ScrolledComposite)
        val scroll = parent as ScrolledComposite
        addFocusObserver { control ->
            val p = Display.getDefault().map(control, scroll, control.location)
            p.x = 0
            p.y += 10
            if (p.y < scroll.origin.y) {
                scroll.origin = p
            } else if (p.y > scroll.origin.y + scroll.bounds.height) {
                scroll.origin = p
            }
        }
    }

    enum class TypeTypes {
        CLASS, INTERFACE;
        //, ENUM;

        fun element(type: ClassOrInterfaceDeclaration) =
            when (this) {
                CLASS -> !type.isInterface
                INTERFACE -> type.isInterface
                //ENUM -> type.isEnumDeclaration
            }

        fun apply(type: ClassOrInterfaceDeclaration) =
            when (this) {
                CLASS -> type.isInterface = false
                INTERFACE -> {
                    type.modifiers.removeIf { it.keyword == Modifier.Keyword.FINAL }
                    type.isInterface = true
                }
                //ENUM -> type.setE
            }

        fun applyReverse(type: ClassOrInterfaceDeclaration) =
            when (this) {
                CLASS -> type.isInterface = true
                INTERFACE -> type.isInterface = false
                //ENUM -> type.setE
            }
    }

    init {
        layout = ROW_LAYOUT_H_SHRINK
        val layout = RowLayout()
        layout.marginTop = 10
        layout.marginLeft = 10
        this.layout = layout

        val insertModifier = TextWidget.create(firstRow)
        insertModifier.widget.layoutData = ROW_DATA_STRING
        configureInsert(insertModifier) {
            if(node.isInterface)
                it != Modifier.Keyword.FINAL
            else
                true
        }

        keyword = newKeywordWidget(firstRow, if(node.isInterface) "interface" else "class",
            alternatives = { TypeTypes.values().map { it.name.lowercase() } }) {
            commandStack.execute(object : Command {
                override val target = node
                override val kind = CommandKind.MODIFY
                override val element =
                    TypeTypes.valueOf(it.uppercase()).element(node)

                override fun run() {
                    TypeTypes.valueOf(it.uppercase()).apply(node)
                }

                override fun undo() {
                    TypeTypes.valueOf(it.uppercase()).applyReverse(node)
                }
            })
        }
//        keyword.addKeyEvent(SWT.SPACE) {
//            commandStack.execute(object : Command {
//                override val target = node
//                override val kind = CommandKind.ADD
//                override val element = Modifier(PUBLIC)
//
//                val index = node.modifiers.size
//
//                override fun run() {
//                    node.modifiers.add(index, element)
//                }
//
//                override fun undo() {
//                    node.modifiers.remove(element)
//                }
//            })
//        }

        name = SimpleNameWidget(firstRow, dec)
        name.addFocusLostAction(::isValidClassType) {
            commandStack.execute(object : Command {
                override val target: Node
                    get() = node
                override val kind: CommandKind = CommandKind.MODIFY
                override val element = node.name

                override fun run() {
                    node.setName(it)
                    node.constructors.forEach { c ->
                        c.name = SimpleName(it)
                    }
                }

                override fun undo() {
                    node.name = element
                    node.constructors.forEach { c ->
                        c.name = element
                    }
                }

            })
        }
        bodyWidget = SequenceWidget(
            column,
            if (staticClass) 0 else configuration.tabLength,
            if (staticClass) 0 else 10
        ) { seq, _ ->
            createInsert(seq)
        }
        TokenWidget(firstRow, "{").addInsert(null, bodyWidget, false)

        node.members.forEach {
            createMember(it)
        }

        closingBracket = TokenWidget(column, "}")

        if (staticClass) {
            firstRow.dispose()
            closingBracket.dispose()
        }

        observeNotNullProperty<Boolean>(ObservableProperty.INTERFACE) {
            keyword.set(if(it) "interface" else "class")
            name.setFocus()
        }
        observeNotNullProperty<SimpleName>(ObservableProperty.NAME) {
            name.set("$it")
            name.textWidget.data = it
        }
        observeListUntilDispose(node.members, object : ListObserver<BodyDeclaration<*>> {
            override fun elementAdd(
                list: NodeList<BodyDeclaration<*>>,
                index: Int,
                member: BodyDeclaration<*>
            ) {
                val tail = index == node.members.size
                val w = createMember(member)
                if (!tail)
                    w.moveAbove(bodyWidget.findByModelIndex(index) as Control)

                if (w is MethodWidget)
                    w.focusParameters()
                else
                    (w as FieldWidget).focusExpressionOrSemiColon()
                bodyWidget.requestLayout()
            }

            override fun elementRemove(
                list: NodeList<BodyDeclaration<*>>,
                index: Int,
                member: BodyDeclaration<*>
            ) {
                (bodyWidget.find(member) as? Control)?.dispose()
                bodyWidget.focusAt(index)
                bodyWidget.requestLayout()
            }

            override fun elementReplace(
                list: NodeList<BodyDeclaration<*>>,
                index: Int,
                old: BodyDeclaration<*>,
                new: BodyDeclaration<*>
            ) {
                TODO("member replace")
            }
        })

        Display.getDefault().addFilter(SWT.FocusIn, focusListenerGlobal)
        addDisposeListener {
            Display.getDefault().removeFilter(SWT.FocusIn, focusListenerGlobal)
        }
    }

    /**
     * Adds an observer whenever a class member (field, constructor, method) gains focus.
     * Changes of focus within a member do not trigger an event.
     */
    fun addFocusObserver(action: (BodyDeclaration<*>?, Node?) -> Unit) {
        modelFocusObservers.add(action)
    }

    fun addFocusObserver(action: (Control) -> Unit) {
        widgetFocusObservers.add(action)
    }

    /**
     * Removes a previously registered an observer.
     */
    fun removeFocusObserver(action: (BodyDeclaration<*>?, Node?) -> Unit) {
        modelFocusObservers.remove(action)
    }

    fun createMember(dec: BodyDeclaration<*>): Composite =
        when (dec) {
            is FieldDeclaration -> {
                val w =
                    FieldWidget(bodyWidget, dec, configuration = configuration)
                w.semiColon.addInsert(w, bodyWidget, true)
                w.semiColon.addDeleteListener {
                    this@ClassWidget.node.members.removeCommand(
                        node as Node,
                        dec
                    )
                }
                w
            }
            is MethodDeclaration, is ConstructorDeclaration -> {
                val w = createMethodWidget(dec as CallableDeclaration<*>)
                w.closingBracket.addInsert(w, bodyWidget, true)
                w
            }
            is ClassOrInterfaceDeclaration -> {
                val w = ClassWidget(
                    bodyWidget,
                    dec,
                    configuration = configuration,
                    commandStack = commandStack
                )
                w.closingBracket.addInsert(w, bodyWidget, true)
                w
            }
            else -> {
                val w = UnsupportedWidget(bodyWidget, dec)
                w.widget.addDeleteListener {
                    this@ClassWidget.node.members.removeCommand(
                        node as Node,
                        dec
                    )
                }
                w.widget.addInsert(w, bodyWidget, true)
                w
            }
        }.apply {
            if (this is MemberWidget<*>) {
                type?.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                    this@ClassWidget.node.members.removeCommand(
                        node as Node,
                        dec
                    )
                }
                name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() || this.node.isConstructorDeclaration}) {
                    this@ClassWidget.node.members.removeCommand(
                        node as Node,
                        dec
                    )
                }
            }
        }


    private fun createInsert(seq: SequenceWidget): TextWidget {
        val CONSTRUCTOR_REGEX =
            { Regex("($MODIFIERS\\s+)*${node.nameAsString}") }

        val insert = TextWidget.create(seq) { c, s ->
            c.toString()
                .matches(Regex("[\\w\\d\\[\\]<>]")) || c == SWT.SPACE && s.isNotEmpty() || c == SWT.BS
        }

        fun modifiers(tail: Int): NodeList<Modifier> {
            val split = insert.text.split(Regex("\\s+"))
            val modifiers = NodeList<Modifier>()
            split.subList(0, split.size - tail).forEach {
                val m = matchModifier(it)
                modifiers.add(m)
            }
            return modifiers
        }

        if (!staticClass)
            insert.addKeyEvent('(',
                precondition = { it.matches(CONSTRUCTOR_REGEX()) }) {
                val newConst =
                    ConstructorDeclaration(modifiers(1), node.nameAsString)

                val insertIndex = seq.findIndexByModel(insert.widget)
                node.members.addCommand(node, newConst, insertIndex)
                insert.delete()
            }

        val memberChars =
            if (staticClass) arrayOf('(') else arrayOf(';', '=', '(')

        insert.addKeyEvent(*memberChars.toCharArray(), precondition = {
            if (it.matches(MEMBER_REGEX)) {
                val split = it.split(Regex("\\s+"))
                isValidType(split[split.lastIndex - 1]) && isValidSimpleName(
                    split[split.lastIndex]
                )
            } else
                false
        }) {
            val split = insert.text.split(Regex("\\s+"))
            val modifiers =
                NodeList(split.dropLast(2).map { matchModifier(it) })
            val dec = if (it.character == ';' || it.character == '=') {
                val newField = FieldDeclaration(
                    modifiers,
                    StaticJavaParser.parseType(split[split.lastIndex - 1]),
                    split.last()
                )
                if (it.character == '=')
                    newField.variables[0].setInitializer(NameExpr(Configuration.fillInToken))
                newField
            } else {
                val newMethod = MethodDeclaration(
                    modifiers,
                    split.last(),
                    StaticJavaParser.parseType(split[split.lastIndex - 1]),
                    NodeList()
                )
                newMethod.body.get().statements.add(EmptyStmt())
                if (node.isInterface)
                    newMethod.setBody(null)

                customizeNewMethodDeclaration(newMethod)
                newMethod
            }
            val insertIndex = seq.findIndexByModel(insert.widget)
            node.members.addCommand(node, dec, insertIndex)
            insert.delete()
        }
        insert.addFocusLostAction {
            insert.clear()
        }
        return insert
    }

    open fun customizeNewMethodDeclaration(dec: MethodDeclaration) {

    }

    open fun createMethodWidget(dec: CallableDeclaration<*>) =
        MethodWidget(
            bodyWidget,
            dec,
            configuration = configuration,
            validModifiers = configuration.methodModifiers,
            commandStack = commandStack
        )

    override fun setFocusOnCreation(firstFlag: Boolean) {
        name.setFocus()
    }

    internal fun TokenWidget.addInsert(
        member: Control?,
        body: SequenceWidget,
        after: Boolean
    ) {
        fun TextWidget.addInsert(
            member: Control?,
            body: SequenceWidget,
            after: Boolean
        ) {
            addKeyEvent(SWT.CR) {
                val w = if (member == null)
                    body.insertBeginning()
                else if (after)
                    body.insertLineAfter(member)
                else
                    body.insertLineAt(member)
                w.addInsert(w.widget, body, true)
            }
        }

        addKeyEvent(SWT.CR) {
            val w = if (member == null) {
                body.insertBeginning()
            } else if (after) {
                body.insertLineAfter(member)
            } else
                body.insertLineAt(member)
            //w.addInsert(w.widget, body, true)
        }
    }


}

