package javawidgets

import basewidgets.*
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label


val MODIFIERS = "(${Modifier.Keyword.values().joinToString(separator = "|") { it.asString() }})"
val MEMBER_REGEX = Regex(
    "($MODIFIERS\\s+)*$ID\\s+$ID"
)


fun matchModifier(keyword: String) =
    Modifier(Modifier.Keyword.valueOf(keyword.uppercase()))

class ClassWidget(parent: Composite, type: ClassOrInterfaceDeclaration) :
    MemberWidget<ClassOrInterfaceDeclaration>(parent, type, listOf("public", "final", "abstract")) {
    private val keyword: TokenWidget
    private val id: Id
    internal var body: SequenceWidget

    val CONSTRUCTOR_REGEX = { Regex("($MODIFIERS\\s+)*${type.nameAsString}") }

    init {
        data = "ROOTAREA"
        layout = FillLayout()
        keyword = Factory.newTokenWidget(firstRow, "class")
        keyword.addKeyEvent(SWT.SPACE) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element = Modifier(Modifier.Keyword.PUBLIC)

                val index = node.modifiers.size

                override fun run() {
                    node.modifiers.add(index, element)
                }

                override fun undo() {
                    node.modifiers.remove(element)
                }
            })
        }

        id = SimpleNameWidget(firstRow, type.name) {
            it.asString()
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

            insert.addKeyEvent(';','=', SWT.CR, precondition = { it.matches(MEMBER_REGEX) }) {
                val split = insert.text.split(Regex("\\s+"))
                val newField = FieldDeclaration(
                    modifiers(2),
                    StaticJavaParser.parseType(split[split.lastIndex - 1]),
                    split.last()
                )
                if(it.character == '=')
                    newField.variables[0].setInitializer(NameExpr("expression"))

                val insertIndex = seq.findIndexByModel(insert.widget)
                Commands.execute(AddMemberCommand(newField, node, insertIndex))
                insert.delete()
            }

            insert.addKeyEvent('(', precondition = { it.matches(CONSTRUCTOR_REGEX()) }) {
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
            insert.addFocusLostAction {
                insert.clear()
            }
            insert
        }
        TokenWidget(firstRow, "{").addInsert(null, body, false)

        type.members.forEach {
            createMember(it)
        }

        FixedToken(column, "}")

        type.observeProperty<SimpleName>(ObservableProperty.NAME) {
            id.set(it?.id ?: "")
            id.textWidget.data = it
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
                    val w = createMember(nodeAddedOrRemoved as BodyDeclaration<*>)
                    if (!tail)
                        w.moveAbove(body.findByModelIndex(index))

                    if (w is MethodWidget)
                        w.focusParameters()
                    else
                        (w as FieldWidget).focusExpressionOrSemiColon()

                } else {
                    body.find(nodeAddedOrRemoved)?.dispose()
                }
                body.requestLayout()
            }
        })
    }

    fun createMember(dec: BodyDeclaration<*>) =
        when (dec) {
            is FieldDeclaration ->  {
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
                val label = Label(body, SWT.NONE)
                label.text = "Unsupported - $dec"
                label
            }
        }

    override fun setFocusOnCreation() {
        id.setFocus()
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



