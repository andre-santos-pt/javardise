package pt.iscte.javardise.widgets

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.ROW_LAYOUT_H_SHRINK
import pt.iscte.javardise.external.ROW_LAYOUT_H_STRING
import pt.iscte.javardise.external.ROW_LAYOUT_H_ZERO
import pt.iscte.javardise.external.row

fun createExpressionWidget(parent: Composite, node: Expression, editEvent: (Expression) -> Unit): ExpWidget<*> =
    when (node) {
        is BinaryExpr -> BiExp(parent, node)
        is CharLiteralExpr -> CharExp(parent, node)
        is StringLiteralExpr -> StringExp(parent, node)
        is MethodCallExpr -> CallExp(parent, node)
        else -> AnyExp(parent, node, editEvent)
    }

abstract class ExpWidget<T>(parent: Composite) : Composite(parent, SWT.NONE), NodeWidget<T>

class AnyExp(parent: Composite, override var node: Expression, val editEvent: (Expression) -> Unit) :
    ExpWidget<Expression>(parent) {

    var expression: TextWidget

    init {
        layout = ROW_LAYOUT_H_SHRINK
        val noparse = node is NameExpr && (node as NameExpr).name.asString() == Configuration.NOPARSE
        val text = if (noparse)
            if (node.comment.isPresent) node.comment.get().content else ""
        else
            node.toString()

        expression = TextWidget.create(this, text) { c, s ->
            c.toString().matches(Regex("[a-zA-Z\\d_\\[\\]()]")) || c == SWT.BS || c == SWT.SPACE
        }
        if(noparse)
            expression.widget.background = ERROR_COLOR()

        updateColor(expression)

        expression.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val find = binaryOperators.find { it.asString().startsWith(e.character) }
                if (find != null && expression.isAtEnd && tryParseExpression(expression.text)) {
                    node = BinaryExpr(StaticJavaParser.parseExpression(expression.text), NameExpr("expression"), find)
                    expression.removeFocusOutListeners()
                    editEvent(node)
                }
            }
        })
        expression.addKeyEvent('"') {
            editEvent(StringLiteralExpr(expression.text))
        }
        expression.addKeyEvent('\'', precondition = { it.isEmpty() }) {
            editEvent(CharLiteralExpr('a'))
        }
        expression.addKeyEvent('(', precondition = { tryParseSimpleName(it) }) {
            editEvent(MethodCallExpr(expression.text))
        }

        expression.addKeyEvent(SWT.BS, precondition = { it.isEmpty() && this.parent is BiExp }) {
            val pp = (this.parent as BiExp).parent
            if (pp is AnyExp) {
                val biexp = node.parentNode.get() as BinaryExpr
                val part = if (node === biexp.left) biexp.right else biexp.left
                pp.editEvent(part.clone())
            }
            //else
            // pp.editEvent(null)
        }
        expression.widget.addModifyListener {
            updateColor(expression)
        }

        expression.addFocusLostAction {
            if (tryParseExpression(expression.text)) {
                val newExp = StaticJavaParser.parseExpression<Expression>(expression.text)
                if (!newExp.equals(node)) {
                    node = newExp
                    editEvent(node)
                }
            } else {
                expression.widget.background = ERROR_COLOR()
                val noparse = NameExpr(Configuration.NOPARSE)
                noparse.setComment(BlockComment(expression.text))
                editEvent(noparse)
            }
        }
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }


}


class StringExp(parent: Composite, override val node: StringLiteralExpr) : ExpWidget<StringLiteralExpr>(parent) {
    val text: TextWidget
    init {
        layout = ROW_LAYOUT_H_STRING
        val open = FixedToken(this, "\"")
        open.label.foreground = COMMENT_COLOR()
        text = TextWidget.create(this, node.value) { _, _ -> true }
        text.widget.foreground = COMMENT_COLOR()
        val close = FixedToken(this, "\"")
        close.label.foreground = COMMENT_COLOR()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }
}

