package pt.iscte.javardise.widgets.statements

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Factory
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.removeCommand
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class IfWidget(
    parent: SequenceWidget,
    node: IfStmt,
    override val block: BlockStmt
) :
    StatementWidget<IfStmt>(parent, node), SequenceContainer {

    var column: Composite
    lateinit var firstRow: Composite
    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    override lateinit var body: SequenceWidget

    var elseWidget: ElseWidget? = null

    lateinit var openClause: FixedToken
    lateinit var openThenBracket: TokenWidget
    override val closingBracket: TokenWidget

    init {
        column = column {
            firstRow = row {
                keyword = Factory.newKeywordWidget(this, "if")
                keyword.addDelete(node, block)
                //keyword.setCopySource(node)
                openClause = FixedToken(this, "(")
                condition = this.createExpWidget(node.condition)
                FixedToken(this, ")")
            }

            body = createSequence(this, node.thenBlock)

            openThenBracket = TokenWidget(firstRow, "{")
            openThenBracket.addInsert(null, body, false)

            //setThenBracketsVisibility(node.thenBlock.statements.size, openThenBracket, closeThenBracket)
        }
        closingBracket = TokenWidget(column, "}")
        closingBracket.addInsert(this@IfWidget, this@IfWidget.parent as SequenceWidget, true)


        node.thenBlock.statements.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node?
            ) {
                val newSize = observedNode.size + if (type == AstObserver.ListChangeType.ADDITION) 1 else -1
                //setThenBracketsVisibility(newSize, openThenBracket, closeThenBracket)
            }
        })

        node.observeProperty<Expression>(ObservableProperty.CONDITION) {
            condition.dispose()
            condition = firstRow.createExpWidget(it!!)
            condition.moveBelow(openClause.label)
            condition.requestLayout()
            condition.setFocusOnCreation()
        }

        if (node.hasElseBranch())
            elseWidget = ElseWidget(column, node.elseBlock)

        node.observeProperty<Statement>(ObservableProperty.ELSE_STMT) {
            if (it == null) {
                elseWidget?.dispose()
                requestLayout()
                keyword.setFocus()
            } else {
                elseWidget = ElseWidget(column, it)
                elseWidget?.focusOpenBracket()
                elseWidget?.requestLayout()
            }
        }
    }

    private fun Composite.createExpWidget(condition: Expression) =
        createExpressionWidget(this, condition) {
            if(it == null)
                block.statements.removeCommand(block.parentNode.get(), node)
            else
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    private fun setThenBracketsVisibility(bodySize: Int, open: TokenWidget, close: TokenWidget) {
        val visible = bodySize == 0 || bodySize > 1
        open.widget.visible = visible
        close.widget.visible = visible
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        condition.setFocus()
    }

    inner class ElseWidget(parent: Composite, elseStatement: Statement) : Composite(parent, SWT.NONE), SequenceContainer {
        lateinit var openBracketElse: TokenWidget
        lateinit var elseBody: SequenceWidget
        lateinit var closeBracketElse: TokenWidget
        lateinit var keyword: TokenWidget

        init {
            layout = ROW_LAYOUT_H_SHRINK
            font = parent.font
            column {
                row {
                    keyword = Factory.newKeywordWidget(this, "else")
                    keyword.addKeyEvent(SWT.BS) {
                        node.modifyCommand(node.elseStmt.getOrNull, null, node::setElseStmt)
                    }
                    openBracketElse = TokenWidget(this, "{")
                }
                elseBody = createSequence(this, elseStatement as BlockStmt)
                closeBracketElse = TokenWidget(this, "}")
                closeBracketElse.addInsert(this@IfWidget, this@IfWidget.parent as SequenceWidget, true)
            }
            openBracketElse.addInsert(null, elseBody, false)
        }

        fun focusOpenBracket() {
            openBracketElse.setFocus()
        }

        override val body: SequenceWidget
            get() = elseBody

        override val closingBracket: TextWidget
            get() = closeBracketElse
    }
}

class IfFeature : StatementFeature<IfStmt, IfWidget>(IfStmt::class.java, IfWidget::class.java) {
    override fun configureInsert(
        insert: TextWidget,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '(', precondition = { it == "if" }) {
            output(IfStmt(
                NameExpr("condition"),
                BlockStmt(),
                null
            ))
        }
        insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
            val seq = insert.widget.parent
            val seqIndex = seq.children.indexOf(insert.widget)
            if (seqIndex > 0) {
                val prev = seq.children[seqIndex - 1]
                if (prev is IfWidget && !prev.node.hasElseBranch()) {
                    insert.delete()
                    prev.node.modifyCommand(
                        null,
                        BlockStmt(),
                        prev.node::setElseStmt
                    )
                }
            }
        }
    }
}