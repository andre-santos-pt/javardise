package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Modifier.Keyword.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.AddMemberCommand
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.isChild
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.widgets.members.FieldWidget
import pt.iscte.javardise.widgets.members.MemberWidget
import pt.iscte.javardise.widgets.members.MethodWidget
import pt.iscte.javardise.widgets.statements.find
import pt.iscte.javardise.widgets.statements.findByModelIndex
import pt.iscte.javardise.widgets.statements.findIndexByModel


val MODIFIERS = "(${Modifier.Keyword.values().joinToString(separator = "|") { it.asString() }})"
val MEMBER_REGEX = Regex(
    "($MODIFIERS\\s+)*$ID\\s+$ID"
)


fun matchModifier(keyword: String) =
    Modifier(Modifier.Keyword.valueOf(keyword.uppercase()))



class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type, listOf(PUBLIC, FINAL, ABSTRACT)), SequenceContainer {
    private val keyword: TokenWidget
    private val id: Id
    override lateinit var body: SequenceWidget
    override val closingBracket: TokenWidget

    private val observers = mutableListOf<(BodyDeclaration<*>?, Node?) -> Unit>()


//    val focusMemberTracker = object : Listener {
//        var current: MemberWidget<*>? = null
//        override fun handleEvent(e: Event) {
//            if ((e.widget as Control).isChild(this@ClassWidget)) {
//                val w = (e.widget as Control).findAncestor<MemberWidget<*>>()
//                if(w != current) {
//                    observers.forEach {
//                        var n = w?.node as? BodyDeclaration<*>
//                        it(n, w)
//                    }
//                    current = w
//                }
//            }
//        }
//    }

//    private val focusListener = { event: Event ->
//        if ((event.widget as Control).isChild(this@ClassWidget)) {
//            val w = (event.widget as Control).findAncestor<MemberWidget<*>>()
//            observers.forEach {
//                var n = w?.node as? BodyDeclaration<*>
//                it(n, w)
//            }
//        }
//    }

    private val focusListenerGlobal = { event: Event ->
        val control = event.widget as Control
        if (control.isChild(this@ClassWidget)) {
            val memberWidget = control.findNode<BodyDeclaration<*>>()
            val nodeWidget = control.findNode<Node>()
            observers.forEach {
                //val member = memberWidget?.node as? BodyDeclaration<*>
                //  val node = nodeWidget?.node as? Node
                it(memberWidget, nodeWidget)
            }
        }
    }

    enum class TypeTypes {
        CLASS, INTERFACE;
        //, ENUM;

        fun element(type: ClassOrInterfaceDeclaration) =
            when(this) {
                CLASS -> !type.isInterface
                INTERFACE -> type.isInterface
                //ENUM -> type.isEnumDeclaration
            }

        fun apply(type: ClassOrInterfaceDeclaration) =
            when(this) {
                CLASS -> type.isInterface = false
                INTERFACE -> type.isInterface = true
                //ENUM -> type.setE
            }

        fun applyReverse(type: ClassOrInterfaceDeclaration) =
            when(this) {
                CLASS -> type.isInterface = true
                INTERFACE -> type.isInterface = false
                //ENUM -> type.setE
            }
    }

