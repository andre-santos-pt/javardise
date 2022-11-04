package pt.iscte.javardise.examples

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.eclipse.swt.widgets.List
import pt.iscte.javardise.Commands
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.FileFilter
import java.util.WeakHashMap
import javax.lang.model.SourceVersion


// TODO folder or dialog
fun main(args: Array<String>) {
    val file = if (args.isEmpty()) File(System.getProperty("user.dir")) else File(args[0])
    val window = JavardiseClassicIDE(file)
    window.open()
}

class JavardiseClassicIDE(val folder: File) {

    private val display = Display()
    private val shell = Shell(display)

    val openTabs = mutableListOf<Composite>()
    val focusMap = WeakHashMap<Composite, Control>()

    init {
        require(folder.exists() && folder.isDirectory)

        shell.layout = FillLayout()
        shell.text = folder.absolutePath
        val form = SashForm(shell, SWT.HORIZONTAL)

        val fileList = List(form, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL)
        val fileArea = Composite(form, SWT.BORDER)
        val stacklayout = StackLayout()
        fileArea.layout = stacklayout

        fileList.addSelectionListener(object: SelectionAdapter() {
            var current = -1
            override fun widgetSelected(e: SelectionEvent) {
                if(fileList.selection.isNotEmpty() && current != fileList.selectionIndex)  {
                    current = fileList.selectionIndex
                    val find = openTabs.find { it.data == fileList.selection.first() }
                    if(find != null) {
                        stacklayout.topControl = find
                        fileArea.layout()
                        if(focusMap.containsKey(find))
                            focusMap[find]?.setFocus()
                    }
                    else {
                        val tab = createTab(fileList, fileArea)
                        openTabs.add(tab)
                        stacklayout.topControl = tab
                        fileArea.layout()
                    }
                }
            }
        })

        folder.listFiles(FileFilter { it.name.endsWith(".java") })?.forEach {
            fileList.add(it.name)
        }


        // BUG lost focus
        display.addFilter(SWT.KeyDown) {
            if (it.stateMask == SWT.MOD1 && it.keyCode == 'z'.code) {
                println("undo")
                Commands.undo()
            }
        }
    }

    private fun createTab(
        list: List,
        comp: Composite
    ): Composite {
        val fileName = list.selection.first()
        val tab = Composite(comp, SWT.CLOSE)
        val file = File(folder.absoluteFile, fileName)
        val typeName = file.name.dropLast(".java".length)
        val model = if (file.exists()) {
            val cu = loadCompilationUnit(file)
            cu.findMainClass() ?: ClassOrInterfaceDeclaration(
                NodeList(), false, typeName
            )
        } else {
            val cu = CompilationUnit()
            cu.addClass(typeName)
        }
        tab.layout = GridLayout()

        val w = tab.scrollable {
            ClassWidget(it, model)
        }
        w.addFocusObserver { control ->
           focusMap[tab] = control
        }
        //w.setAutoScroll()
        //w.layoutData = GridData(GridData.FILL_BOTH)
        tab.data = fileName
        return tab
    }

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
        Commands.reset()
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





