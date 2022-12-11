package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.statements.ExpressionStatementWidget
import pt.iscte.javardise.widgets.statements.StatementFeature

class CallExpressionWidget(
    parent: Composite,
    override val node: MethodCallExpr,
    override val editEvent: (Expression?) -> Unit
) : ExpressionWidget<MethodCallExpr>(parent) {

    private var scope: ExpressionWidget<*>? = null
    private var dot: FixedToken? = null
    private var methodName: Id
    private val args: ArgumentListWidget<Expression, MethodCallExpr>

    init {
        layout = ROW_LAYOUT_H_CALL
        methodName = SimpleNameWidget(this, node)
        methodName.addFocusLostAction(::isValidSimpleName) {
            node.modifyCommand(
                node.name,
                SimpleName(methodName.text),
                node::setName
            )
        }
        methodName.addKeyEvent(
            SWT.BS,
            precondition = { methodName.isAtBeginning }) {
            node.modifyCommand(
                node.scope.getOrNull,
                null,
                node::setScope
            )
        }
        methodName.addKeyEvent(
            '.',
            precondition = { !node.scope.isPresent && !methodName.isAtBeginning && !methodName.isAtEnd }) {
            node.modifyCommand(
                null,
                NameExpr(methodName.text.substring(0, methodName.caretPosition)),
                node::setScope
            )
            node.modifyCommand(
                node.name,
                SimpleName(methodName.text.substring(methodName.caretPosition)),
                node::setName
            )
        }

        if (node.scope.isPresent)
            createScope(node.scope.get())

        args = ArgumentListWidget(this, "(", ")", this, node.arguments)

        node.observeProperty<Expression>(ObservableProperty.SCOPE) {
            scope?.dispose()
            scope = null
            dot?.dispose()
            dot = null
            if (it != null)
                createScope(it)
            setFocus()
            requestLayout()
        }
        node.observeNotNullProperty<SimpleName>(ObservableProperty.NAME)
        {
            methodName.set(it.asString())
        }
    }

    private fun createScope(e: Expression) {
        scope = createExpressionWidget(this, e) {
            node.modifyCommand(
                node.scope.get(),
                it,
                node::setScope
            )
        }
        scope!!.moveAbove(methodName.widget)
        dot = FixedToken(this, ".")
        dot!!.label.moveBelow(scope!!)
        dot!!.label.requestLayout()
    }

    override val tail: TextWidget
        get() = args.closeBracket

    override val head: TextWidget
        get() = scope?.head ?: methodName

    override fun setFocus(): Boolean {
        return scope?.setFocus() ?: methodName.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        args.setFocus()
    }
}


object CallFeature : StatementFeature<ExpressionStmt, CallStatementWidget>(
    ExpressionStmt::class.java,
    CallStatementWidget::class.java
) {

    override fun targets(stmt: Statement): Boolean =
        stmt is ExpressionStmt && stmt.expression is MethodCallExpr

    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {

        insert.addKeyEvent('(',
            precondition = {
                insert.isAtEnd &&
                        (tryParse<NameExpr>(it) || tryParse<FieldAccessExpr>(it) || tryParse<ArrayAccessExpr>(it))
            }) {
            var e: Expression = StaticJavaParser.parseExpression(insert.text)

            output(
                if (e is NameExpr) ExpressionStmt(
                    MethodCallExpr(
                        null,
                        e.name,
                        NodeList()
                    )
                )
                else ExpressionStmt(
                    MethodCallExpr(
                        (e as FieldAccessExpr).scope,
                        e.nameAsString,
                        NodeList()
                    )
                )
            )
        }
    }
}

class CallStatementWidget(
    parent: SequenceWidget,
    node: ExpressionStmt,
    parentBlock: BlockStmt ):
    ExpressionStatementWidget(parent, node, parentBlock) {

        init {
            expression.head.addKeyEvent('=', precondition = {expression.head.isAtBeginning}) {

                commandStack.execute(object : Command {
                    override val target: Node
                        get() = parentBlock
                    override val kind: CommandKind = CommandKind.MODIFY
                    override val element: Statement = node

                    var newNode: Node? = null

                    override fun run() {
                        newNode = ExpressionStmt(AssignExpr(
                            NameExpr(Configuration.fillInToken),
                            node.expression.clone(), AssignExpr.Operator.ASSIGN))
                        parentBlock.replace(node, newNode)
                    }

                    override fun undo() {
                        parentBlock.replace(newNode, element.clone())
                    }

                })
            }
        }
    }