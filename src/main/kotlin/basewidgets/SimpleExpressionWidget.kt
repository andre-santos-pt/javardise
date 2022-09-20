package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Text

import basewidgets.Constants.Companion.isNumber

class SimpleExpressionWidget(parent: Composite, literal: String, e: Any?) :
    EditorWidget(parent, e), TextWidget, Expression {

    private var w: Text
    private val rules: MutableList<Rule> = mutableListOf()
    private var skipVerify = false

    init {
        layout = Constants.ROW_LAYOUT_H_ZERO
        w = TextWidget.createText(this, literal) { e: Char, text: String ->
            skipVerify ||
                    validCharacter(e)
                    || e == '.' && text.indexOf('.') == -1 && (text.isEmpty() || isNumber(text))
                    || e == Constants.DEL_KEY
        }

        addTransformationKeyListener()
        w.addModifyListener {
        }
        w.menu = Menu(w)
    }

    data class Rule(
        val accept: (TextWidget, Char) -> Boolean,
        val creator: (Composite, TextWidget, Char) -> Expression,
        val afterAction: (Expression) -> Unit = { it.setFocus() }
    )

    fun addRule(
        accept: (TextWidget, Char) -> Boolean,
        creator: (Composite, TextWidget, Char) -> Expression,
        afterAction: (Expression) -> Unit = {}
    ) = rules.add(Rule(accept, creator, afterAction))


    private fun addTransformationKeyListener() {
        w.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (w.isDisposed) return
                var rule = rules.find { it.accept(this@SimpleExpressionWidget, e.character) }
                if (rule == null && e.character == SWT.CR) this@SimpleExpressionWidget.w.traverse(SWT.TRAVERSE_TAB_NEXT)
                if (rule != null) {
                    applyRule(rule, e.character)
                }
            }
        })
    }

    fun applyRule(character: Char): Boolean {
        var rule = rules.find { it.accept(this, character) }
        rule?.let {
            applyRule(rule, character)
            return true
        }

        return false
    }

    fun applyRule(rule: Rule, character: Char) {
        var w: Expression = rule.creator(parent, this@SimpleExpressionWidget, character)
        val p = parent as Expression
        if (p.isSubstitutable) {
            (p as Expression.SubstitutableExpression).substitute(this@SimpleExpressionWidget, w)
            p.requestLayoutInternal()
        }
        rule.afterAction(w)
    }

    override fun setData(data: Any?) {
        w.data = data
    }

    override val widget : Text
        get() = w

    override fun setFocus(): Boolean {
        return w.setFocus()
    }

    override fun addKeyListenerInternal(listener: KeyListener) {

    }

    override fun addFocusListenerInternal(listener: FocusListener) {
        w.addFocusListener(listener)
    }

    override fun addFocusLostAction(action: () -> Unit) {
        w.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                action()
            }
        })
    }

    override fun requestLayoutInternal() {
        w.requestLayout()
    }

    private fun validCharacter(c: Char): Boolean {
        return true
    }

    override fun setMenu(menu: Menu) {
        w.menu = menu
    }

    fun set(expression: String?) {
        skipVerify = true
        w.text = expression
        skipVerify = false
    }


    override fun toString(): String {
        return w.text
    }

    override fun copyTo(parent: EditorWidget): Expression {
        val c = SimpleExpressionWidget(parent, w.text, programElement)
        c.rules.addAll(rules)
        return c
    }

    override val textToSerialize: String
        get() = if (w.text.isBlank()) "BLANK" else w.text


}