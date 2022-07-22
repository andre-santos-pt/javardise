package javawidgets

import basewidgets.Constants
import basewidgets.FixedToken
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.api.row

class ReturnWidget(parent: SequenceWidget, override val node: ReturnStmt, override val block: BlockStmt) :
    StatementWidget<ReturnStmt>(parent) {
    lateinit var keyword: TokenWidget
    var exp: ExpWidget? = null
    lateinit var semiColon: FixedToken

    init {
        layout = FillLayout()
        row {
            keyword = Factory.newTokenWidget(this, "return")
            keyword.addDelete(node, block)
            Constants.addInsertLine(keyword)

            if (node.expression.isPresent)
                exp = createExpWidget(node.expression.get())
            semiColon = FixedToken(this, ";")
        }
        keyword.addKeyEvent(' ') {
            Commands.execute(object : Command {
                override fun run() {
                    node.setExpression(NameExpr("expression"))
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

    private fun Composite.createExpWidget(exp: Expression): ExpWidget {
        val w = ExpWidget(this, exp) {
            Commands.execute(object : Command {
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

    override fun setFocusOnCreation() {
        exp?.setFocus()
    }
}