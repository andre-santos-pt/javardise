package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Text

import java.util.function.Supplier

class Id(parent: Composite, id: String) :
    EditorWidget(parent), TextWidget {
    private var read_only: Boolean
    private var skipVerify = false
    private val textWidget: Text
    private var editAction = {  -> Unit}
    private var allowEmpty = Supplier { false }

    init {
        layout = Constants.ROW_LAYOUT_H_ZERO
        read_only = false
        textWidget = TextWidget.createText(this, id)
        textWidget.addVerifyListener { e: VerifyEvent ->
            e.doit = skipVerify
                    || !read_only
                    || e.character == Constants.DEL_KEY
                    || e.character == SWT.CR
        }
        textWidget.menu = Menu(textWidget) // prevent system menu
    }

    override val widget: Text get() = textWidget

    override fun setFocus(): Boolean {
        textWidget.setFocus()
        return true
    }


    override fun addKeyListenerInternal(listener: KeyListener) {
        textWidget.addKeyListener(listener)
    }

    override fun addFocusListenerInternal(listener: FocusListener) {
        textWidget.addFocusListener(listener)
    }

    fun setAllowEmpty(allowEmpty: Supplier<Boolean>) {
        this.allowEmpty = allowEmpty
    }

    fun setReadOnly() {
        read_only = true
    }

    fun setEditAction(editAction: () -> Unit) {
        this.editAction = editAction
    }


    override fun setMenu(menu: Menu) {
        textWidget.menu = menu
    }

    fun set(id: String?) {
        skipVerify = true
        textWidget.text = id
        skipVerify = false
        //textWidget.requestLayout()
    }



    override val textToSerialize: String =
        if (textWidget.text.isBlank()) "ID" else textWidget.text
}