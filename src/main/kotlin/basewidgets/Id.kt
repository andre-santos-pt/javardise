package basewidgets

import javawidgets.BACKGROUND_COLOR
import javawidgets.ERROR_COLOR
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.*
import javax.lang.model.SourceVersion

val ID = Regex("[a-zA-Z][a-zA-Z0-9_]*")
val ID_CHARS = Regex("[a-zA-Z0-9_]")

open class Id(parent: Composite, id: String) :
    TextWidget {
    private var readOnly: Boolean
    internal val textWidget: Text
    private var skip = false

    init {
        readOnly = false
        textWidget = TextWidget.createText(parent, id) { c, s ->
            skip ||
                    !readOnly && (
                    c.toString().matches(ID_CHARS)
                            || c == Constants.DEL_KEY
                            || c == SWT.CR)
        }
        textWidget.menu = Menu(textWidget) // prevent system menu

        textWidget.addModifyListener {
            if (!textWidget.text.matches(ID)) {
                textWidget.background = ERROR_COLOR()
                textWidget.toolTipText = "Valid identifiers cannot start with a number."
            } else if (SourceVersion.isKeyword(textWidget.text)) {
                textWidget.background = ERROR_COLOR()
                textWidget.toolTipText = "'${textWidget.text}' is a reserved keyword in Java."
            } else {
                textWidget.background = BACKGROUND_COLOR()
                textWidget.toolTipText = ""
            }
        }
    }

    fun isValid() = textWidget.text.matches(ID) && !SourceVersion.isKeyword(textWidget.text)

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

    fun setReadOnly() {
        readOnly = true
    }

    fun set(id: String?) {
        skip = true
        textWidget.text = id
        skip = false
    }
}