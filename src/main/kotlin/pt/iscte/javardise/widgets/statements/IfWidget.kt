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
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.*
import pt.iscte.javardise.CommandStack.NullStack.modifyCommand
import pt.iscte.javardise.basewidgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.expressions.ExpressionWidget
import pt.iscte.javardise.widgets.expressions.createExpressionWidget

class IfWidget(
    parent: SequenceWidget,
    node: IfStmt,
    override val parentBlock: BlockStmt
) :
    StatementWidget<IfStmt>(parent, node), SequenceContainer<IfStmt> {

    var column: Composite
    lateinit var firstRow: Composite
    lateinit var keyword: TokenWidget
    lateinit var condition: ExpressionWidget<*>
    override lateinit var bodyWidget: SequenceWidget
    override val body: BlockStmt = node.thenStmt.asBlockStmt()

    var elseWidget: ElseWidget? = null

    lateinit var openClause: FixedToken
    lateinit var openThenBracket: TokenWidget
    override val closingBracket: TokenWidget

    init {
        column = column {
            firstRow = row {
                keyword = newKeywordWidget(this, "if",
                    alternatives = {
                        if (node.hasElseBranch()) listOf() else listOf(
                            "while"
                        )
                    }) {

                    commandStack.execute(object : Command {
                        override val target: Node = parentBlock
                        override val kind: CommandKind = CommandKind.MODIFY
                        override val element: Statement = node

                        val whil = WhileStmt(
                            node.condition.clone(),
                            node.thenStmt.clone()
                        )

                        override fun run() {
                            node.replace(whil)
                        }

                        override fun undo() {
                            whil.replace(element)
                        }

                    })
                }
                keyword.addDelete(node, parentBlock)
                keyword.addShallowDelete()
                keyword.addEmptyStatement(this@IfWidget, parentBlock, node, false)
                keyword.setCopySource(node)
                openClause = FixedToken(this, "(")
                condition = this.createExpWidget(node.condition)
                FixedToken(this, ")")
            }

            bodyWidget = createSequence(this, node.thenBlock)

            openThenBracket = TokenWidget(firstRow, "{")
            openThenBracket.addEmptyStatement(this@IfWidget, node.thenBlock)
            //setThenBracketsVisibility(node.thenBlock.statements.size, openThenBracket, closeThenBracket)
        }
        closingBracket = TokenWidget(column, "}")
        closingBracket.addEmptyStatement(this, parentBlock, node)
        addMoveBracket() {
            !node.hasElseBranch()
        }


        node.thenBlock.statements.register(object : AstObserverAdapter() {
            override fun listChange(
                observedNode: NodeList<*>,
                type: AstObserver.ListChangeType,
                index: Int,
                nodeAddedOrRemoved: Node?
            ) {
                val newSize =
                    observedNode.size + if (type == AstObserver.ListChangeType.ADDITION) 1 else -1
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
            if (it == null)
                parentBlock.statements.removeCommand(
                    parentBlock.parentNode.get(),
                    node
                )
            else
                node.modifyCommand(node.condition, it, node::setCondition)
        }

    private fun setThenBracketsVisibility(
        bodySize: Int,
        open: TokenWidget,
        close: TokenWidget
    ) {
        val visible = bodySize == 0 || bodySize > 1
        open.widget.visible = visible
        close.widget.visible = visible
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation(firstFlag: Boolean) {
        condition.setFocus()
    }

    inner class ElseWidget(parent: Composite, elseStatement: Statement) :
        Composite(parent, SWT.NONE), SequenceContainer<IfStmt> {
        lateinit var openBracketElse: TokenWidget
        lateinit var elseBody: SequenceWidget
        lateinit var closeBracketElse: TokenWidget
        lateinit var keyword: TokenWidget

        override val body: BlockStmt get() = node.elseStmt.get().asBlockStmt()

        init {
            layout = ROW_LAYOUT_H_SHRINK
            background = parent.background
            foreground = parent.foreground
            font = parent.font
            column {
                row {
                    keyword = newKeywordWidget(this, "else")
                    keyword.addKeyEvent(SWT.BS) {
                        node.modifyCommand(
                            node.elseStmt.getOrNull,
                            null,
                            node::setElseStmt
                        )
                    }
                    keyword.addShallowDelete()
                    openBracketElse = TokenWidget(this, "{")
                }
                elseBody = createSequence(this, elseStatement as BlockStmt)
                closeBracketElse = TokenWidget(this, "}")
                closeBracketElse.addEmptyStatement(this@IfWidget, parentBlock, node)
                addMoveBracket() // TODO special case shallow delete ELSE
            }
            openBracketElse.addEmptyStatement(this@IfWidget, elseStatement as BlockStmt)
        }

        fun focusOpenBracket() {
            openBracketElse.setFocus()
        }

        override val bodyWidget: SequenceWidget
            get() = elseBody

        override val closingBracket: TextWidget
            get() = closeBracketElse

        override val node: IfStmt
            get() = this@IfWidget.node

        override fun setFocusOnCreation(firstFlag: Boolean) {
            focusOpenBracket()
        }

        override val control: Control
            get() = this
    }
}

object IfFeature : StatementFeature<IfStmt, IfWidget>(
    IfStmt::class.java,
    IfWidget::class.java
) {
    override fun configureInsert(
        insert: TextWidget,
        block: BlockStmt,
        node: Statement,
        commandStack: CommandStack,
        output: (Statement) -> Unit
    ) {
        insert.addKeyEvent(SWT.SPACE, '(', precondition = { it == "if" }) {
            output(
                IfStmt(
                    NameExpr(Configuration.fillInToken),
                    BlockStmt(),
                    null
                )
            )
        }
        insert.addKeyEvent(SWT.SPACE, '{', precondition = { it == "else" }) {
            val index = block.statements.indexOfIdentity(node)
            if (index > 0) {
                val prev = block.statements[index - 1]!!
                if (prev is IfStmt && !prev.hasElseBranch())
                    commandStack.modifyCommand(prev,
                        null,
                        BlockStmt(),
                        prev::setElseStmt
                    )
            }
        }
    }
}