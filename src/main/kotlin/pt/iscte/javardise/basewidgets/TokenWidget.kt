package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Text

class TokenWidget(
    parent: Composite,
    token: String,
    val alternatives: () -> List<String> = { emptyList() },
    val editAction: (String) -> Unit = {}
) : TextWidget {

    override val widget: Text = TextWidget.createText(parent, token)
    private val map: MutableMap<Char, MutableList<String>> = mutableMapOf()

    fun MutableMap<Char, MutableList<String>>.putPair(c: Char, s: String) {
        if(containsKey(c))
            get(c)?.add(s)
        else
            put(c, mutableListOf(s))
    }

    init {
        widget.editable = false
        widget.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == SWT.CR) {
                    addMenu(alternatives())
                    menu.setLocation(widget.toDisplay(0, 20))
                    menu.isVisible = true
                } else if (map.containsKey(e.character)) {
                    val list = ArrayList(map[e.character])
                    val i = list.indexOf(widget.text)
                    val item = list[(i + 1) % list.size]
                    editAction(item)
                    e.doit = false
                }
            }
        })
        widget.data = this
    }

    private fun addMenu(alternatives: List<String>) {
        val menu = Menu(widget)
        for (t in alternatives) {
            val item = MenuItem(menu, SWT.NONE)
            item.text = t
            map.putPair(t[0], t)
            item.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    if(item.text != widget.text)
                        editAction(item.text)
                }
            })
        }
        widget.menu = menu
    }


    var menu: Menu
        get() = widget.menu
        set(menu) {
            widget.menu = menu
        }

    override var text: String
        get() = super.text
        set(value) {
            widget.text = value
            widget.requestLayout()

        }

    override fun toString(): String = text

    fun dispose() = widget.dispose()

    override fun setFocus() = widget.setFocus()

    override fun addKeyListenerInternal(listener: KeyListener) {
        widget.addKeyListener(listener)
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

    fun set(value: String) {
        widget.text = value
    }
}