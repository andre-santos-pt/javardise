package pt.iscte.javardise.examples

import com.github.javaparser.ParseProblemException
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.eclipse.swt.widgets.List
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.FileFilter
import java.io.PrintWriter
import java.util.WeakHashMap
import javax.lang.model.SourceVersion


// TODO folder or dialog
fun main(args: Array<String>) {
    val file =
        if (args.isEmpty()) File(System.getProperty("user.dir")) else File(args[0])
    val window = JavardiseClassicEditor(file)
    window.open()
}

class JavardiseClassicEditor(val folder: File) {

    private val display = Display()
    private val shell = Shell(display)

    val openTabs = mutableListOf<Composite>()
    val focusMap = WeakHashMap<Composite, Control>()
    val stacklayout = StackLayout()

    val classOnFocus: ClassWidget?
        get() = if (stacklayout.topControl == null)
            null
        else
            (stacklayout.topControl.data as TabData).classWidget

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
                        if (focusMap.containsKey(find))
                            focusMap[find]?.setFocus()
                    } else {
                        val tab = createTab(
                            File(
                                folder.absoluteFile,
                                fileList.selection.first()
                            ), fileList, editorArea
                        )
                        openTabs.add(tab)
                        stacklayout.topControl = tab
                        editorArea.layout()
                    }
                }
            }
        })

        folder.listFiles(FileFilter { it.name.endsWith(".java") })?.forEach {
            fileList.add(it.name)
        }


        val menu = Menu(fileList)
        val newFile = MenuItem(menu, SWT.PUSH)
        newFile.text = "New file"
        newFile.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                prompt("File name") {
                    val f = File(folder, it + ".java")
                    f.createNewFile()
                    fileList.add(f.name)
                    fileList.requestLayout()
                    fileList.select(fileList.itemCount)
                }
//                val newTab = createTab(f, fileList, editorArea)
//                stacklayout.topControl = newTab
//                editorArea.layout()

            }
        })
        fileList.menu = menu

        // BUG lost focus
        display.addFilter(SWT.KeyDown) {
            if (it.stateMask == SWT.MOD1 && it.keyCode == 'z'.code) {
                println("undo")
                val cmd = classOnFocus?.commandStack
                cmd?.undo()
            }
        }
    }

    private fun createTab(
        file: File,
        list: List,
        comp: Composite
    ): Composite {
        val tab = Composite(comp, SWT.BORDER)
        val layout = FillLayout()
        layout.marginHeight = 10
        layout.marginWidth = 10
        tab.layout = layout

        val typeName = file.name.dropLast(".java".length)
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

        if(model == null) {
            // tab.scrollable {
            tab.data = TabData(file, null)
            tab.fill {
                val src = file.readLines().joinToString(separator = "\n")
                label(src).foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED)
            }
            //}
        }
        else {
            val w = tab.scrollable {
                ClassWidget(it, model)
            }
            // TODO not working
            w.addFocusObserver { control ->
                focusMap[tab] = control
            }
            w.setAutoScroll()

            w.commandStack.addObserver { _, _ ->
                val writer = PrintWriter(file)
                writer.println(model.toString())
                writer.close()

                checkCompileErrors(listOf(
                    Pair(model, w)
                ))
            }
            tab.data = TabData(file, w)
        }
        return tab
    }

    data class TabData(val file: File, val classWidget: ClassWidget?)

    fun notFoundLabel(p: Composite) = Composite(p, SWT.BORDER).apply {
        layout = FillLayout()
        Label(this, SWT.NONE).apply {
            text = "No public class found"
        }
    }


    private fun load(file: File): ClassOrInterfaceDeclaration {
        require(file.name.endsWith(".java")) {
            "Java file must have '.java' extension"
        }
        val typeName = file.name.dropLast(".java".length)

        require(SourceVersion.isIdentifier(typeName)) {
            "'$typeName' is not a valid identifier for a Java type"
        }

        val model = if (file.exists()) {
            val cu = loadCompilationUnit(file)
            cu.findMainClass() ?: ClassOrInterfaceDeclaration(
                NodeList(), false, typeName
            )
        } else {
            val cu = CompilationUnit()
            cu.addClass(typeName)
        }

        model.observeProperty<SimpleName>(ObservableProperty.NAME) {
            shell.text = it?.toString() ?: "No public class found"
        }
        shell.requestLayout()
        return model
    }


    fun open() {
        shell.size = Point(600, 800)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }
}





