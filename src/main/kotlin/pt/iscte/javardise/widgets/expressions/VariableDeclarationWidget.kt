package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.isValidSimpleName
import pt.iscte.javardise.external.isValidType
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

class VariableDeclarationWidget(
    parent: Composite,
    override val node: VariableDeclarationExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<VariableDeclarationExpr>(parent) {
    var type: Id
    var name: Id
    var equals: FixedToken? = null
    var expression: ExpressionWidget<*>? = null

    val dec = node.variables[0] // multi variable not supported

    init {
        fun toAssign() {
            if (dec.initializer.isPresent)
                editEvent(
                    AssignExpr(
                        dec.nameAsExpression,
                        dec.initializer.get(),
                        AssignExpr.Operator.ASSIGN
                    )
                )
            else
                editEvent(null)
        }

        type = SimpleTypeWidget(this, dec.type)
        type.addFocusLostAction(::isValidType) {
            node.modifyCommand(
                node.commonType,
                StaticJavaParser.parseType(type.text),
                node::setAllTypes
            )
        }

        if(dec.type.asString() == Configuration.fillInToken)
            type.widget.background = configuration.fillInColor

        type.addDeleteEmptyListener {
            toAssign()
        }

        name = SimpleNameWidget(this, dec)
        name.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(dec.name, SimpleName(name.text), dec::setName)
        }
        name.addKeyEvent('=') {
            if(name.isEmpty) {
                editEvent(AssignExpr(
                    NameExpr(dec.typeAsString),
                    Configuration.hole(),
                    AssignExpr.Operator.ASSIGN
                ))
            }
            else {
                val hole = NameExpr(Configuration.fillInToken)
                hole.setParentNode(dec)
                dec.modifyCommand(
                    dec.initializer.getOrNull,
                    hole,
                    dec::setInitializer
                )
            }
        }
        name.addDeleteEmptyListener {
            if (dec.initializer.isPresent)
                editEvent(
                    AssignExpr(
                        dec.nameAsExpression,
                        dec.initializer.get(),
                        AssignExpr.Operator.ASSIGN
                    )
                )
            else
                editEvent(null)
        }

        if (dec.initializer.isPresent) {
            equals = FixedToken(this, "=")
            expression = createExpWidget(dec, dec.initializer.get())
        }

        observeNotNullProperty<Type>(ObservableProperty.TYPE, target = dec) {
            type.set(it.asString())
            type.widget.data = it
        }
        observeNotNullProperty<SimpleName>(
            ObservableProperty.NAME,
            target = dec
        ) {
            name.set(it.toString())
        }
        observeProperty<Expression>(
            ObservableProperty.INITIALIZER,
            target = dec
        ) {
            if (it == null) {
                equals?.dispose()
                equals = null
                expression?.dispose()
                expression = null
                requestLayout()
                name.setFocus()
            } else {
                expression?.dispose()
                if (equals == null)
                    equals = FixedToken(this, "=")
                expression = this.createExpWidget(dec, it)
                expression!!.requestLayout()
                expression!!.setFocusOnCreation()
            }
        }
    }

    private fun Composite.createExpWidget(
        variable: VariableDeclarator,
        expression: Expression
    ) = createExpressionWidget(this, expression) {
        variable.modifyCommand(
            variable.initializer.getOrNull,
            it,
            variable::setInitializer
        )
    }

    override val head: TextWidget
        get() = type

    override val tail: TextWidget
        get() = expression?.tail ?: name

    override fun setFocus(): Boolean {
        return type.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if (dec.type.asString() == Configuration.fillInToken)
            type.setFocus()
        else
            expression?.setFocus() ?: name.setFocus()
    }
}

object VariableDeclarationFeature :
    StatementFeature<ExpressionStmt, ExpressionStatementWidget>(
        ExpressionStmt::class.java,
        ExpressionStatementWidget::class.java
    ) {

    override fun targets(stmt: Statement): Boolean =
        stmt is ExpressionStmt && stmt.expression is VariableDeclarationExpr

    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {

        insert.addKeyEvent(
            SWT.SPACE,
            precondition = { insert.isAtEnd && isValidType(it) }) {
            val stmt = ExpressionStmt(
                VariableDeclarationExpr(
                    StaticJavaParser.parseType(insert.text),
                    Configuration.fillInToken
                )
            )
            output(stmt)
        }
//        insert.addKeyEvent(';', precondition = {
//            insert.isAtEnd && it.split(Regex("\\s+")).size == 2 && isValidType(
//                it.split(
//                    Regex("\\s+")
//                )[0]
//            ) && tryParse<NameExpr>(
//                it.split(Regex("\\s+"))[1]
//            )
//        }) {
//            val split = insert.text.split(Regex("\\s+"))
//            val stmt = ExpressionStmt(
//                VariableDeclarationExpr(
//                    StaticJavaParser.parseType(split[0]), split[1]
//                )
//            )
//            output(stmt)
//        }
//
//
//        insert.addKeyEvent('=', precondition = {
//            val parts = it.trim().split(Regex("\\s+"))
//            insert.isAtEnd && parts.size == 2 && isValidType(parts[0]) && tryParse<NameExpr>(
//                parts[1]
//            )
//        }) {
//            val split = insert.text.split(Regex("\\s+"))
//            val dec = VariableDeclarator(
//                StaticJavaParser.parseType(split[0]),
//                split[1],
//                NameExpr(Configuration.fillInToken)
//            )
//            val stmt = ExpressionStmt(VariableDeclarationExpr(dec))
//            output(stmt)
//        }
    }
}