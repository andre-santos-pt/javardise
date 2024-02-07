package pt.iscte.javardise.editor

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.SourceFileInfoExtractor
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.external.*
import pt.iscte.javardise.isWindows
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*


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
                MessageBox(Shell(display), SWT.ICON_ERROR).apply {
                    text = "Error"
                    message = "Could not create folder: ${folder.absoluteFile}"
                }.open()
                return;
            }
        folder
    }

    if (root.exists())
        CodeEditor(display, root).open()
}


data class TabData(
    val file: File,
    val model: ClassOrInterfaceDeclaration?,
    val classWidget: ClassWidget?
)

fun setupSymbolSolver(folder: File) {
    val combinedTypeSolver = CombinedTypeSolver()
    combinedTypeSolver.add(ReflectionTypeSolver())
    combinedTypeSolver.add(JavaParserTypeSolver(folder))

    val symbolSolver = JavaSymbolSolver(combinedTypeSolver)
    StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver)
}


class CodeEditor(val display: Display, val folder: File) {
    private val shell: Shell
    private val toolbar: ToolBar
    private val tabs: CTabFolder
    private val console: Text

    private val javaIcon = loadImage("java.png")
    private val textIcon = loadImage("text-format.png")

    private val actions: MutableMap<Action, ToolItem> = mutableMapOf()

    private val commandObservers = mutableListOf<(Command?, Boolean?, CommandStack?) -> Unit>()

    private val settings = Settings(this)

    private val defaultActions = listOf(
        object : Action {
            override val name = "New file"
            override val iconPath = "new-document.png"
            override fun run(editor: CodeEditor, toggle: Boolean) {
                editor.shell.prompt("New file", "name") {
                    val f = File(folder, it)
                    if (f.exists()) {
                        editor.shell.message { label("File $f already exists.") }
                    } else {
                        f.createNewFile()
                        val tab = createFileTab(f)
                        tab.parent.selection = tab
                    }
                }
            }
        },
        object : Action {
            override val name = "Settings"
            override val iconPath = "settings.png"
            override fun run(editor: CodeEditor, toggle: Boolean) {
                settings.openDialog(editor.shell)
            }
        }
    )

    fun updateTabs() {
        tabs.items.forEach {
            val old = it.control
            it.control = createTab(it.data as File, tabs, it)
            old.dispose()
        }
    }


    init {
        require(folder.exists() && folder.isDirectory)

        setupSymbolSolver(folder)

        shell = Shell(display)
        shell.layout = GridLayout(1, false)
        shell.text = "Javardise: ${folder.absolutePath}"

        toolbar = ToolBar(shell, SWT.NONE)

        val comp = Composite(shell, SWT.NONE)
        comp.layout = GridLayout(2, false)
        comp.layoutData = GridData(GridData.FILL_BOTH)

        val gdata = GridData(GridData.VERTICAL_ALIGN_FILL)
        gdata.minimumWidth = 150

        val sash = SashForm(comp, SWT.VERTICAL).apply {
            layoutData = GridData(GridData.FILL_BOTH)
        }

        tabs = createTabArea(sash)

        buildTabs()

        fun Iterable<Action>.buildToolBarItems() =
            forEach {
                actions[it] = toolBarItem(it)
            }

        defaultActions.buildToolBarItems()
        ToolItem(toolbar, SWT.SEPARATOR)
        val pluginActions = ServiceLoader.load(Action::class.java)
        pluginActions.buildToolBarItems()

        console = Text(sash, SWT.MULTI or SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL).apply {
            editable = false
            font = Font(display, FontData("Courier", 14, SWT.NONE))
            toolTipText = "Console (read-only)"
            //background = Color(Display.getDefault(), 220, 220, 220)
        }

        sash.setWeights(90, 10)

        handleShortcuts()
        setActionsEnabled()
        addCommandObserver { _, _, _ ->
            setActionsEnabled()
        }
    }

    fun consoleAppend(s: String?) {
        console.text += s
    }

    fun consoleClear() {
        console.text = ""
    }

