package pt.iscte.javardise.widgets.members

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
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
import pt.iscte.javardise.basewidgets.SequenceWidget
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
    val importsComposite: Composite

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
            val name =
                if (node.packageDeclaration.isPresent) node.packageDeclaration.get().name else Configuration.qNameHole()
            packageWidget = NameWidget(this, node).apply {
                set(nodeText(name))
            }
            packageWidget.addFocusLostAction(::isValidQualifiedName) {
                try {
                    val newName = StaticJavaParser.parseName(packageWidget.text)
                    node.modifyCommand(
                        node.packageDeclaration.get(),
                        PackageDeclaration(newName),
                        node::setPackageDeclaration
                    )
                } catch (e: Exception) {
                    packageWidget.set(name.toString())
                }
            }
            packageWidget.addDeleteEmptyListener {
                node.modifyCommand(node.packageDeclaration.get(), null, node::setPackageDeclaration)
            }
            TokenWidget(this, ";").apply {
                addKeyEvent(SWT.CR) {
                    val newImport = ImportDeclaration(Configuration.qNameHole(), false, false)
                    node.imports.addCommand(node, newImport, 0)
                }
                addDeleteListener {
                    node.modifyCommand(node.packageDeclaration.get(), null, node::setPackageDeclaration)
                }
            }
        }
        packageDeclaration.visible = node.packageDeclaration.isPresent

        importsComposite = column {
            layout = RowLayout(SWT.VERTICAL).apply {
                spacing = 0
                marginLeft = 0
            }
            node.imports.forEach {
                ImportWidget(this, it)
            }
        }
        setImportMargin(node.imports.isNonEmpty)

        node.findMainClass()?.let {
            classWidget = ClassWidget(this, it, configuration = configuration)
        }

        node.observeProperty<PackageDeclaration>(ObservableProperty.PACKAGE_DECLARATION) {
            if (it == null) {
                packageDeclaration.visible = false
                if (importsComposite.children.isEmpty())
                    classWidget.setFocus()
                else
                    importsComposite.setFocus()
            } else {
                packageWidget.set(nodeText(it.name))
                packageDeclaration.visible = true
                packageWidget.setFocus()
            }
        }
        observeListUntilDispose(node.imports, object : ListAnyModificationObserverPost<ImportDeclaration> {
            override fun listModification(postList: NodeList<ImportDeclaration>, indexChanged: Int) {
                importsComposite.children.forEach { it.dispose() }
                postList.forEachIndexed { i, e ->
                    val w = ImportWidget(importsComposite, e)
                    if (i == indexChanged)
                        w.setFocus()
                }
                if (postList.isEmpty())
                    packageWidget.setFocus()

                setImportMargin(postList.isNonEmpty)
            }
        })
    }

    private fun setImportMargin(on: Boolean) {
        (importsComposite.layout as RowLayout).marginTop = if (on) 15 else 10
        (importsComposite.layout as RowLayout).marginBottom = if (on) 15 else 0
    }

    override val control: Control
        get() = this

    override fun setFocus(): Boolean {
        if (!isDisposed)
            if (packageDeclaration.visible)
                return packageWidget.setFocus()
            else if (importsComposite.children.isNotEmpty())
                return importsComposite.setFocus()
            else
                return classWidget.setFocus()
        return false
    }

    override fun setFocusOnCreation(firstFlag: Boolean) {
        if (firstFlag) {
            keyword.setFocus()
        } else {
            packageWidget.setFocus()
        }
    }

    fun focusPackage() {
        if (packageDeclaration.visible)
            packageWidget.setFocus()
    }

    fun focusClass() {
        classWidget.setFocus()
    }

    override val commandStack: CommandStack
        get() = classWidget.commandStack


    inner class ImportWidget(parent: Composite, override val node: ImportDeclaration) :
        Composite(parent, SWT.NONE), NodeWidget<ImportDeclaration> {

        lateinit var keyword: TokenWidget
        lateinit var name: NameWidget<ImportDeclaration>

        init {
            layout = ROW_LAYOUT_H
            font = parent.font
            background = parent.background
            foreground = parent.foreground

            row {
                layout = ROW_LAYOUT_H
                keyword = newKeywordWidget(this, "import")
                keyword.addDeleteListener {
                    removeImport()
                }
                name = NameWidget(this, node, true).apply {
                    set(nodeText(node.name))
                }
                name.addFocusLostAction(::isValidImportName) {
                    try {
                        val wildcard = name.text.endsWith(".*")
                        val newName = StaticJavaParser.parseName(
                            if (wildcard)
                                name.text.substring(0, name.text.length - 2)
                            else
                                name.text
                        )
                        node.modifyCommand(node.name, newName, node::setName)
                        node.modifyCommand(node.isAsterisk, wildcard, node::setAsterisk)
                    } catch (e: Exception) {
                        name.set(
                            if (node.isAsterisk)
                                nodeText(node.name) + ".*"
                            else
                                nodeText(node.name)
                        )
                    }
                }
                name.addDeleteEmptyListener {
                    removeImport()
                }

                TokenWidget(this, ";").apply {
                    addKeyEvent(SWT.CR) {
                        val newImport = ImportDeclaration(Configuration.qNameHole(), false, false)
                        val index = this@CompilationUnitWidget.node.imports.indexOfIdentity(node)
                        this@CompilationUnitWidget.node.imports.addCommand(
                            this@CompilationUnitWidget.node,
                            newImport,
                            index + 1
                        )
                    }
                    addDeleteListener {
                        removeImport()
                    }
                }
            }
        }

        private fun removeImport() {
            this@CompilationUnitWidget.node.imports.removeCommand(this@CompilationUnitWidget.node, node)
            this@CompilationUnitWidget.setFocus()
        }

        override val control: Control
            get() = this

        override fun setFocus(): Boolean {
            return keyword.setFocus()
        }

        override fun setFocusOnCreation(firstFlag: Boolean) {
            name.setFocus()
        }
    }

}