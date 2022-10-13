package pt.iscte.javardise.widgets.statements

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.Id
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.external.*

class VariableWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent, node) {
    var type: Id
    var name: Id
    var expression: ExpWidget<*>? = null

    init {
        require(node.expression is VariableDeclarationExpr)

        val assignment = node.expression as VariableDeclarationExpr
        val decl = assignment.variables[0] // multi variable not supported

        layout = ROW_LAYOUT_H_SHRINK

        // TODO delete to assign
        type = SimpleTypeWidget(this, decl.type) { it.asString() }
        type.addFocusLostAction {
            if (type.text != decl.typeAsString)
                if (tryParseType(type.text))
                    Commands.execute(object : ModifyCommand<Type>(assignment, assignment.commonType) {
                        override fun run() {
                            assignment.setAllTypes(StaticJavaParser.parseType(type.text))
                        }

                        override fun undo() {
                            assignment.setAllTypes(element)
                        }
                    })
                else
                    type.set(decl.typeAsString)
        }

        name = SimpleNameWidget(this, decl) { it.name.asString() }
        name.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }, action = createDeleteEvent(node, block))
        name.addFocusLostAction {
            if (name.text != decl.nameAsString)
                Commands.execute(object : ModifyCommand<SimpleName>(decl, decl.name) {
                    override fun run() {
                        decl.name = SimpleName(name.text)
                    }

                    override fun undo() {
                        decl.name = element
                    }
                })
        }

        if (decl.initializer.isPresent) {
            FixedToken(this, "=")
            expression = createExpWidget(decl, decl.initializer.get())
        }
        val semiColon = TokenWidget(this, ";")
        semiColon.addInsert(this@VariableWidget, this@VariableWidget.parent as SequenceWidget, true)

        assignment.observeProperty<Type>(ObservableProperty.TYPE) {
            type.set(it?.asString())
        }
        decl.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it.toString())
        }
        decl.observeProperty<Expression>(ObservableProperty.INITIALIZER) {
            expression?.dispose()
            expression = this@VariableWidget.createExpWidget(decl, it!!)
            expression!!.moveAbove(semiColon.widget)
            expression!!.requestLayout()
        }
    }

    private fun Composite.createExpWidget(variable: VariableDeclarator, expression: Expression) =
        createExpressionWidget(this, expression) {
            Commands.execute(object : ModifyCommand<Expression>(variable, variable.initializer.get()) {
                override fun run() {
                    variable.setInitializer(it)
                }

                override fun undo() {
                    variable.setInitializer(element)
                }
            })
        }

    override fun setFocus(): Boolean {
        return name.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression?.setFocus()
    }
}