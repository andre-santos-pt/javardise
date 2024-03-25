package pt.iscte.javardise.widgets.members

import com.github.javaparser.ast.CompilationUnit
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.ROW_LAYOUT_H
import pt.iscte.javardise.external.ROW_LAYOUT_V_ZERO
import pt.iscte.javardise.external.findMainClass
import pt.iscte.javardise.external.row

class CompilationUnitWidget(
    parent: Composite,
    override val node: CompilationUnit,
    override val configuration: Configuration
) : Composite(parent, SWT.NONE), NodeWidget<CompilationUnit>, ConfigurationRoot {

    lateinit var classWidget: ClassWidget

    init {
        layout = ROW_LAYOUT_V_ZERO
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor

        row {
            layout = ROW_LAYOUT_H
            if (node.packageDeclaration.isPresent) {
                newKeywordWidget (this, "package")
                SimpleNameWidget(this, node.packageDeclaration.get().name).apply {
                    setReadOnly()
                }
                FixedToken(this, ";")
            }
        }
        Label(this, SWT.NONE)

        node.findMainClass()?.let {
            classWidget = ClassWidget(this, it, configuration = configuration)
        }
    }


    override val control: Control
        get() = this

    override fun setFocusOnCreation(firstFlag: Boolean) {

    }

    override val commandStack: CommandStack
        get() = classWidget.commandStack
}