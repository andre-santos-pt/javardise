package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.updateColor

val ID = Regex("[a-zA-Z][a-zA-Z0-9_]*")
val ID_CHARS = Regex("[a-zA-Z0-9_]")

val TYPE_CHARS = Regex("[a-zA-Z0-9_\\[\\]<>]")

data class Validation(val ok: Boolean, val msg: String) {
    val fail get() = !ok
}

open class Id(parent: Composite, id: String, allowedChars: Regex,
              validate: (String) -> Validation
) :
    TextWidget {
    private var readOnly: Boolean
    internal val textWidget: Text
    private var skip = false

    init {
        readOnly = false
        textWidget = TextWidget.createText(parent, id) { c, s ->
            skip ||
                    !readOnly && (
                    c.toString().matches(allowedChars)
                            || c == SWT.BS
                            || c == SWT.CR)
        }
        textWidget.menu = Menu(textWidget) // prevent system menu

        textWidget.background = parent.background
        updateColor(this)

        textWidget.addModifyListener {
            updateColor(this)
        }
//        textWidget.addModifyListener {
//            val validate = validate(textWidget.text)
//            if (validate.fail) {
//                textWidget.background = ERROR_COLOR()
//                textWidget.toolTipText = validate.msg
//                //textWidget.toolTipText = "Valid identifiers cannot start with a number."
//            } else if (SourceVersion.isKeyword(textWidget.text)) {
//                textWidget.background = ERROR_COLOR()
//                textWidget.toolTipText = "'${textWidget.text}' is a reserved keyword in Java." // BUG shown in types
//            } else {
//                textWidget.background = BACKGROUND_COLOR()
//                textWidget.toolTipText = ""
//            }
//        }
    }


    open fun isValid() = true

    override val widget: Text get() = textWidget

    override fun setFocus(): Boolean {
        textWidget.setFocus()
        textWidget.requestLayout()
        return true
    }


    override fun addKeyListenerInternal(listener: KeyListener) {
        textWidget.addKeyListener(listener)
    }

    fun setReadOnly() {
        readOnly = true
    }

    fun set(text: String?) {
        skip = true
        textWidget.text = text ?: ""
        skip = false
    }
}

