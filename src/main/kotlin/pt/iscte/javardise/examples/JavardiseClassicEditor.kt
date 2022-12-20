package pt.iscte.javardise.examples

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.eclipse.swt.widgets.List
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.external.*
import pt.iscte.javardise.views.ClassDocumentationView
import pt.iscte.javardise.widgets.expressions.CallFeature
import pt.iscte.javardise.widgets.expressions.VariableDeclarationFeature
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.javardise.widgets.statements.EmptyStatementFeature
import pt.iscte.javardise.widgets.statements.IfFeature
import pt.iscte.javardise.widgets.statements.ReturnFeature
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import kotlin.collections.set


fun main(args: Array<String>) {
    val display = Display()
    val root = if (args.isEmpty()) {
        val fileDialog = DirectoryDialog(Shell(display))
        File(fileDialog.open() ?: "")
    } else {
        val folder = File(args[0])
        if (!folder.exists())
            try {
                folder.mkdir()
            } catch (e: FileNotFoundException) {
                System.err.println("could not create folder: ${folder.absoluteFile}")
                return;
            }
        folder
    }

    val window = JavardiseClassicEditor(display, root)
    window.open()
}


data class TabData(
    val file: File,
    val model: ClassOrInterfaceDeclaration?,
    val classWidget: ClassWidget?
)


class JavardiseClassicEditor(val display: Display, val folder: File) {

    private val shell = Shell(display)

    val openTabs = mutableListOf<Composite>()
    val focusMap = WeakHashMap<Composite, Control>()
    val stacklayout = StackLayout()

    val classOnFocus: ClassWidget?
        get() = if (stacklayout.topControl == null)
            null
        else
            (stacklayout.topControl.data as TabData).classWidget

    val compileErrors: CompileErrors = mutableMapOf()

    init {
        require(folder.exists() && folder.isDirectory)

        shell.layout = GridLayout(2, false)
        shell.text = folder.absolutePath

        val fileList = List(shell, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL)
        val gdata = GridData(GridData.VERTICAL_ALIGN_FILL)
        gdata.minimumWidth = 150
        fileList.layoutData = gdata
        val editorArea = Composite(shell, SWT.NONE)
        editorArea.layoutData = GridData(GridData.FILL_BOTH)
        editorArea.layout = stacklayout

        fileList.addSelectionListener(object : SelectionAdapter() {
            var current = -1
            override fun widgetSelected(e: SelectionEvent) {
                if (fileList.selection.isNotEmpty() && current != fileList.selectionIndex) {
                    current = fileList.selectionIndex
                    val find =
                        openTabs.find { (it.data as TabData).file.name == fileList.selection.first() }
                    if (find != null) {
                        stacklayout.topControl = find
                        editorArea.layout()
                        // TODO repor
//                        if (focusMap.containsKey(find))
//                            focusMap[find]?.setFocus()
                    } else {
                        val tab = createTab(
                            File(
                                folder.absoluteFile,
                                fileList.selection.first()
                            ), editorArea
                        )
                        openTabs.add(tab)
                        stacklayout.topControl = tab

                        editorArea.layout()

                        val data = tab.data as TabData
//                        if (data.model != null)
//                            compile(data.model)
                    }
                }
            }
        })

        // FileFilter { it.name.endsWith(".java") }
        folder.listFiles()?.forEach {
            fileList.add(it.name)
        }


        fileList.menu {
            item("New file") {
                shell.prompt("New file", "name") {
                    val f = File(folder, it)
                    f.createNewFile()
                    fileList.add(f.name)
                    fileList.requestLayout()
                    fileList.select(fileList.itemCount)
                }
            }
            item("Compile") {
                if (stacklayout.topControl != null) {
                    val model = (stacklayout.topControl.data as TabData).model
                    model?.let {
                        compile(it)
                    }
                }
            }
            item("Documentation") {
                val model = (stacklayout.topControl.data as TabData).model
                shell {
                    ClassDocumentationView(this, model!!)
                }.open()
            }

        }

        handleShortcuts()
    }

    private fun handleShortcuts() {
        display.addFilter(SWT.KeyDown) {
            if (it.stateMask == SWT.MOD1 && it.keyCode == 'z'.code) {
                val commandStack = classOnFocus?.commandStack
                commandStack?.undo()
                it.doit = false
            } else if (it.stateMask == SWT.MOD1 or SWT.SHIFT && it.keyCode == 'z'.code) {
                val commandStack = classOnFocus?.commandStack
                commandStack?.redo()
                it.doit = false
            } else if (it.keyCode == SWT.PAGE_DOWN) {
                classOnFocus?.getMemberOnFocus()?.let {
                    val members = classOnFocus!!.node.members
                    val index = members.indexOf(it)
                    if (index + 1 < members.size)
                        classOnFocus?.focus(members[index + 1])
                }
                it.doit = false
            } else if (it.keyCode == SWT.PAGE_UP) {
                classOnFocus?.getMemberOnFocus()?.let {
                    val members = classOnFocus!!.node.members
                    val index = members.indexOf(it)
                    if (index - 1 >= 0)
                        classOnFocus?.focus(members[index - 1])
                }
                it.doit = false
            }

        }
    }

