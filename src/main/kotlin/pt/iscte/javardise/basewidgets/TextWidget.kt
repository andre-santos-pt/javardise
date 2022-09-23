package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.CODE_FONT
import pt.iscte.javardise.Configuration

interface TextWidget {
    val widget: Text
    val textToSerialize: String
        get() {
            return widget.text
        }

    var text: String
        get() = widget.text
        set(value) {
            widget.text = value
        }


    val statement: Control
        get() {
            var c: Control = widget
            while (!(c.data == "ROOTAREA" || c.parent is SequenceWidget)) c = c.parent
            return c
        }

    val sequenceContainer: SequenceContainer
        get() {
            var c: Control = widget.parent
            while (c !is SequenceContainer) {
                c = c.parent
            }
            return c
        }

    val isEmpty: Boolean
        get() = widget.text.isBlank()

    val caretPosition: Int
        get() = widget.caretPosition

    val selectionCount: Int
        get() = widget.selectionCount

    val isAtBeginning: Boolean
        get() = widget.caretPosition == 0 && widget.selectionCount == 0

    val isAtEnd: Boolean
        get() = widget.caretPosition == widget.text.length

    val isSelected: Boolean
        get() = widget.selectionCount == widget.text.length

    val isModifiable: Boolean
        get() = widget.editable


    fun delete() = widget.dispose()
    fun setAtLeft() = widget.setSelection(0, 0)
    fun setAtRight() = widget.setSelection(widget.text.length)
    fun setFocus() = widget.setFocus()

    fun moveBelowInternal(control: Control) = widget.moveBelow(control)
    fun moveAboveInternal(control: Control) = widget.moveAbove(control)
    fun layoutInternal() = widget.requestLayout()

    fun setToolTip(text: String) {
        widget.toolTipText = text
    }

    fun clear() {
        widget.text = ""
    }

