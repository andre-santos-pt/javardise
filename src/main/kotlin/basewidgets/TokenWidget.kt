package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.widgets.*
import java.util.*

// TODO Observable menu
class TokenWidget(
    parent: Composite,
    token: String,
    val alternatives: () -> List<String> = {emptyList()},
    val editAction: (String) -> Unit = {}
) : TextWidget {

    override val widget: Text = TextWidget.createText(parent, token)
    private val map: MultiMapList<Char, String> = MultiMapList()
    init {
        widget.editable = false
        widget.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == Constants.MENU_KEY) {
                    addMenu(alternatives())
                    menu.setLocation(widget.toDisplay(0, 20))
                    menu.isVisible = true
                } else if (map.containsKey(e.character)) {
                    val list = ArrayList(map[e.character])
                    val i = list.indexOf(widget.text)
                    val item = list[(i + 1) % list.size]
                    editAction(item)
                    //widget.text = item
                    //widget.requestLayout()
                    //widget.selectAll()
                }
            }
        })
    }

    private fun addMenu(alternatives: List<String>) {
        val menu = Menu(widget)
        for (t in alternatives) {
            val item = MenuItem(menu, SWT.NONE)
            item.text = t
            map.put(t[0], t)
            item.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    editAction(item.text)
                    //widget.text = item.text
                    //widget.requestLayout()
                    //widget.selectAll()
                    //widget.setFocus()
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