    init {
        layout = ROW_LAYOUT_H_SHRINK
        keyword = Factory.newKeywordWidget(firstRow, "class",
            alternatives = {TypeTypes.values().map { it.name.lowercase() }}) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.MODIFY
                override val element = TypeTypes.valueOf(it.uppercase()).element(node)

                override fun run() {
                    TypeTypes.valueOf(it.uppercase()).apply(node)
                }

                override fun undo() {
                    TypeTypes.valueOf(it.uppercase()).applyReverse(node)
                }
            })
        }
        keyword.addKeyEvent(SWT.SPACE) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element = Modifier(PUBLIC)

                val index = node.modifiers.size

                override fun run() {
                    node.modifiers.add(index, element)
                }

                override fun undo() {
                    node.modifiers.remove(element)
                }
            })
        }

        id = SimpleNameWidget(firstRow, type) {
            it.name.asString()
        }

        id.addFocusLostAction {
            if (id.isValid() && id.text != node.nameAsString)
                Commands.execute(object : ModifyCommand<SimpleName>(node, node.name) {
                    override fun run() {
                        node.name = SimpleName(id.text)
                        node.constructors.forEach { it.name = node.name.clone() }
                    }

                    override fun undo() {
                        node.name = element
                    }
                })
            else {
                id.set(node.name.id)
            }
        }

        body = SequenceWidget(column, 1) { seq, _ ->
            createInsert(seq)
        }

        TokenWidget(firstRow, "{").addInsert(null, body, false)

        node.members.forEach {
            createMember(it)
        }

        closingBracket = TokenWidget(column, "}")

        registerObservers()
        Display.getDefault().addFilter(SWT.FocusIn, focusListenerGlobal)
    }

    /**
     * Adds an observer wheneven a class member (field, constructor, method) gains focus.
     * Changes of focus within a member do not trigger an event.
     */
    fun addMemberFocusObserver(action: (BodyDeclaration<*>?, Node?) -> Unit) {
        observers.add(action)
    }

 //   fun removeMemberFocusObserver(action: (BodyDeclaration<*>?, MemberWidget<*>?) -> Unit) {
   //     observers.remove(action)
    //}

    private fun registerObservers() {
        node.observeProperty<Boolean>(ObservableProperty.INTERFACE) {
            keyword.set(if(it!!) "interface" else "class")
            id.setFocus()
        }
        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            id.set(it?.id ?: "")
            id.textWidget.data = it
        }

        node.members.register(
            object : AstObserverAdapter() {
                override fun listChange(
                    observedNode: NodeList<*>,
                    change: AstObserver.ListChangeType,
                    index: Int,
                    nodeAddedOrRemoved: Node
                ) {
                    if (change == AstObserver.ListChangeType.ADDITION) {
                        val tail = index == node.members.size
                        val w = createMember(nodeAddedOrRemoved as BodyDeclaration<*>)
                        if (!tail)
                            w.moveAbove(body.findByModelIndex(index) as Control)

                        if (w is MethodWidget)
                            w.focusParameters()
                        else
                            (w as FieldWidget).focusExpressionOrSemiColon()

                    } else {
                        (body.find(nodeAddedOrRemoved) as? Control)?.dispose()
                    }
                    body.requestLayout()
                }
            })
    }

    override fun dispose() {
        Display.getDefault().removeFilter(SWT.FocusIn, focusListenerGlobal)
        super.dispose()
    }


    private fun createInsert(seq: SequenceWidget): TextWidget {
        val CONSTRUCTOR_REGEX = { Regex("($MODIFIERS\\s+)*${node.nameAsString}") }

        val insert = TextWidget.create(seq) { c, s ->
            Character.isLetter(c) || c == SWT.SPACE && s.isNotEmpty() || c == SWT.BS
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

        insert.addKeyEvent(';', '=', SWT.CR, precondition = { it.matches(MEMBER_REGEX) }) {
            val split = insert.text.split(Regex("\\s+"))
            val newField = FieldDeclaration(
                modifiers(2),
                StaticJavaParser.parseType(split[split.lastIndex - 1]),
                split.last()
            )
            if (it.character == '=')
                newField.variables[0].setInitializer(NameExpr("expression"))

            val insertIndex = seq.findIndexByModel(insert.widget)
            Commands.execute(AddMemberCommand(newField, node, insertIndex))
            insert.delete()
        }

        insert.addKeyEvent('(', precondition = { it.matches(CONSTRUCTOR_REGEX()) }) {
            val newConst = ConstructorDeclaration(modifiers(1), node.nameAsString)
            val insertIndex = seq.findIndexByModel(insert.widget)
            Commands.execute(AddMemberCommand(newConst, node, insertIndex))
            insert.delete()
        }

        insert.addKeyEvent('(', precondition = { it.matches(MEMBER_REGEX) }) {
            val split = insert.text.split(Regex("\\s+"))
            val newMethod = MethodDeclaration(
                modifiers(2),
                split.last(),
                StaticJavaParser.parseType(split[split.lastIndex - 1]),
                NodeList()
            )
            if(node.isInterface)
                newMethod.setBody(null)

            val insertIndex = seq.findIndexByModel(insert.widget)
            Commands.execute(AddMemberCommand(newMethod, node, insertIndex))
            insert.delete()
        }
        insert.addFocusLostAction {
            insert.clear()
        }
        return insert
    }

    fun createMember(dec: BodyDeclaration<*>) =
        when (dec) {
            is FieldDeclaration -> {
                val w = FieldWidget(body, dec)
                w.semiColon.addInsert(w, body, true)
                w
            }
            is MethodDeclaration, is ConstructorDeclaration -> {
                val w = MethodWidget(body, dec as CallableDeclaration<*>)
                w.closingBracket.addInsert(w, body, true)
                w
            }
            else -> {
                UnsupportedWidget(body, dec)
            }
        }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        id.setFocus()
    }
}

internal fun TokenWidget.addInsert(
    member: Control?,
    body: SequenceWidget,
    after: Boolean
) {
    addKeyEvent(SWT.CR) {
        if (member == null)
            body.insertBeginning()
        else if (after)
            body.insertLineAfter(member)
        else
            body.insertLineAt(member)
    }
}
