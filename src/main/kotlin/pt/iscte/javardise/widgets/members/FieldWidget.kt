package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.isValidSimpleName
import pt.iscte.javardise.external.isValidType
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class FieldWidget(
    parent: Composite,
    val dec: FieldDeclaration,
    configuration: Configuration = DefaultConfigurationSingleton,
    validModifiers: List<List<Modifier.Keyword>> = configuration.fieldModifiers,
    override val commandStack: CommandStack = CommandStack.create(),
) :
    MemberWidget<FieldDeclaration>(
        parent, dec, configuration, validModifiers
    ),  ConfigurationRoot {

    // multi var is transformed to list of singles on parse
    val FieldDeclaration.variable: VariableDeclarator get() = this.variables[0]

    override val type: Id
    override val name: Id
    var equals: TokenWidget? = null
    var initializer: ExpressionWidget<*>? = null
    val semiColon: TokenWidget


    init {
        type = SimpleTypeWidget(firstRow, dec.commonType)
        type.addFocusLostAction(::isValidType) {
            dec.modifyCommand(
                dec.elementType,
                StaticJavaParser.parseType(it),
                dec::setAllTypes
            )
        }
        configureInsert(type)

        name = SimpleNameWidget(firstRow, dec.variable)
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(
                dec.variable.nameAsString,
                it,
                dec.variable::setName
            )
        }

        name.addKeyEvent('=', precondition = {name.isAtEnd}) {
            node.modifyCommand(
                dec.variable.initializer.getOrNull,
                Configuration.hole(),
                dec.variable::setInitializer
            )
        }

        name.setCopySource(node)

        semiColon = TokenWidget(firstRow, ";")
        semiColon.addKeyEvent('=', precondition = { initializer == null }) {
            node.modifyCommand(
                dec.variable.initializer.getOrNull,
                Configuration.hole(),
                dec.variable::setInitializer
            )
        }

        if (dec.variable.initializer.isPresent)
            addInitializer(dec.variable.initializer.get())

        observeNotNullProperty<Type>(ObservableProperty.TYPE, target = dec.variable) {
            type.set(it.toString())
        }

        observeNotNullProperty<SimpleName>(
            ObservableProperty.NAME,
            target = dec.variable
        ) {
            name.set(it.id)
            //name.textWidget.widget.data = it
        }

        observeProperty<Expression>(
            ObservableProperty.INITIALIZER,
            target = dec.variable
        ) {
            if (initializer == null && it != null) {
                addInitializer(it)
            } else if (initializer != null && it == null) {
                equals!!.dispose()
                initializer!!.dispose()
                initializer = null
                name.setFocus()
                firstRow.requestLayout()
            } else {
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
            dec.modifyCommand(
                dec.variable.initializer.getOrNull,
                it,
                dec.variable::setInitializer
            )
        }
        equals!!.moveAboveInternal(semiColon.widget)
        initializer!!.moveBelow(equals!!.widget)
        initializer!!.requestLayout()
        initializer!!.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        type.setFocus()
    }

    override fun setFocus(): Boolean {
        return firstRow.setFocus()
    }

    fun focusExpressionOrSemiColon() {
        initializer?.setFocus() ?: semiColon.setFocus()
    }

}