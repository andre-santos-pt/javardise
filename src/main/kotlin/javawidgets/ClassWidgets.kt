package javawidgets

import basewidgets.*
import com.github.javaparser.JavaParser
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display

val ID = "[a-zA-Z_][a-zA-Z_0-9]*"
val MODIFIERS = "(${Modifier.Keyword.values().joinToString(separator = "|") { it.asString() }})"
val MEMBER_REGEX = Regex(
    "($MODIFIERS\\s+)*$ID\\s+$ID"
)


fun matchModifier(keyword: String) =
    Modifier(Modifier.Keyword.valueOf(keyword.uppercase()))

//fun matchType(name: String) =
//    if(PrimitiveType.Primitive.values().map { it.name.lowercase() }.contains(name))
//        ;
//    else
//        StaticJavaParser.par
class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type, listOf("public", "final", "abstract")) {
    private val keyword: TokenWidget
    private val id: Id
    internal var body: SequenceWidget

    val CONSTRUCTOR_REGEX = Regex(
        "($MODIFIERS\\s+)*${type.nameAsString}"
    )

    init {
        println(type.name.begin.get().toString() + " " + type.name.end)
        data = "ROOTAREA"
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


        body = SequenceWidget(column, 1) { seq, e ->
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

            insert.addKeyEvent(';', precondition = { it.matches(MEMBER_REGEX) }) {
                val split = insert.text.split(Regex("\\s+"))
                val newField = FieldDeclaration(
                    modifiers(2),
                    StaticJavaParser.parseType(split[split.lastIndex - 1]),
                    split.last()
                )
                val insertIndex = seq.findIndexByModel(insert.widget)
                Commands.execute(AddMemberCommand(newField, node, insertIndex))
                insert.delete()
            }
            insert.addKeyEvent('(', precondition = { it.matches(CONSTRUCTOR_REGEX) }) {
                val newConst = ConstructorDeclaration(modifiers(1), type.nameAsString)
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
                val insertIndex = seq.findIndexByModel(insert.widget)
                Commands.execute(AddMemberCommand(newMethod, node, insertIndex))
                insert.delete()
            }
            insert.addFocusListenerInternal(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    insert.clear()
                }
            })
            insert
        }
        TokenWidget(firstRow, "{").addInsert(null, body, false)

        // firstRow.addInsert(null, body, node, false)

        type.members.forEach {
            createMember(type, it)
        }
        FixedToken(column, "}")

        type.observeProperty<SimpleName>(ObservableProperty.NAME) {
            id.text = it?.id ?: ""
        }

        type.members.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                change: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node
            ) {
                if (change == AstObserver.ListChangeType.ADDITION) {
                    val tail = index == type.members.size
                    val w = createMember(type, nodeAddedOrRemoved as BodyDeclaration<*>)
                    if (!tail)
                        w.moveAbove(body.findByModelIndex(index))

                    if (w is MethodWidget)
                        w.focusParameters()
                    else
                        (w as FieldWidget).focusSemiColon()

                } else {
                    body.find(nodeAddedOrRemoved)?.dispose()
                }
                body.requestLayout()
            }
        })

    }

    fun createMember(type: ClassOrInterfaceDeclaration, dec: BodyDeclaration<*>) =
        when (dec) {
            is FieldDeclaration -> FieldWidget(body, dec)
            is MethodDeclaration, is ConstructorDeclaration -> {
                val w = MethodWidget(body, dec as CallableDeclaration<*>)
                //body.addInsert(w, body, node, true)
                w
            }

            else -> TODO("unsupported - $dec")
        }

    override fun setFocusOnCreation() {
        id.setFocus()
    }


    inner class FieldWidget(parent: Composite, val dec: FieldDeclaration) :
        MemberWidget<FieldDeclaration>(parent, dec) {

        val typeId: Id

        val semiColon: TokenWidget

        init {
            typeId = Id(firstRow, dec.elementType.toString())
            val name = Id(firstRow, dec.variables[0].name.asString()) // TODO multi var
            semiColon = TokenWidget(firstRow, ";")
            semiColon.addInsert(this, body, true)

            name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {
                Commands.execute(object : Command {
                    override val target: ClassOrInterfaceDeclaration =
                        dec.parentNode.get() as ClassOrInterfaceDeclaration
                    override val kind: CommandKind = CommandKind.REMOVE
                    override val element: Node = dec
                    val index: Int = target.members.indexOf(dec)
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

        fun focusSemiColon() {
            semiColon.setFocus()
        }

    }
}

internal fun TokenWidget.addInsert(
    member: Control?,
    body: SequenceWidget,
    //node: ClassOrInterfaceDeclaration,
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