    fun addKeyEvent(vararg chars: Char, precondition: (String) -> Boolean = {true}, action: (KeyEvent) -> Unit): KeyListener {
        val l = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (!widget.isDisposed && precondition(widget.text) && chars.contains(e.character)) {
                    action(e)
                    e.doit = false
                }
            }
        }
        widget.addKeyListener(l)
        widget.toolTipText += chars.joinToString() + " do something\n"
        return l
    }

    fun addKeyListenerInternal(listener: KeyListener)

    fun addFocusListenerInternal(listener: FocusListener)

    fun addFocusLostAction(action: () -> Unit) : FocusListener

    fun addDeleteListener(action: () -> Unit) =
            addKeyEvent(Constants.DEL_KEY) {
                if (it.widget is Label) action()
                else if (it.widget is Text) {
                    val t = it.widget as Text
                        if (t.text.isEmpty() && t.caretPosition == 0 || !t.editable) action()
                }
            }

    fun setInsertLineMode() {
        Constants.addInsertLine(this)
    }

    companion object {

        @JvmStatic
        fun createText(
            parent: Composite,
            text: String,
            accept: ((Char, String) -> Boolean)? = null
        ): Text {
            val t = Text(parent, SWT.NONE)
            t.background = parent.background
            t.foreground = parent.foreground
            t.text = text
            t.font = CODE_FONT()
            t.cursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);

            accept?.let {
                t.addVerifyListener {
                    it.doit = accept(it.character, t.text)
                }
            }

            t.addTraverseListener { e ->
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
                    e.doit = true
            }
            t.addFocusListener(FOCUS_SELECTALL)
            t.addMouseTrackListener(MOUSE_FOCUS)
            t.addModifyListener(MODIFY_PACK)
            t.addKeyListener(LISTENER_ARROW_KEYS)  // BUGS
            //t.data = parent

            return t
        }


        @JvmStatic
        fun create(parent: Composite,
                   text: String = "",
                   accept: ((Char, String) -> Boolean) = { _: Char, _: String -> false }): TextWidget {
            return object : TextWidget {

                var acceptFlag = false

                val w: Text = createText(parent, text) {
                    c, s -> acceptFlag || accept(c, s)
                }.apply {
                    background = parent.background
                    foreground = parent.foreground
                }

                override val widget: Text
                    get() = w

                override fun clear() {
                    acceptFlag = true
                    w.text = ""
                    acceptFlag = false
                }

                override var text: String
                    get() = super.text
                    set(value) {
                        acceptFlag = true
                        w.text = value
                        acceptFlag = false
                    }

                override fun addKeyListenerInternal(listener: KeyListener) {
                    widget.addKeyListener(listener)
                }

                override fun addFocusListenerInternal(listener: FocusListener) {
                    widget.addFocusListener(listener)
                }

                override fun addFocusLostAction(action: () -> Unit): FocusListener {
                    val listener = object : FocusAdapter() {
                        override fun focusLost(e: FocusEvent?) {
                            action()
                        }
                    }
                    widget.addFocusListener(listener)
                    return listener
                }
            }
        }

        @JvmStatic
        fun findStatement(widget: Control): Control {
            var c: Control = widget
            while (!(c.data == "ROOTAREA" || c.parent is SequenceWidget)) c = c.parent
            return c
        }

        private val MODIFY_PACK =
                ModifyListener { e -> //			((Control) e.widget).setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
                    (e.widget as Control).pack()
                    (e.widget as Control).requestLayout()
                }

        private val LISTENER_ARROW_KEYS: KeyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val focusControl = Display.getDefault().focusControl
                if (focusControl != null && focusControl.parent is TextWidget) {
                    val w = focusControl.parent as TextWidget
                    val text = w.widget
                    if (e.keyCode == SWT.ARROW_RIGHT && (!text.editable || w.isAtEnd)) {
                        text.traverse(SWT.TRAVERSE_TAB_NEXT)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_LEFT && (!text.editable || w.isAtBeginning)) {
                        text.traverse(SWT.TRAVERSE_TAB_PREVIOUS)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_UP) {
                        moveCursorUp(w)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_DOWN) {
                        moveCursorDown(w)
                        e.doit = false
                    }
                } else if (focusControl is Text && !focusControl.editable) {
                    if (e.keyCode == SWT.ARROW_RIGHT) {
                        focusControl.traverse(SWT.TRAVERSE_TAB_NEXT)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_LEFT) {
                        focusControl.traverse(SWT.TRAVERSE_TAB_PREVIOUS)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_UP) {
                        moveCursorUp(focusControl)
                        e.doit = false
                    } else if (e.keyCode == SWT.ARROW_DOWN) {
                        moveCursorDown(focusControl)
                        e.doit = false
                    }
                }
            }
        }

        private val FOCUS_SELECTALL: FocusListener = object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                (e.widget as Text).selectAll()
            }
        }

        private val MOUSE_FOCUS : MouseTrackListener = object : MouseTrackAdapter() {
            override fun mouseEnter(e: MouseEvent) {
                if (Configuration.focusFollowsMouse) {
                    val c =  e.widget as Control
                    c.foreground = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW)
                    c.requestLayout()
                }
            }

            override fun mouseExit(e: MouseEvent) {
                val c =  e.widget as Control
                if (Configuration.focusFollowsMouse) {
                    c.foreground = Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
                    c.requestLayout()
                }
            }
        }

        fun moveCursorUp(widget: TextWidget) {
            val statement = widget.statement
            if (statement.data != "ROOTAREA") {
                val seq = statement.parent as SequenceWidget
                seq.focusPreviousStatement(statement)
            }
        }

        fun moveCursorUp(widget: Control) {
            val statement = findStatement(widget)
            if (statement.data != "ROOTAREA") {
                val seq = statement.parent as SequenceWidget
                seq.focusPreviousStatement(statement)
            }
        }

        fun moveCursorDown(widget: TextWidget) {
            val statement = widget.statement
            if (statement.data != "ROOTAREA") {
                val seq = statement.parent as SequenceWidget
                seq.focusNextStatement(widget.statement)
            }
        }

        fun moveCursorDown(widget: Control) {
            val statement = findStatement(widget)
            if (statement.data != "ROOTAREA") {
                val seq = statement.parent as SequenceWidget
                seq.focusNextStatement(statement)
            }
        }

    }
}