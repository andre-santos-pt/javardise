package pt.iscte.javardise.widgets

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier.Keyword.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.Id
import pt.iscte.javardise.basewidgets.TokenWidget

class FieldWidget(parent: Composite, val dec: FieldDeclaration) :
    MemberWidget<FieldDeclaration>(parent, dec, listOf(PUBLIC, PRIVATE, PROTECTED, FINAL)) {

    // multi var is transformed to list of singles on parse
    val FieldDeclaration.variable: VariableDeclarator get() = this.variables[0]

    val type: Id
    val name: Id
    var equals: TokenWidget? = null
    var initializer: ExpWidget<*>? = null
    val semiColon: TokenWidget

    init {
        type = SimpleTypeWidget(firstRow, dec.elementType) {
            it.asString()
        }

        type.addInsertModifier()

        name = SimpleNameWidget(firstRow, dec.variable.name) {
            it.asString()
        }

        semiColon = TokenWidget(firstRow, ";")

        semiColon.addKeyEvent('=', precondition = { initializer == null }) {
            Commands.execute(object : AddCommand<Expression>(dec.variable, NameExpr("expression")) {
                override fun run() {
                    dec.variable.setInitializer(element)
                }

                override fun undo() {
                    val nulll: Expression? = null
                    dec.variable.setInitializer(nulll)
                }
            })
        }

        if (dec.variable.initializer.isPresent)
            addInitializer(dec.variable.initializer.get())

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

        type.addFocusLostAction {
            val parse = try {
                StaticJavaParser.parseType(type.text)
            } catch (e: ParseProblemException) {
                null
            }
            if (parse != null && parse.asString() != dec.elementType.asString())
                Commands.execute(object : ModifyCommand<Type>(dec, dec.elementType) {
                    override fun run() {
                        dec.setAllTypes(parse)
                    }

                    override fun undo() {
                        dec.setAllTypes(element)
                    }

                })
            else
                type.set(dec.elementType.asString())
        }

        name.addFocusLostAction {
            if (name.text.isNotEmpty() && name.text != dec.variable.nameAsString)
                Commands.execute(object : ModifyCommand<SimpleName>(dec, dec.variable.name) {
                    override fun run() {
                        dec.variable.name = SimpleName(name.text)
                    }

                    override fun undo() {
                        dec.variable.name = element
                    }
                })
            else
                name.set(dec.variable.name.asString())
        }

        dec.variable.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it?.id ?: "")
            name.textWidget.data = it
        }
        dec.variable.observeProperty<Expression>(ObservableProperty.INITIALIZER) {
            if (initializer == null && it != null) {
                addInitializer(it)
            }

            else if (initializer != null && it == null) {
                equals!!.dispose()
                initializer!!.dispose()
                initializer = null
                firstRow.requestLayout()
            }
            else {
                it?.let {
                    equals?.dispose()
                    initializer?.dispose()
                    addInitializer(it)
                }
            }
        }
    }

    private fun addInitializer(expression: Expression) {
        equals = TokenWidget(firstRow, "=")
        initializer = createExpressionWidget(firstRow, expression) {
            Commands.execute(object : ModifyCommand<Expression>(dec, dec.variable.initializer.get()) {
                override fun run() {
                    dec.variable.setInitializer(it)
                }

                override fun undo() {
                    dec.variable.setInitializer(element)
                }
            })
        }
        equals!!.moveAboveInternal(semiColon.widget)
        initializer!!.moveBelow(equals!!.widget)
        initializer!!.requestLayout()
        initializer!!.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        type.setFocus()
    }

    fun focusExpressionOrSemiColon() {
        initializer?.let {
            it.setFocus()
        } ?: semiColon.setFocus()
    }

}