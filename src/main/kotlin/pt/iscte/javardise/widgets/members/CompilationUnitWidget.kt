package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import pt.iscte.javardise.*
import pt.iscte.javardise.basewidgets.FixedToken
import pt.iscte.javardise.basewidgets.TokenWidget
import pt.iscte.javardise.external.*

class CompilationUnitWidget(
    parent: Composite,
    override val node: CompilationUnit,
    override val configuration: Configuration
) : Composite(parent, SWT.NONE), NodeWidget<CompilationUnit>, ConfigurationRoot {

    var packageDeclaration: Composite
    lateinit var keyword: TokenWidget
    lateinit var packageWidget: NameWidget<CompilationUnit>
    lateinit var classWidget: ClassWidget

    init {
        layout = ROW_LAYOUT_V_ZERO
        font = configuration.font
        background = configuration.backgroundColor
        foreground = configuration.foregroundColor

        packageDeclaration = row {
            layout = ROW_LAYOUT_H
            keyword = newKeywordWidget(this, "package")
            keyword.addDeleteListener {
                node.modifyCommand(node.packageDeclaration.get(), null, node::setPackageDeclaration)
            }
            val name = if(node.packageDeclaration.isPresent) node.packageDeclaration.get().name else Name("NA")
            packageWidget = NameWidget(this, node).apply {
                set(name.toString())
            }
            packageWidget.addFocusLostAction {
                try {
                    val newName = StaticJavaParser.parseName(packageWidget.text)
                    node.modifyCommand(node.packageDeclaration.get(), PackageDeclaration(newName), node::setPackageDeclaration)
                } catch (e: Exception) {
                    packageWidget.set(name.toString())
                }
            }
            packageWidget.addDeleteEmptyListener {
                node.modifyCommand(node.packageDeclaration.get(), null, node::setPackageDeclaration)
            }
            FixedToken(this, ";")

        }
        packageDeclaration.visible = node.packageDeclaration.isPresent
        Label(this, SWT.NONE)


        node.findMainClass()?.let {
            classWidget = ClassWidget(this, it, configuration = configuration)
        }

        node.observeProperty<PackageDeclaration>(ObservableProperty.PACKAGE_DECLARATION) {
            if (it == null) {
                packageDeclaration.visible = false
                classWidget.setFocus()
            }
            else{
                packageWidget.set(it.name.toString())
                packageDeclaration.visible = true
                packageWidget.setFocus()
            }
        }
    }


    override val control: Control
        get() = this

    override fun setFocus(): Boolean {
        return keyword.setFocus()
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if (firstFlag) {
            keyword.setFocus()
        } else {
            packageWidget.setFocus()
        }
    }

    override val commandStack: CommandStack
        get() = classWidget.commandStack
}