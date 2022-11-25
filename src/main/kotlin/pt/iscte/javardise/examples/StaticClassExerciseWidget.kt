package pt.iscte.javardise.examples

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.CommandStack.Companion.create
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.widgets.members.ClassWidget

internal class StaticClassExerciseWidget(
    parent: Composite?,
    dec: ClassOrInterfaceDeclaration,
    conf: Configuration
) : Composite(parent, SWT.NONE) {
    internal inner class CustomClassWidget(
        arg0: Composite?,
        arg1: ClassOrInterfaceDeclaration?,
        arg2: Configuration?,
        arg3: CommandStack?,
        arg4: Boolean
    ) : ClassWidget(
        arg0!!, arg1!!, arg2!!, arg3!!, arg4
    ) {
        override fun customizeNewMethodDeclaration(dec: MethodDeclaration) {
            dec.addModifier(Modifier.Keyword.STATIC)
        }
    }

    var classWidget: CustomClassWidget

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

//			MethodWidget w = new MethodWidget(group, dec.addMe, SWT.NONE, new Conf(), CommandStack.Companion.create());

        // MethodWidget w = new MethodWidget(group, dec.getMethods().get(0), SWT.NONE,
        // new Conf(), CommandStack.Companion.create());
        classWidget = CustomClassWidget(code, dec, conf, create(), true)
        classWidget.layoutData = GridData(GridData.FILL_HORIZONTAL)
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