class CharExp(parent: Composite, override val node: CharLiteralExpr) : ExpWidget<CharLiteralExpr>(parent) {
    val text: TextWidget
    init {
        layout = ROW_LAYOUT_H_STRING
        val open = FixedToken(this, "'")
        open.label.foreground = COMMENT_COLOR()
        text = TextWidget.create(this, node.value) { _, s -> s.isEmpty() }
        text.widget.foreground = COMMENT_COLOR()
        val close = FixedToken(this, "'")
        close.label.foreground = COMMENT_COLOR()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        text.setFocus()
    }
}

class BiExp(parent: Composite, override val node: BinaryExpr) : ExpWidget<BinaryExpr>(parent) {

    var left: ExpWidget<*>
    var operator: TokenWidget
    var right: ExpWidget<*>
    val leftObserver: AstObserver
    val rightObserver: AstObserver
    val operatorObserver: AstObserver

    init {
        layout = ROW_LAYOUT_H_SHRINK
        operator = TokenWidget(this, node.operator.asString(),
            alternatives = { binaryOperators.map { it.asString() } }) {
            Commands.execute(object : Command {
                override val target: Node = node
                override val kind = CommandKind.MODIFY
                override val element = node.operator

                override fun run() {
                    node.operator = binaryOperators.find { op -> op.asString() == it }
                }

                override fun undo() {
                    node.operator = element
                }

            })

        }
        left = drawLeft(this, node.left)
        right = drawRight(this, node.right)
        leftObserver = node.observeProperty<Expression>(ObservableProperty.LEFT) {
            //if (!this.isDisposed) {
            left.dispose()
            drawLeft(this, it!!)
            //}
        }
        rightObserver = node.observeProperty<Expression>(ObservableProperty.RIGHT) {
            // if (!this.isDisposed) {
            right.dispose()

            drawRight(this, it!!)
            //
        }
        operatorObserver = node.observeProperty<BinaryExpr.Operator>(ObservableProperty.OPERATOR) {
            operator.set(it?.asString() ?: "??")
            operator.setFocus()
        }
    }


    override fun dispose() {
        super.dispose()
        node.unregister(leftObserver)
        node.unregister(rightObserver)
        node.unregister(operatorObserver)
    }

    private fun drawLeft(parent: Composite, expression: Expression): ExpWidget<*> {
        left = createExpressionWidget(parent, expression) {
            Commands.execute(object :
                ModifyCommand<Expression>(node, node.left) {
                val old = node.left.clone()
                override fun run() {
                    node.left = it
                }

                override fun undo() {
                    node.left = old
                }
            })
            left.dispose()
            drawLeft(parent, it)
        }
        left.moveAbove(operator.widget)
        left.requestLayout()
        left.setFocusOnCreation()
        return left
    }

    private fun drawRight(parent: Composite, expression: Expression): ExpWidget<*> {
        right = createExpressionWidget(parent, expression) {
            Commands.execute(object :
                ModifyCommand<Expression>(node, node.right) {
                val old = node.right.clone()
                override fun run() {
                    node.right = it
                }

                override fun undo() {
                    node.right = old
                }
            })
            right.dispose()
            drawRight(parent, it)
        }
        right.moveBelow(operator.widget)
        right.requestLayout()
        right.setFocusOnCreation()
        return right
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        right.setFocus()
    }


    override fun toString(): String {
        return "BiExp $node"
    }
}



