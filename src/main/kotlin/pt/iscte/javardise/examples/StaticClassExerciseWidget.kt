package pt.iscte.javardise.examples

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.FileNotFoundException


val testCode = """
        public class StaticClass {
        
        }
    """.trimIndent()
fun main(args: Array<String>) {
    val display = Display()
    val shell = Shell(display)
    shell.layout = FillLayout()
    val w = StaticClassExerciseWidget(shell,
        StaticJavaParser.parse(testCode).types[0] as ClassOrInterfaceDeclaration, DefaultConfiguration())

    shell.open()
    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
internal class StaticClassExerciseWidget(
    parent: Composite?,
    dec: ClassOrInterfaceDeclaration,
    conf: Configuration
) : Composite(parent, SWT.NONE) {




    internal inner class StaticClassWidget(
        parent: Composite,
        dec: ClassOrInterfaceDeclaration,
        conf: Configuration
    ) : ClassWidget(
        parent, dec, conf, staticClass =  true
    ) {
        override fun customizeNewMethodDeclaration(dec: MethodDeclaration) {
            dec.addModifier(Modifier.Keyword.PUBLIC)
            dec.addModifier(Modifier.Keyword.STATIC)
        }
    }

    var classWidget: StaticClassWidget

    init {
        layout = GridLayout()
        dec.comment.ifPresent { c: Comment ->
            val group = Group(this, SWT.NONE)
            group.text = "Goal"
            group.layout = GridLayout()
            val label = Text(group, SWT.MULTI or SWT.READ_ONLY or SWT.WRAP)
            label.text = c.content
        }
        val code = Group(this, SWT.NONE)
        code.text = "Code"
        code.layout = GridLayout()
        code.layoutData = GridData(GridData.FILL_HORIZONTAL)
        code.font = conf.font

        classWidget = StaticClassWidget(code, dec, conf)

        classWidget.layoutData = GridData(GridData.FILL_HORIZONTAL)

        classWidget.bodyWidget.insertBeginning()

        val tests = Group(this, SWT.NONE)
        tests.text = "Tests"
        tests.layout = GridLayout()
        val table = Table(tests, SWT.NONE)
        table.headerVisible = true
        val col1 = TableColumn(table, SWT.NONE)
        col1.text = "Invocation"
        val col2 = TableColumn(table, SWT.NONE)
        col2.text = "Expected"
        val item = TableItem(table, SWT.None)
        item.setText(arrayOf("f(1)", "0"))
        val item2 = TableItem(table, SWT.None)
        item2.setText(arrayOf("f(2)", "1"))
    }


}