package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

// TODO delete initializer
class VariableDeclarationWidget(
    parent: Composite,
    override val node: VariableDeclarationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<VariableDeclarationExpr>(parent) {
    var type: Id
    var name: Id
    var expression: ExpressionWidget<*>? = null

    init {
        val decl = node.variables[0] // multi variable not supported

        layout = ROW_LAYOUT_H_SHRINK

        // TODO delete to assign
        type = SimpleTypeWidget(this, decl.type) { it.asString() }
        type.addFocusLostAction(::isValidType) {
            node.modifyCommand(
                node.commonType,
                StaticJavaParser.parseType(type.text),
                node::setAllTypes
            )
        }
        name = SimpleNameWidget(this, decl) { it.name.asString() }
//        name.addKeyEvent(
//            SWT.BS,
//            precondition = { it.isEmpty() },
//            action = createDeleteEvent(node, block)
//        )
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(decl.name, SimpleName(name.text), decl::setName)
        }

        if (decl.initializer.isPresent) {
            FixedToken(this, "=")
            expression = createExpWidget(decl, decl.initializer.get())
        }
       // val semiColon = TokenWidget(this, ";")
        //semiColon.addInsert(this, this.parent as SequenceWidget, true)

        node.observeProperty<Type>(ObservableProperty.TYPE) {
            type.set(it?.asString())
        }
        decl.observeProperty<SimpleName>(ObservableProperty.NAME) {
            name.set(it.toString())
        }
        decl.observeProperty<Expression>(ObservableProperty.INITIALIZER) {
            expression?.dispose()
            expression = this.createExpWidget(decl, it ?: NameExpr("expression"))
            //expression!!.moveBelow(semiColon.widget)
            expression!!.requestLayout()
            expression!!.setFocusOnCreation()
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

class VariableDeclarationFeature : StatementFeature<ExpressionStmt, ExpressionStatementWidget>(ExpressionStmt::class.java, ExpressionStatementWidget::class.java) {

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