package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.basewidgets.Constants
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.SequenceWidget
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.widgets.*
import pt.iscte.javardise.external.*

class IfWidget(
    parent: SequenceWidget,
    node: IfStmt,
    override val block: BlockStmt
) :
    StatementWidget<IfStmt>(parent, node) {

    lateinit var column: Composite
    lateinit var exp: ExpressionFreeWidget
    lateinit var thenBody: SequenceWidget

    var elseWidget: ElseWidget? = null
    var elseBody: SequenceWidget? = null

    lateinit var openThenBracket : TokenWidget
    lateinit var closeThenBracket : TokenWidget

    init {
        layout = RowLayout()
        column = column {
            val firstRow = row {
                val keyword = Factory.newKeywordWidget(this, "if")
                keyword.setCopySource()
                //Constants.addInsertLine(keyword)
                keyword.addDelete(node, block)
                FixedToken(this, "(")
                exp = ExpressionFreeWidget(this, node.condition) {
                    Commands.execute(object : AbstractCommand<Expression>(node, CommandKind.MODIFY, node.condition) {
                        override fun run() {
                            node.condition = it
                        }

                        override fun undo() {
                            node.condition = element.clone()
                        }
                    })
                }
                exp.addKeyEvent(SWT.CR) {
                    thenBody.insertLine()
                }
                Constants.addInsertLine(exp,)
                FixedToken(this, ")")

            }

            thenBody = createSequence(this, node.thenBlock)

            openThenBracket = TokenWidget(firstRow, "{")
            openThenBracket.addInsert(null, thenBody, false)
            closeThenBracket = TokenWidget(this, "}")
            closeThenBracket.addInsert(this@IfWidget, this@IfWidget.parent as SequenceWidget, true)

            //setThenBracketsVisibility(node.thenBlock.statements.size, openThenBracket, closeThenBracket)
        }

        // TODO else brackets visibility
        node.thenBlock.statements.register(object: AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node?
            ) {
                val newSize = observedNode.size + if(type == AstObserver.ListChangeType.ADDITION) 1 else -1
                //setThenBracketsVisibility(newSize, openThenBracket, closeThenBracket)
            }
        })

        node.observeProperty<Expression>(ObservableProperty.EXPRESSION) {
           exp.update(it)
        }

        if (node.hasElseBranch())
            elseWidget = ElseWidget(column, node.elseBlock)

        node.observeProperty<Statement>(ObservableProperty.ELSE_STMT) {
            if (it == null)
                elseWidget?.dispose()
            else
                elseWidget = ElseWidget(column, it)

            elseWidget?.requestLayout()
        }
    }

    private fun setThenBracketsVisibility(bodySize: Int, open: TokenWidget, close: TokenWidget) {
        val visible = bodySize == 0 || bodySize > 1
        open.widget.visible = visible
        close.widget.visible = visible
    }

    inner class ElseWidget(parent: Composite, elseStatement: Statement) : Composite(parent, SWT.NONE){
        init {
            layout = FillLayout()
            column {
                row {
                    val keyword = Factory.newKeywordWidget(this, "else")
                    keyword.addKeyEvent(SWT.BS) {
                        Commands.execute(object : AbstractCommand<Statement>(node, CommandKind.REMOVE, elseStatement) {
                            override fun run() {
                                node.removeElseStmt()
                            }

                            override fun undo() {
                                node.setElseStmt(elseStatement.clone())
                            }
                        })
                    }

                    FixedToken(this, "{")
                }
                elseBody = createSequence(this, elseStatement as BlockStmt)
                FixedToken(this, "}")
            }
        }
    }

    override fun setFocus(): Boolean = exp.setFocus()
    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}