package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.observeProperty
import pt.iscte.javardise.external.row
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget
import pt.iscte.javardise.widgets.members.addInsert

class ReturnWidget(parent: SequenceWidget, node: ReturnStmt, override val block: BlockStmt) :
    StatementWidget<ReturnStmt>(parent, node) {
    lateinit var keyword: TokenWidget
    var exp: ExpressionWidget<*>? = null
    lateinit var semiColon: TokenWidget
    val row: Composite

    var listener: AstObserver? = null

    init {
        layout = FillLayout()
        row = row {
            keyword = Factory.newKeywordWidget(this, "return")
            keyword.addDelete(node, block)
            keyword.setCopySource()
            keyword.setMoveSource()

            if (node.expression.isPresent) {
                exp = createExpressionWidget(this, node.expression.get()) {
                    Commands.execute(object :
                        ModifyCommand<Expression>(node, if (node.expression.isPresent) node.expression.get() else null) {
                        val old = if (node.expression.isPresent) node.expression.get() else null
                        override fun run() {
                            node.setExpression(it)
                        }

                        override fun undo() {
                            node.setExpression(old)
                        }
                    })
                }
            }
            semiColon = TokenWidget(this, ";")
            semiColon.addInsert(this@ReturnWidget, this@ReturnWidget.parent as SequenceWidget, true)
        }
        keyword.addKeyEvent(' ') {
            Commands.execute(object : AbstractCommand<Expression>(node, CommandKind.ADD, NameExpr("expression")) {
                override fun run() {
                    node.setExpression(element)
                }

                override fun undo() {
                    node.removeExpression()
                }
            })
        }

        listener = node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it != null && exp != null) {
                exp?.dispose()
                exp = createExpressionWidget(row, it) { e ->
                    Commands.execute(object :
                        ModifyCommand<Expression>(node, if (node.expression.isPresent) node.expression.get() else null) {
                        val old = if (node.expression.isPresent) node.expression.get() else null
                        override fun run() {
                            node.setExpression(e)
                        }

                        override fun undo() {
                            node.setExpression(old)
                        }
                    })
                }
                exp!!.moveAbove(semiColon.widget)
                exp!!.requestLayout()
                exp!!.setFocusOnCreation()
            }
        }
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }


    override fun setFocusOnCreation(firstFlag: Boolean) {
        exp?.setFocus()
    }
}