class CallExp(parent: Composite, override val node: MethodCallExpr) :
    ExpWidget<MethodCallExpr>(parent) {
    lateinit var target: Id
    var methodName: Id
    lateinit var insert: TextWidget
    val openBracket: FixedToken

    val argumentWidgets = mutableListOf<ExpWidget<*>>()

    init {
        layout = ROW_LAYOUT_H_SHRINK
        if (node.scope.isPresent) {
            target = SimpleNameWidget(this, node.scope.get()) { it.toString() }
            target.addKeyEvent(SWT.BS, precondition = { it.isEmpty() }) {

            }  //action = createDeleteEvent(node, block))
            FixedToken(this, ".")
        }
        methodName = SimpleNameWidget(this, node.name) { it.asString() }
        openBracket = FixedToken(this, "(")

        node.arguments.forEach {
            createArgument(it)
        }
        if (node.arguments.isEmpty())
            createInsert(this)

        FixedToken(this, ")")

        methodName.addFocusLostAction {
            if (methodName.text.isEmpty())
                methodName.set(node.name.asString())
            else if (methodName.text != node.name.asString()) {
                Commands.execute(object : Command {
                    override val target = node
                    override val kind = CommandKind.MODIFY
                    override val element = node.name
                    override fun run() {
                        target.name = SimpleName(methodName.text)
                    }

                    override fun undo() {
                        target.name = element
                    }
                })
            }
        }
        registerObservers()
    }

    private fun registerObservers() {
        node.observeProperty<SimpleName>(ObservableProperty.NAME) {
            methodName.set(it?.asString() ?: node.name.asString())
        }

        node.arguments.observeList(object : ListObserver<Expression> {
            override fun elementAdd(list: NodeList<Expression>, index: Int, node: Expression) {
                createArgument(node).setFocus()
            }

            override fun elementRemove(list: NodeList<Expression>, index: Int, node: Expression) {
                val a = argumentWidgets.find { it.node === node }
                a?.let {
                    it.dispose()
                    argumentWidgets.remove(it)
                }
            }
        })
    }

    private fun createArgument(exp: Expression): ExpWidget<*> {
        val arg = createExpressionWidget(this, exp) {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element = exp

                override fun run() {
                    val index = node.arguments.indexOf(exp) // BUG
                    node.arguments[index] = it
                }

                override fun undo() {
                    val index = node.arguments.indexOf(it)
                    node.arguments[index] = element
                }
            })
        }
        // TODO cast..
        (arg as AnyExp).expression.addKeyEvent(',') {
            Commands.execute(object : Command {
                override val target = node
                override val kind = CommandKind.ADD
                override val element get() = NameExpr("expression")

                override fun run() {
                    node.arguments.addAfter(element, arg.node)
                }

                override fun undo() {
                    node.arguments.remove(element)
                }
            })
        }
        if (argumentWidgets.isEmpty()) {
            (arg as AnyExp).expression.moveBelowInternal(openBracket.label)
        } else {
            val comma = FixedToken(this, ",")
            comma.label.moveBelow(argumentWidgets.last())
            (arg as AnyExp).expression.moveBelowInternal(comma.label)
        }
        argumentWidgets.add(arg)
        requestLayout()
        return arg
    }

    override fun setFocus(): Boolean {
        return methodName.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        methodName.setFocus()
    }

    private fun createInsert(parent: Composite) {
        insert = TextWidget.create(parent, " ") { c, s ->
            c.toString().matches(TYPE_CHARS)
        }

        insert.addFocusLostAction {
            if (tryParseExpression(insert.text)) {
                doAddArgummentCommand(StaticJavaParser.parseExpression(insert.text))
                insert.delete()
            } else
                insert.clear()
        }
        insert.addKeyEvent(',', precondition = { tryParseExpression(it) }) {
            val insertExp = StaticJavaParser.parseExpression<Expression>(insert.text)
            doAddArgummentCommand(insertExp)
            doAddArgummentCommand(NameExpr("expression"), insertExp)
            insert.delete()
        }
        val listener = insert.addFocusLostAction {
            insert.widget.text = " "
        }

        insert.widget.addDisposeListener {
            insert.widget.removeFocusListener(listener)
        }
    }

    private fun doAddArgummentCommand(expression: Expression, after: Expression? = null) {
        Commands.execute(object : Command {
            override val target = node
            override val kind = CommandKind.ADD
            override val element = expression

            override fun run() {
                if (after != null)
                    target.arguments.addAfter(element, after)
                else
                    target.arguments.add(element)
            }

            override fun undo() {
                target.arguments.remove(element)
            }
        })
    }
}
