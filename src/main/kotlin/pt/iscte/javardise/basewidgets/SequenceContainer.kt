package pt.iscte.javardise.basewidgets

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.UnsupportedWidget
import pt.iscte.javardise.addCommand
import pt.iscte.javardise.external.ListAddRemoveObserver
import pt.iscte.javardise.widgets.statements.IfWidget
import pt.iscte.javardise.widgets.statements.find
import pt.iscte.javardise.widgets.statements.findByModelIndex
import pt.iscte.javardise.widgets.statements.findIndexByModel


interface SequenceContainer<T : Node> : NodeWidget<T>{
    val body: SequenceWidget?
    fun setFocus(): Boolean

    fun focusFirst() {
        body?.setFocus()
    }

    fun focusLast() {
        if(body?.children?.isNotEmpty() == true) {
            val last = body!!.children.last()
            if(last is SequenceContainer<*>)
                if(last is IfWidget && last.elseWidget != null)
                    last.elseWidget?.closingBracket?.setFocus()
                else
                    last.closingBracket.setFocus()
            else
                last.setFocus()
        }
        else
            setFocus()
    }

    val closingBracket: TextWidget

    fun addWidget(
        stmt: Statement,
        block: BlockStmt,
        parent: SequenceWidget
    ): NodeWidget<*> {
        val statementFeature =
            configuration.statementFeatures.find { it.targets(stmt) }
        if (statementFeature != null)
            return statementFeature.create(parent, stmt, block)
        else
            return UnsupportedWidget(parent, stmt)
//        throw UnsupportedOperationException("NA $stmt ${stmt::class}")
    }

    fun createSequence(parent: Composite, block: BlockStmt): SequenceWidget {
        val seq = SequenceWidget(parent, configuration.tabLength) { w, e ->
            createInsert(w, block)
        }
        populateSequence(seq, block)
        return seq
    }

    private fun populateSequence(seq: SequenceWidget, block: BlockStmt) {
        block.statements.forEach {
            addWidget(it, block, seq)
        }
        block.statements.register(object : ListAddRemoveObserver<Statement>() {
            override fun elementAdd(
                list: NodeList<Statement>,
                index: Int,
                node: Statement
            ) {
                val prev = seq.findByModelIndex(index) as? Control
                val w = addWidget(node, block, seq)
                if (prev != null) (w as Composite).moveAbove(prev) // bug with comments?
                seq.requestLayout()
                w.setFocusOnCreation()
            }

            override fun elementRemove(
                list: NodeList<Statement>,
                index: Int,
                node: Statement
            ) {
                val control = seq.find(node) as? Control
                val index = seq.children.indexOf(control)

                control?.dispose()
                seq.requestLayout()

                val childrenLen = seq.children.size
                if (index < childrenLen) seq.children[index].setFocus()
                else if (index - 1 in 0 until childrenLen) seq.children[index - 1].setFocus()
                else seq.parent.setFocus()
            }

            override fun elementReplace(
                list: NodeList<Statement>,
                index: Int,
                old: Statement,
                new: Statement
            ) {
                TODO("Not yet implemented")
            }
        })
    }

    fun createInsert(seq: SequenceWidget, block: BlockStmt): TextWidget {
        val insert = TextWidget.create(seq) { c, s ->
            c.toString().matches(Regex("\\w|\\[|]|\\.|\\+|-|\\*|/|%"))
                    || c == SWT.SPACE && !s.endsWith(SWT.SPACE)
                    || c == SWT.BS
        }

//    val insert = EmptyStatement(seq, EmptyStmt(), block) { c, s ->
//                c.toString().matches(Regex("\\w|\\[|]|\\.|\\+|-|\\*|/|%"))
//                || c == SWT.SPACE && !s.endsWith(SWT.SPACE)
//                || c == SWT.BS
//    }

        fun insert(stmt: Statement) {
            val insertIndex = seq.findIndexByModel(insert.widget)
            insert.delete()
            block.statements.addCommand(block.parentNode.get(), stmt, insertIndex)
        }

        configuration.statementFeatures.forEach {
            it.configureInsert(insert, ::insert)
        }

        insert.addFocusLostAction {
            insert.clear()
        }

//    insert.addKeyListenerInternal(object : KeyAdapter() {
//        override fun keyPressed(e: KeyEvent) {
//            if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'v'.code)) {
//                Clipboard.paste(block, seq.findIndexByModel(insert.widget))
//            }
//        }
//    })
        return insert
    }
}