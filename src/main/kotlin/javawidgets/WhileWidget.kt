package javawidgets

import basewidgets.Constants
import basewidgets.FixedToken
import basewidgets.SequenceWidget
import basewidgets.TokenWidget
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.eclipse.swt.layout.RowLayout
import pt.iscte.javardise.api.column
import pt.iscte.javardise.api.row

class WhileWidget(
    parent: SequenceWidget,
    override val node: WhileStmt,
    override val block: BlockStmt
) :
    StatementWidget<WhileStmt>(parent) {

    lateinit var keyword: TokenWidget
    lateinit var exp: ExpWidget
    lateinit var body: SequenceWidget

    init {
        layout = RowLayout()
        column {
            row {
                keyword = Factory.newTokenWidget(this, "while")
                FixedToken(this, "(")
                exp = ExpWidget(this, node.condition) {
                    Commands.execute(object : Command {
                        val old = node.condition
                        override fun run() {
                            node.condition = it
                        }

                        override fun undo() {
                            node.condition = old.clone()
                        }
                    })
                }
                FixedToken(this, ") {")
            }
            body = createSequence(this, node.block)
            val bodyClose = TokenWidget(this, "}")
            Constants.addInsertLine(bodyClose, true)
        }

        keyword.addDelete(node, block)

        node.register(object : AstObserverAdapter() {
            override fun propertyChange(
                observedNode: Node,
                property: ObservableProperty,
                oldValue: Any?,
                newValue: Any?
            ) {
                TODO()
                println(property.toString() + " " + newValue)
            }
        })
    }

    override fun setFocus(): Boolean = keyword.setFocus()

    override fun setFocusOnCreation() {
        exp.setFocus()
    }
}