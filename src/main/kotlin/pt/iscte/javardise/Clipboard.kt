package pt.iscte.javardise

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Display
import pt.iscte.javardise.basewidgets.TextWidget


object Clipboard {
    val swtClipboard = org.eclipse.swt.dnd.Clipboard(Display.getDefault())

    var onCopy: Pair<Node, (Node, Node, Int?) -> Unit>? = null
    // var cut: Boolean = false

    fun copy(node: Node, copy: (Node, Node, Int?) -> Unit) {
        onCopy = Pair(node, copy)
    }

//    fun cut(node: Node, copy: (Node,Node, Int?) -> Unit) {
//        copy(node, copy)
//        cut = true
//    }

//    fun paste(block: BlockStmt, index: Int) {
//        class AddStatementCommand(val stmt: Statement, val block: BlockStmt, val index: Int) : Command {
//            override val kind: CommandKind = CommandKind.ADD
//            override val target = block
//            override val element = stmt
//
//            override fun run() {
//                block.addStatement(index, stmt)
//            }
//
//            override fun undo() {
//                block.remove(stmt)
//            }
//        }
//        Commands.execute(AddStatementCommand(onCopy!!.first.clone() as Statement, block, index))
//    }

    var copyStatement: Node? = null
}

fun TextWidget.setCopySource(node: Node) {
    addKeyListenerInternal(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if ((e.stateMask == SWT.MOD1) && (e.character == 'c')) {
                if(node == Clipboard.copyStatement && node.parentNode.isPresent && node.parentNode.get() is BlockStmt) {
                    Clipboard.copyStatement = (node.parentNode.get() as Statement).clone()
//                    if(Clipboard.copyStatement is BlockStmt)
//                        (Clipboard.copyStatement as BlockStmt).statements.dropWhile { it is EmptyStmt }.dropLastWhile { it is EmptyStmt }
                }
                else
                    Clipboard.copyStatement = node.clone()
                println("copy ${Clipboard.copyStatement}")

                val textTransfer = TextTransfer.getInstance()
                Clipboard.swtClipboard.setContents(
                    arrayOf<Any>(node.toString()),
                    arrayOf<Transfer>(textTransfer)
                )
                e.doit = false
            }
        }
    })
}

fun TextWidget.setPasteTarget(clipboard: (Node) -> Unit) {
    addKeyListenerInternal(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if ((e.stateMask == SWT.MOD1) && (e.character == 'v')) {
                Clipboard.copyStatement?.let {
                    clipboard(it.clone())
                    println("paste $it")
                }
//                Clipboard.copy(node) { node, dest, index ->
//                    if (index != null) (dest as BlockStmt).statements.add(
//                        index,
//                        node as Statement
//                    )
//                }
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