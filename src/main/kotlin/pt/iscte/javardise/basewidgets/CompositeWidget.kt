package pt.iscte.javardise.basewidgets

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite

open class CompositeWidget(parent: Composite, vertical: Boolean = false) :
        Composite(parent, SWT.NONE) {
    init {
        layout = if (vertical) Constants.ROW_LAYOUT_V_ZERO else Constants.ROW_LAYOUT_H_ZERO
        //background = configuration.style.background
    }
}