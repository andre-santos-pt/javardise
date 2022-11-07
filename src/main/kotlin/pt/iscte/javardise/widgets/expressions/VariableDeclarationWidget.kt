package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Id
import pt.iscte.javardise.SimpleNameWidget
import pt.iscte.javardise.SimpleTypeWidget
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature
import kotlin.reflect.KFunction1

class VariableDeclarationWidget(
    parent: Composite,
    override val node: VariableDeclarationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<VariableDeclarationExpr>(parent) {
    var type: Id
    var name: Id
    var equals: FixedToken? = null
    var expression: ExpressionWidget<*>? = null

    init {
        val decl = node.variables[0] // multi variable not supported

        type = SimpleTypeWidget(this, decl.type)
        type.addFocusLostAction(::isValidType) {
            node.modifyCommand(
                node.commonType,
                StaticJavaParser.parseType(type.text),
                node::setAllTypes
            )
        }
        type.addDeleteListener {
            if(decl.initializer.isPresent)
                editEvent(AssignExpr(decl.nameAsExpression, decl.initializer.get(),  AssignExpr.Operator.ASSIGN))
            else
                editEvent(null)
        }

        name = SimpleNameWidget(this, decl)
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(decl.name, SimpleName(name.text), decl::setName)
        }
        name.addKeyEvent('=') {
            val setter: KFunction1<Expression?, Node> = decl::setInitializer
            decl.modifyCommand(decl.initializer.getOrNull, NameExpr("expression"), setter)
        }
        name.addDeleteEmptyListener {
            if(decl.initializer.isPresent)
                editEvent(AssignExpr(decl.nameAsExpression, decl.initializer.get(),  AssignExpr.Operator.ASSIGN))
            else
                editEvent(null)
        }

        if (decl.initializer.isPresent) {
            equals = FixedToken(this, "=")
            expression = createExpWidget(decl, decl.initializer.get())
        }

        node.observeProperty<Type>(ObservableProperty.TYPE) {
            type.set(it?.asString())
        }
        decl.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it.toString())
        }
        decl.observeProperty<Expression>(ObservableProperty.INITIALIZER) {
            if(it == null) {
                equals?.dispose()
                equals = null
                expression?.dispose()
                expression = null
                requestLayout()
                name.setFocus()
            }
            else {
                expression?.dispose()
                if(equals == null)
                    equals = FixedToken(this, "=")
                expression = this.createExpWidget(decl, it)
                expression!!.requestLayout()
                expression!!.setFocusOnCreation()
            }
        }
    }

    private fun Composite.createExpWidget(
        variable: VariableDeclarator,
        expression: Expression
    ) = createExpressionWidget(this, expression) {
           variable.modifyCommand(variable.initializer.getOrNull, it, variable::setInitializer)
        }

    override val tail: TextWidget
        get() = expression?.tail ?: name

    override fun setFocus(): Boolean {
        return type.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression?.setFocus() ?: name.setFocus()
    }
}

object VariableDeclarationFeature : StatementFeature<ExpressionStmt, ExpressionStatementWidget>(ExpressionStmt::class.java, ExpressionStatementWidget::class.java) {

    override fun targets(stmt: Statement): Boolean = stmt is ExpressionStmt && stmt.expression is VariableDeclarationExpr

    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {

        insert.addKeyEvent(';', precondition = {
            insert.isAtEnd && it.split(Regex("\\s+")).size == 2 && isValidType(
                it.split(
                    Regex("\\s+")
                )[0]
            ) && tryParse<NameExpr>(
                it.split(Regex("\\s+"))[1]
            )
        }) {
            val split = insert.text.split(Regex("\\s+"))
            val stmt = ExpressionStmt(
                VariableDeclarationExpr(
                    StaticJavaParser.parseType(split[0]), split[1]
                )
            )
            output(stmt)
        }


        insert.addKeyEvent('=', precondition = {
            val parts = it.trim().split(Regex("\\s+"))
            insert.isAtEnd && parts.size == 2 && isValidType(parts[0]) && tryParse<NameExpr>(
                parts[1]
            )
        }) {
            val split = insert.text.split(Regex("\\s+"))
            val dec = VariableDeclarator(
                StaticJavaParser.parseType(split[0]),
                split[1],
                NameExpr("expression")
            )
            val stmt = ExpressionStmt(VariableDeclarationExpr(dec))
            output(stmt)
        }
    }
}