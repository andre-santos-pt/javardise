package pt.iscte.javardise.widgets.statements

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
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
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget
import pt.iscte.javardise.widgets.members.addInsert

// TODO delete initializer
// to expression?
class VariableWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    override val block: BlockStmt
) : StatementWidget<ExpressionStmt>(parent, node) {
    var type: Id
    var name: Id
    var expression: ExpressionWidget<*>? = null

    init {
        require(node.expression is VariableDeclarationExpr)

        val assignment = node.expression as VariableDeclarationExpr
        val decl = assignment.variables[0] // multi variable not supported

        layout = ROW_LAYOUT_H_SHRINK

        // TODO delete to assign
        type = SimpleTypeWidget(this, decl.type) { it.asString() }
        type.addFocusLostAction(::isValidType) {
            node.modifyCommand(
                assignment.commonType,
                StaticJavaParser.parseType(type.text),
                assignment::setAllTypes
            )
        }
        name = SimpleNameWidget(this, decl) { it.name.asString() }
        name.addKeyEvent(
            SWT.BS,
            precondition = { it.isEmpty() },
            action = createDeleteEvent(node, block)
        )
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(decl.name, SimpleName(name.text), decl::setName)
        }

        if (decl.initializer.isPresent) {
            FixedToken(this, "=")
            expression = createExpWidget(decl, decl.initializer.get())
        }
        val semiColon = TokenWidget(this, ";")
        semiColon.addInsert(this, this.parent as SequenceWidget, true)

        assignment.observeProperty<Type>(ObservableProperty.TYPE) {
            type.set(it?.asString())
        }
        decl.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it.toString())
        }
        decl.observeProperty<Expression>(ObservableProperty.INITIALIZER) {
            expression?.dispose()
            expression = this.createExpWidget(decl, it ?: NameExpr("expression"))
            expression!!.moveAbove(semiColon.widget)
            expression!!.requestLayout()
            expression!!.setFocusOnCreation()
        }
    }

    //fun VariableDeclarator.setInit(e: Expression): VariableDeclarator = setInitializer(e)

    private fun Composite.createExpWidget(
        variable: VariableDeclarator,
        expression: Expression
    ) =
        createExpressionWidget(this, expression) {
           // node.modifyCommand(variable.initializer, it, variable::setInit)
            Commands.execute(object : ModifyCommand<Expression>(
                variable,
                if(variable.initializer.isPresent) variable.initializer.get() else null
            ) {
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