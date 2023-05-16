package pt.iscte.javardise.widgets.expressions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.parseFillIn

val TYPE = Regex("[a-zA-Z]\\w*")

class SimpleExpressionWidget(
    parent: Composite,
    override var node: Expression,
    override val editEvent: (Expression?) -> Unit
) :
    ExpressionWidget<Expression>(parent) {

    var expression: TextWidget
    val keyListener: KeyListener



    init {
        expression = TextWidget.create(this, nodeText(node)) { c, s ->
            c.toString()
                .matches(Regex("\\w")) ||
                    c == SWT.BS ||
                    c == SWT.SPACE && !s.endsWith(" ") ||
                    c == '.' && s.toIntOrNull() != null ||
                    c == '.' && s.isEmpty()

        }
        if (node.isNoParse)
            expression.widget.background = configuration.errorColor
        else if (node.isFillIn)
            expression.widget.background = configuration.fillInColor

        expression.widget.data = node

        addUpdateColor(expression.widget)

        keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (isDisposed)
                    return

                if (expression.isAtBeginning) {
                    if (e.character == SWT.BS)
                        editEvent(null)
                    else {
                        val unop = unaryOperators.filter { it.isPrefix }
                            .find { it.asString().startsWith(e.character) }
                        if (unop != null) {
                            if(tryParse<Expression>(expression.text)) {
                                node = UnaryExpr(
                                    StaticJavaParser.parseExpression(expression.text),
                                    unop
                                )
                            }
                            else
                                node = UnaryExpr(
                                    NameExpr(Configuration.fillInToken),
                                    unop
                                )
                            editEvent(node)
                        }
                    }
                } else if (expression.isAtEnd) {
                    val biop = binaryOperators.find {
                        it.asString().startsWith(e.character)
                    }
                    if (biop != null && tryParse<Expression>(expression.text)
                    ) {
                        // TODO var decl error
//                        at java.base/java.util.Optional.get(Optional.java:143)
//                        at pt.iscte.javardise.widgets.expressions.SimpleExpressionWidget$2.keyPressed(SimpleExpressionWidget.kt:92)
//                        at org.eclipse.swt.widgets.TypedListener.handleEvent(TypedListener.java:171)
                        if(node.parentNode.getOrNull is BinaryExpr && (node.parentNode.getOrNull as BinaryExpr).right === node) {
                            val parentExp = node.parentNode.get().clone() as BinaryExpr
                            parentExp.right = parseFillIn(expression.text)
                            val newnode = BinaryExpr(parentExp,
                                Configuration.hole(), biop
                            )
                            (parent as ExpressionWidget<Expression>).editEvent(newnode)
                        }
                        else {
                            node =
                                BinaryExpr(
                                    parseFillIn(expression.text), Configuration.hole(), biop
                                )
                            editEvent(node)
                        }
                    }
                } else {
                    val op = binaryOperators.find {
                        it.asString().startsWith(e.character)
                    }
                    op?.let {
                        val left = tail.text.slice(0 until tail.caretPosition)
                        val right =
                            tail.text.slice(tail.caretPosition until tail.text.length)
                        if (tryParse<Expression>(left) && tryParse<Expression>(right)
                        ) {
                            editEvent( // TODO new parse with hole
                                BinaryExpr(
                                    StaticJavaParser.parseExpression(left),
                                    StaticJavaParser.parseExpression(right),
                                    it
                                )
                            )
                        }
                    }
                }
            }
        }

        expression.addKeyListenerInternal(keyListener)

        expression.addKeyEvent('"') {
            editEvent(StringLiteralExpr(expression.text))
        }
        expression.addKeyEvent('\'', precondition = { it.isEmpty() || it.length == 1 }) {
            editEvent(CharLiteralExpr(if(expression.text.isEmpty()) ' ' else expression.text[0]))
        }
        expression.addKeyEvent('(',
            precondition = { expression.isAtEnd && isValidSimpleName(it) }) {
            editEvent(MethodCallExpr(expression.text))
        }

        expression.addKeyEvent('.',
            precondition = { expression.isAtEnd && (tryParse<NameExpr>(it) || tryParse<ThisExpr>(it)) }) {
            editEvent(FieldAccessExpr(StaticJavaParser.parseExpression(expression.text), NodeList(), Configuration.idHole()))
        }

        expression.addKeyEvent('[', precondition = {
            it.matches(Regex("new\\s+${TYPE.pattern}"))
        }) {
            val arrayCreationExpr = ArrayCreationExpr(
                StaticJavaParser.parseType(expression.text.split(Regex("\\s+"))[1]),
                NodeList.nodeList(ArrayCreationLevel(Configuration.hole())),
                null
            )
            editEvent(arrayCreationExpr)
        }

        expression.addKeyEvent('[', precondition = { expression.isAtEnd && tryParse<NameExpr>(it) }) {
            val arrayAccess = ArrayAccessExpr(
                StaticJavaParser.parseExpression(expression.text),
                NameExpr(Configuration.fillInToken)
            )
            editEvent(arrayAccess)
        }

        expression.addKeyEvent('(', precondition = {
            it.matches(Regex("new\\s+${TYPE.pattern}")) && isValidClassType(
                expression.text.split(Regex("\\s+"))[1]
            )

        }) {
            val objectCreationExpr = ObjectCreationExpr(
                null,
                StaticJavaParser.parseClassOrInterfaceType(
                    expression.text.split(
                        Regex("\\s+")
                    )[1]
                ),
                NodeList.nodeList()
            )
            editEvent(objectCreationExpr)
        }

        expression.addKeyEvent('(', precondition = {expression.isAtBeginning}) {
            editEvent(EnclosedExpr(parseFillIn(expression.text)))
        }

        expression.addKeyEvent(')', precondition = {expression.isAtEnd}) {
            editEvent(EnclosedExpr(parseFillIn(expression.text)))
        }

        expression.addKeyEvent('{', precondition={expression.isEmpty}) {
            editEvent(ArrayInitializerExpr())
        }

        expression.addKeyEvent(SWT.CR) {
            expression.widget.traverse(SWT.TRAVERSE_TAB_NEXT)
        }

        expression.addFocusLostAction {
            if(!expression.widget.isDisposed) {
                val new = parseFillIn(expression.text)
                if(new != node) {
                    node = new
                    expression.widget.data = node
                    editEvent(node)
                }
            }
        }
    }



    override val head: TextWidget
        get() = expression

    override val tail: TextWidget
        get() = expression

    override fun setFocusOnCreation(firstFlag: Boolean) {
        expression.setFocus()
    }
}