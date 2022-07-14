package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.widgets.*
import java.util.*

class TokenWidget(
    parent: Composite,
    token: String,
    alternatives: List<String> = emptyList()
) : TextWidget {

    override val widget: Text = TextWidget.createText(parent, token) { _, _ -> false }
    private val map: MultiMapList<Char, String> = MultiMapList()

    init {
        widget.editable = false
        addMenu(alternatives)
    }

    private fun addMenu(alternatives: List<String>) {
        val menu = Menu(widget)
        for (t in alternatives) {
            val item = MenuItem(menu, SWT.NONE)
            item.text = t
            map.put(t[0], t)
            item.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    widget.text = item.text
                    widget.requestLayout()
                    widget.selectAll()
                    widget.setFocus()
                }
            })
        }
        MenuItem(menu, SWT.SEPARATOR)
        widget.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == Constants.MENU_KEY) {
                    menu.setLocation(widget.toDisplay(0, 20))
                    menu.isVisible = true
                } else if (map.containsKey(e.character)) {
                    val list = ArrayList(map[e.character])
                    val i = list.indexOf(widget.text)
                    val item = list[(i + 1) % list.size]
                    widget.text = item
                    widget.requestLayout()
                    widget.selectAll()
                }
            }
        })
        widget.menu = menu
    }

    var menu: Menu
        get() = widget.menu
        set(menu) {
            widget.menu = menu
        }

    fun setVisible(visible: Boolean) {
        widget.isVisible = visible
    }

    override fun toString(): String = text

    fun isKeyword(keyword: String) = text == keyword

    fun setLayoutData(data: RowData) {
        widget.layoutData = data
    }

    fun dispose() = widget.dispose()

    override fun setFocus() = widget.setFocus()

    override fun addKeyListenerInternal(listener: KeyListener) {
        widget.addKeyListener(listener)
    }

    override fun addFocusListenerInternal(listener: FocusListener) {
        widget.addFocusListener(listener)
    }

}