package javawidgets.statements

import basewidgets.*
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import javawidgets.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.row

class ReturnWidget(parent: SequenceWidget, node: ReturnStmt, override val block: BlockStmt) :
    StatementWidget<ReturnStmt>(parent, node) {
    lateinit var keyword: TokenWidget
    var exp: ExpressionFreeWidget? = null
    lateinit var semiColon: FixedToken

    init {
        layout = FillLayout()
        row {
            keyword = Factory.newKeywordWidget(this, "return")
            keyword.addDelete(node, block)
            //Constants.addInsertLine(keyword, false)
            keyword.setCopySource()
            keyword.setMoveSource()

            if (node.expression.isPresent) {
                exp = createExpWidget(node.expression.get())
                exp!!.setMoveSource()
                exp!!.addKeyEvent(SWT.BS, precondition = { exp!!.isEmpty }, action = createDeleteEvent(node, block))
                Constants.addInsertLine(exp!!, true)
            }
            semiColon = FixedToken(this, ";")
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


        // BUG update exp not working visual
        node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
            if (it != null && exp == null) {
                exp = createExpWidget(it)
                exp!!.textWidget.moveAboveInternal(semiColon.label)
                requestLayout()
                exp!!.setFocus()
            }
            exp?.update(it!!)
        }
    }

    private fun Composite.createExpWidget(exp: Expression): ExpressionFreeWidget {
        val w = ExpressionFreeWidget(this, exp) {
            Commands.execute(object : ModifyCommand<Expression>(node, if (node.expression.isPresent) node.expression.get() else null) {
                val old = if (node.expression.isPresent) node.expression.get() else null
                override fun run() {
                    node.setExpression(it)
                }

                override fun undo() {
                    node.setExpression(old)
                }
            })
        }
        return w
    }

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }



    override fun setFocusOnCreation(firstFlag: Boolean) {
        exp?.setFocus()
    }
}