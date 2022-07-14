package pt.iscte.javardise.api

import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text

interface IEditorConfiguration {

    fun configureToken(widget: Text) { }

    fun configureFixedToken(widget: Label) { }

    val idStyler : (w: Text) -> Unit
        get() = { }

}