    private fun compile(model: ClassOrInterfaceDeclaration) {
        compileErrors.forEach {
            it.value.forEach { it.delete() }
        }

        compileErrors.clear()
        // FileFilter { it.name.endsWith(".java") }

        val files = folder.listFiles()
            .map {
                Triple(
                    it.absolutePath,
                    StaticJavaParser.parse(it).findMainClass(),
                    null
                )
            }

            .filter { it.second != null }
            .map {
                Pair(
                    it.second!!,
                    openTabs.find { w -> (w.data as TabData).file.absolutePath == it.first }?.data as? TabData
                )
            }
            .filter { it.second != null }
            .map {
                Pair(
                    it.first,
                    it.second!!.classWidget!!
                )
            }
            .map { println(it); it }
        val errors = checkCompileErrors(files)
        compileErrors.putAll(errors)

        compileErrors[model]?.forEach {
            it.show()
        }
    }

    val File.extension
        get() = if (name.contains('.'))
            name.substring(name.lastIndexOf('.') + 1)
        else
            ""

    private fun createTab(
        file: File,
        comp: Composite
    ): Composite {
        val tab = Composite(comp, SWT.BORDER)
        val layout = FillLayout()
        tab.layout = layout

        val typeName = if (file.name.contains('.'))
            file.name.substring(0, file.name.lastIndexOf('.'))
        else
            file.name

        val model = if (file.exists()) {
            try {
                val cu = loadCompilationUnit(file)
                cu.findMainClass() ?: ClassOrInterfaceDeclaration(
                    NodeList(), false, typeName
                )
            } catch (e: ParseProblemException) {
                System.err.println("Could not load: $file")
                null
            }
        } else {
            ClassOrInterfaceDeclaration(NodeList(), false, typeName)
        }

        if (model == null) {
            tab.data = TabData(file, null, null)
            tab.fill {
                val src = file.readLines().joinToString(separator = "\n")
                label(src).foreground =
                    Display.getDefault().getSystemColor(SWT.COLOR_RED)
            }
        } else {
            val w = tab.scrollable {
                createWidget(file.extension, it, model)
            }
            // TODO not working
            w.addFocusObserver { control ->
                focusMap[tab] = control
            }
            w.setAutoScroll()

            val scroll = w.parent as ScrolledComposite
            scroll.verticalBar.addListener(SWT.Selection, object : Listener {
                override fun handleEvent(p0: Event?) {
                    compileErrors[model]?.forEach {
                        it.show()
                    }
                }
            })

            w.commandStack.addObserver { _, _ ->
                val writer = PrintWriter(file)
                writer.println(model.toString())
                writer.close()

                // compile(model)
            }
            tab.data = TabData(file, model, w)

            //addUndoScale(tab, w)
        }

        return tab
    }

    private fun addUndoScale(
        tab: Composite,
        w: ClassWidget
    ) {

        val scale = Scale(tab, SWT.BORDER)
        scale.minimum = 0
        scale.maximum = 1
        scale.pageIncrement = 1
        scale.enabled = false

        scale.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                while (scale.selection  < w.commandStack.stackTop)
                    w.commandStack.undo()
                while (scale.selection  > w.commandStack.stackTop) {
                    w.commandStack.redo()
                }
            }
        })
        w.commandStack.addObserver { command, exec ->
            if (w.commandStack.stackTop >= 0) {
                scale.enabled = true
                scale.maximum = w.commandStack.stackSize-1
                scale.selection = w.commandStack.stackTop
            } else {
                scale.enabled = false
            }
            scale.requestLayout()
        }
    }

    fun createWidget(
        ext: String,
        parent: Composite,
        model: ClassOrInterfaceDeclaration
    ): ClassWidget =
        when (ext) {
            "sjava" -> StaticClassWidget(parent, model)
            "fjava" -> StaticClassWidget(
                parent,
                model,
                configuration = object : DefaultConfiguration() {
                    override val fontSize: Int
                        get() = 20

                    override val statementFeatures
                        get() = listOf(
                            EmptyStatementFeature,
                            IfFeature,
                            VariableDeclarationFeature,
                            CallFeature,
                            ReturnFeature
                        )

                })
            else -> ClassWidget(parent, model)
        }


    fun notFoundLabel(p: Composite) = Composite(p, SWT.BORDER).apply {
        layout = FillLayout()
        Label(this, SWT.NONE).apply {
            text = "No public class found"
        }
    }


    fun open() {
        shell.size = Point(600, 800)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }


//    private fun load(file: File): ClassOrInterfaceDeclaration {
//        require(file.name.endsWith(".java")) {
//            "Java file must have '.java' extension"
//        }
//        val typeName = file.name.dropLast(".java".length)
//
//        require(SourceVersion.isIdentifier(typeName)) {
//            "'$typeName' is not a valid identifier for a Java type"
//        }
//
//        val model = if (file.exists()) {
//            val cu = loadCompilationUnit(file)
//            cu.findMainClass() ?: ClassOrInterfaceDeclaration(
//                NodeList(), false, typeName
//            )
//        } else {
//            val cu = CompilationUnit()
//            cu.addClass(typeName)
//        }
//
//        model.observeProperty<SimpleName>(ObservableProperty.NAME) {
//            shell.text = it?.toString() ?: "No public class found"
//        }
//        shell.requestLayout()
//        return model
//    }
}