    private fun createTabArea(comp: Composite) =
        CTabFolder(comp, SWT.BORDER).apply {
            layoutData = GridData(GridData.FILL_BOTH)
            menu {
                item("Delete") {
                    val file = this@apply.selection?.data as? File
                    file?.let {
                        shell.promptConfirmation(
                            "Are you sure you want to permanently delete the file ${it.name}?"
                        ) {
                            it.delete()
                            this@apply.selection.dispose()
                            // to trigger delete file event
                            commandObservers.forEach {
                                it(null, null, null)
                            }
                        }
                    }
                }

                // does not work in Windows
//                item("Open in file system") {
//                    val file = this@apply.selection.data as? File
//                    file?.let {
//                        Desktop.getDesktop().browseFileDirectory(it)
//                    } ?: Desktop.getDesktop().browseFileDirectory(folder)
//                }
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

    private fun buildTabs() {
        (folder.listFiles() ?: emptyArray<File>())
            .filter { it.extension == "java" }
            .sortedBy { it.name }
            .forEach {
                createFileTab(it)
            }

        tabs.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                setActionsEnabled()
            }
        })
    }

    private fun createFileTab(f: File): CTabItem {
        val item = CTabItem(tabs, SWT.NONE)
        item.text = if (f.extension == "java") f.nameWithoutExtension else f.name
        item.image = if (f.extension == "java") javaIcon else textIcon
        val tab = createTab(f, tabs, item)
        item.setControl(tab)
        item.data = f
        return item
    }


    val classOnFocus: ClassWidget? get() = (tabs.selection?.control?.data as? TabData)?.classWidget
    fun getClassWidget(file: File) = (tabs.items.find { it.data == file }
        ?.control?.data as? TabData)?.classWidget

    fun allClassWidgets() =
        tabs.items.filter { it.control?.data is TabData }.map { (it.control.data as TabData).classWidget }.filterNotNull()

    fun allClasses() =
        tabs.items.filter { it.control?.data is TabData }.map { (it.control.data as TabData).model }.filterNotNull()


    fun setFileErrors(files: Set<File>) {
        tabs.items.forEach {
            val color = if (files.contains(it.data))
                Display.getDefault().getSystemColor(SWT.COLOR_RED)
            else
                Display.getDefault().getSystemColor(SWT.COLOR_TITLE_FOREGROUND)
            it.selectionForeground = color
            it.foreground = color
        }
    }

    fun addCommandObserver(o: (Command?, Boolean?, CommandStack?) -> Unit) {
        commandObservers.add(o)
        tabs.items.filter { (it.control.data is TabData) }
            .forEach {
                (it.control.data as TabData).classWidget?.commandStack?.addObserver { c: Command, undo: Boolean ->
                    o(c, undo, (it.control.data as TabData).classWidget?.commandStack)
                }
            }
    }

    fun removeCommandObserver(o: (Command?, Boolean?, CommandStack?) -> Unit) {
        commandObservers.remove(o)
        tabs.items.filter { (it.control.data is TabData) }
            .forEach {
                // TODO BUG nao remove observador
                (it.control.data as TabData).classWidget?.commandStack?.removeObserver { c: Command, undo: Boolean ->
                    o(c, undo, null)
                }
            }
    }

    private fun invert(img: Image): Image {
        val data = img.imageData
        for (y in 0 until data.height)
            for (x in 0 until data.width) {
                val rgb = data.palette.getRGB(data.getPixel(x, y))
                rgb.red = 255 - rgb.red
                rgb.green = 255 - rgb.green
                rgb.blue = 255 - rgb.blue
                data.setPixel(x, y, data.palette.getPixel(rgb))
            }
        return Image(Display.getDefault(), data)

    }

    private fun toolBarItem(action: Action) =
        ToolItem(toolbar, if (action.toggle) SWT.CHECK else SWT.PUSH).apply {
            if (action.iconPath == null)
                text = action.name
            action.iconPath?.let { path ->
                val icon = Image(Display.getDefault(), this::class.java.classLoader.getResourceAsStream(path))
                image = if (Display.isSystemDarkTheme() && !isWindows) {
                    invert(icon)
                } else
                    icon
                toolTipText = action.name
            }

            action.init(this@CodeEditor)
            if (action.toggle && action.toggleDefault) {
                selection = true
                action.run(this@CodeEditor, selection)
                setActionsEnabled()
            }

            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    if (action.isEnabled(this@CodeEditor)) {
                        action.run(this@CodeEditor, selection)
                        setActionsEnabled()
                    }
                }
            })
        }

    private fun setActionsEnabled() {
        actions.forEach {
            it.value.enabled = it.key.isEnabled(this)
        }
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


    val File.extension
        get() = if (name.contains('.'))
            name.substring(name.lastIndexOf('.') + 1)
        else
            ""

    private fun createTab(
        file: File,
        comp: Composite,
        item: CTabItem
    ): Composite {
        require(file.exists())

        val tab = Composite(comp, SWT.BORDER)
        val layout = FillLayout()
        tab.layout = layout

        val model = if (file.name.endsWith(".java")) {
            val typeName = if (file.name.contains('.'))
                file.name.substring(0, file.name.lastIndexOf('.'))
            else
                file.name

            try {
                val cu = loadCompilationUnit(file)
                val clazz = cu.findMainClass() ?: ClassOrInterfaceDeclaration(
                    NodeList(), false, typeName
                )
                if (cu.findMainClass() == null) {
                    val writer = PrintWriter(file, "UTF-8")
                    writer.println(clazz.toString())
                    writer.close()
                }
                clazz
            } catch (e: ParseProblemException) {
                //System.err.println("Could not load: $file")
                System.err.println(e.problems)
                null
            } catch (e: FileNotFoundException) {
                null
            }
        } else null

        if (model == null) {
            tab.data = TabData(file, null, null)
            tab.grid {
                val src = file.readLines().joinToString(separator = "\n")
                val t = Text(this, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL or SWT.H_SCROLL).apply {
                    text = src
                    fillGrid()
                }
                if (file.extension == "java")
                    button("Reload") {
                        val writer = PrintWriter(file, "UTF-8")
                        writer.println(t.text)
                        writer.close()
                        val tabItem = tabs.items.find { it.control == tab }
                        tabItem?.control = createTab(file, tabs, tabItem!!)
                        tab.dispose()
                    }.moveAbove(t)
            }
        } else {
            val w = tab.scrollable {
                createWidget(file.extension, it, model)
            }

            addAutoRenameFile(model, file, item)
            w.commandStack.addObserver { cmd, _ ->
                saveAndSyncRanges(item.data as File, model)
            }
            commandObservers.forEach {
                w.commandStack.addObserver  { c: Command, undo: Boolean ->
                    it(c, undo, w.commandStack)
                }
            }
            // to trigger new file event
            commandObservers.forEach {
                it(null, null, null)
            }

            tab.data = TabData(file, model, w)

            //addUndoScale(tab, w)
        }

        return tab
    }

    private fun addAutoRenameFile(
        model: ClassOrInterfaceDeclaration,
        file: File,
        item: CTabItem
    ) {
        model.register(object : AstObserverAdapter() {
            override fun propertyChange(
                observedNode: Node,
                property: ObservableProperty,
                oldValue: Any?,
                newValue: Any?
            ) {
                if (property == ObservableProperty.NAME) {
                    val newFile = File(folder, "$newValue.java")
                    if (newFile != file && !newFile.exists()) {
                        newFile.createNewFile()
                        file.delete()
                        item.text = newValue.toString()
                        item.data = newFile
                    }
                }
            }
        })
    }

    private fun saveAndSyncRanges(file: File, model: ClassOrInterfaceDeclaration) {
        val writer = PrintWriter(file, "UTF-8")
        writer.println((model.findCompilationUnit().getOrNull ?: model).toString())
        writer.close()

        val parse = StaticJavaParser.parse(file).types.first.get()
        val srcNodeList = mutableListOf<Node>()
        parse.accept(NodeCollectorVisitor(), srcNodeList)

        val modelNodeList = mutableListOf<Node>()
        model.accept(NodeCollectorVisitor(), modelNodeList)

//        check(modelNodeList.size == srcNodeList.size) {
//            var msg = "${modelNodeList.size} ${srcNodeList.size}"
//            for (i in 0 until min(modelNodeList.size, srcNodeList.size)) {
//                if (modelNodeList[i] != srcNodeList[i]) {
//                    msg = "${modelNodeList[i]} ${System.lineSeparator()} ${System.lineSeparator()} ${srcNodeList[i]}"
//                    break
//                }
//            }
//            msg
//        }

//        srcNodeList.forEachIndexed { i, n ->
//            if (i < modelNodeList.size && modelNodeList[i] == srcNodeList[i])
//                modelNodeList[i].setRange(n.range.get())
//        }

        var i = 0
        var j = 0
        while (i < srcNodeList.size && j < modelNodeList.size) {
            if (modelNodeList[j] == srcNodeList[i]) {
                modelNodeList[j].setRange(srcNodeList[i].range.get())
                j++
            }
            i++;
        }
    }


    private fun createWidget(
        ext: String,
        parent: Composite,
        model: ClassOrInterfaceDeclaration
    ): ClassWidget = ClassWidget(parent, model, configuration = settings.editorConfiguration, workingDir = folder)
//        when (ext) {
//            "sjava" -> StaticClassWidget(parent, model)
//            "fjava" -> StaticClassWidget(
//                parent,
//                model,
//                configuration = object : DefaultConfiguration() {
//                    override var fontSize: Int = super.fontSize
//                        get() = settings.editorConfiguration.fontSize
//
//                    override val statementFeatures
//                        get() = listOf(
//                            EmptyStatementFeature,
//                            IfFeature,
//                            VariableDeclarationFeature,
//                            CallFeature,
//                            ReturnFeature
//                        )
//
//                })
    //"mjava" -> MainScriptWidget(parent, model)
//            else -> ClassWidget(parent, model, configuration = settings.editorConfiguration, workingDir = folder)
//        }


    private fun loadImage(filename: String) = Image(display, this.javaClass.classLoader.getResourceAsStream(filename))


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
                while (scale.selection < w.commandStack.stackTop)
                    w.commandStack.undo()
                while (scale.selection > w.commandStack.stackTop) {
                    w.commandStack.redo()
                }
            }
        })
        w.commandStack.addObserver { command, exec ->
            if (w.commandStack.stackTop >= 0) {
                scale.enabled = true
                scale.maximum = w.commandStack.stackSize - 1
                scale.selection = w.commandStack.stackTop
            } else {
                scale.enabled = false
            }
            scale.requestLayout()
        }
    }
}

