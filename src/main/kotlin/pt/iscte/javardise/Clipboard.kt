package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import pt.iscte.javardise.basewidgets.TextWidget

object Clipboard {
    var onCopy: Pair<Node, (Node, Node, Int?) -> Unit>? = null
    // var cut: Boolean = false

    fun copy(node: Node, copy: (Node, Node, Int?) -> Unit) {
        onCopy = Pair(node, copy)
    }

//    fun cut(node: Node, copy: (Node,Node, Int?) -> Unit) {
//        copy(node, copy)
//        cut = true
//    }

    fun paste(block: BlockStmt, index: Int) {
        class AddStatementCommand(val stmt: Statement, val block: BlockStmt, val index: Int) : Command {
            override val kind: CommandKind = CommandKind.ADD
            override val target = block
            override val element = stmt

            override fun run() {
                block.addStatement(index, stmt)
            }

            override fun undo() {
                block.remove(stmt)
            }
        }
        Commands.execute(AddStatementCommand(onCopy!!.first.clone() as Statement, block, index))
    }
}

fun TextWidget.setCopySource(node: Statement) {
    addKeyListenerInternal(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'c'.code)) {
                Clipboard.copy(node) { node, dest, index ->
                    if (index != null) (dest as BlockStmt).statements.add(
                        index,
                        node as Statement
                    )
                }
            }
//                else if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'x'.code)) {
//                    Clipboard.cut(node) { node, dest, index ->
//                        if (index != null)
//                            (dest as BlockStmt).statements.add(index, node as Statement)
//                    }
//                }
        }
    })
}

fun TextWidget.setMoveSource(stmt: Statement, block: BlockStmt) {
    addKeyListenerInternal(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if ((e.stateMask == SWT.ALT) && (e.keyCode == SWT.ARROW_UP)) {
                val index = block.statements.indexOf(stmt)
                if (index == 0) {
                    if (block.parentNode.isPresent) println(block.parentNode.get())
                } else {
                    block.statements.remove(stmt)
                    block.statements.add(index - 1, stmt)
                }
                e.doit = false
            } else if ((e.stateMask == SWT.ALT) && (e.keyCode == SWT.ARROW_DOWN)) {
                val index = block.statements.indexOf(stmt)
                if (index == block.statements.lastIndex) {


                } else {
                    block.statements.remove(stmt)
                    block.statements.add(index + 1, stmt)
                }
                e.doit = false
            }
        }
    })
}