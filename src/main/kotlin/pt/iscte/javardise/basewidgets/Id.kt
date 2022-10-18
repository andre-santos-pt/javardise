package pt.iscte.javardise.basewidgets

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.updateColor
import javax.lang.model.SourceVersion

val ID = Regex("[a-zA-Z][a-zA-Z0-9_]*")
val ID_CHARS = Regex("[a-zA-Z0-9_]")

val TYPE = Regex("[a-zA-Z0-9_\\[\\]<>]")
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

    fun isValidAndDifferent(previous: String) = isValid() && text != previous

    override val widget: Text get() = textWidget

    override fun setFocus(): Boolean {
        textWidget.setFocus()
        textWidget.requestLayout()
        return true
    }


    override fun addKeyListenerInternal(listener: KeyListener) {
        textWidget.addKeyListener(listener)
    }

    override fun addFocusLostAction(action: () -> Unit): FocusListener {
        val listener = object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                action()
            }
        }
        textWidget.addFocusListener(listener)
        return listener
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

open class TypeId(parent: Composite, id: String) : Id(parent, id, TYPE_CHARS, {
        s ->
        try {
            StaticJavaParser.parseType(s)
            Validation(true, "")
        } catch (e: ParseProblemException) {
            Validation(false, e.message.toString())
        }

})