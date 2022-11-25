package pt.iscte.javardise.basewidgets

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import pt.iscte.javardise.*
import pt.iscte.javardise.external.ListAddRemoveObserver
import pt.iscte.javardise.widgets.statements.IfWidget
import pt.iscte.javardise.widgets.statements.find
import pt.iscte.javardise.widgets.statements.findByModelIndex
import pt.iscte.javardise.widgets.statements.findIndexByModel


interface SequenceContainer<T : Node> : NodeWidget<T>{

    val body: BlockStmt?
    val bodyWidget: SequenceWidget?
    fun setFocus(): Boolean

    fun focusFirst() {
        bodyWidget?.setFocus()
    }

    fun focusLast() {
        if(bodyWidget?.children?.isNotEmpty() == true) {
            val last = bodyWidget!!.children.last()
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


    fun addMoveBracket(precondition: () -> Boolean = {true}) {
        require(node is Statement)

        closingBracket.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.isCombinedKey(SWT.ARROW_DOWN) && precondition()) {
                    require(body != null)
                    val parentBlock = node.parentNode.get() as BlockStmt
                    val i = parentBlock.statements.indexOf(node as Statement)
                    if (i + 1 < parentBlock.statements.size) {
                        commandStack.execute(object : Command {
                            override val target: Node
                                get() = node
                            override val kind: CommandKind
                                get() = CommandKind.MOVE
                            override val element: Statement
                                get() = parentBlock.statements[i + 1]

                            override fun run() {
                                body!!.asBlockStmt().statements.add(element.clone())
                                element.remove()
                            }

                            override fun undo() {
                                TODO("undo")
                            }

                        })
                        closingBracket.setFocus()
                    }

                } else if (e.isCombinedKey(SWT.ARROW_UP) && precondition()) {
                    require(body != null)
                    val parentBlock = node.parentNode.get() as BlockStmt
                    if (!body!!.asBlockStmt().isEmpty) {
                        commandStack.execute(object : Command {
                            override val target: Node
                                get() = node
                            override val kind: CommandKind
                                get() = CommandKind.MOVE
                            override val element: Statement
                                get() = body!!.statements.last()

                            override fun run() {
                                parentBlock.statements.addAfter(
                                    element.clone(),
                                    node as Statement
                                )
                                element.remove()
                            }

                            override fun undo() {
                                TODO("undo")
                            }

                        })
                        closingBracket.setFocus()
                    }
                }
            }
        })
    }

    fun TokenWidget.addShallowDelete() {
        require(node is Statement)
        addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == SWT.DEL) {
                    require(body != null)
                    val parentBlock = node.parentNode.get() as BlockStmt
                    commandStack.execute(object : Command {
                        override val target: Node
                            get() = parentBlock
                        override val kind: CommandKind
                            get() = CommandKind.REMOVE
                        override val element: Statement
                            get() = node as Statement

                        override fun run() {
                            body!!.statements.forEach {
                                parentBlock.statements.addAfter(it.clone(), element)
                            }
                            element.remove()
                        }

                        override fun undo() {
                            TODO("undo")
                        }

                    })
                }
            }
        })
    }



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