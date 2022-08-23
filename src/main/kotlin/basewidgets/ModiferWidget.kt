package basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Composite

abstract class ModiferWidget<T>(parent: Composite, e: Any) :
    EditorWidget(parent, e) {


    private val modifiers: MutableList<TokenWidget> = mutableListOf()
    protected abstract val header: EditorWidget
    protected abstract val modifierProvider: (List<String>)->List<String>


    protected fun addModifierKey(control: TextWidget) {
        control.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == SWT.SPACE && (!control.isModifiable || control.isAtBeginning)) {
                    val k = inexistentKeyword()
                    if (k != null) {
                        addModifier(k)
                        e.doit = false
                    }
                }
            }

            private fun inexistentKeyword(): String? {
                for (k in modifierProvider(getModifiers()))
                    if (!modifiers.stream().anyMatch { t: TokenWidget -> t.text == k })
                        return k
                return null
            }
        })
    }

    fun addModifier(mod: String) {
        val modifier = TokenWidget(header, mod) { modifierProvider(getModifiers()) }
        modifier.widget.data = "MODIFIER"
        modifier.addKeyListenerInternal(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.character == Constants.DEL_KEY) {
                    // TODO property update
//					procedure.setProperty(mod.toString(), null);
                    modifier.dispose()
                    header.requestLayout()
                    val i = modifiers.indexOf(modifier)
                    modifiers.remove(modifier)
                    if (i < modifiers.size) modifiers[i].setFocus()
                    else header.setFocus()
                }
            }
        })
        if (modifiers.isEmpty()) modifier.moveAboveInternal(header.children.first())
        else modifier.moveBelowInternal(modifiers.last().widget)
        modifier.widget.requestLayout()
        modifier.setFocus()
        modifiers.add(modifier)
    }

    fun getModifiers() = modifiers.map { it.text }
}