package pt.iscte.javardise.examples

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.widgets.Composite
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.DefaultConfigurationSingleton
import pt.iscte.javardise.widgets.members.ClassWidget
import pt.iscte.javardise.widgets.members.MethodWidget


class StaticClassWidget(
    parent: Composite,
    dec: ClassOrInterfaceDeclaration,
    configuration: Configuration = DefaultConfigurationSingleton,
) : ClassWidget(parent, dec, configuration, staticClass = true) {

    init {
        if(node.members.isEmpty())
             bodyWidget.insertBeginning()
    }

    override fun customizeNewMethodDeclaration(dec: MethodDeclaration) {
        dec.addModifier(Modifier.Keyword.PUBLIC)
        dec.addModifier(Modifier.Keyword.STATIC)
    }

    override fun createMethodWidget(dec: CallableDeclaration<*>) =
        MethodWidget(
            bodyWidget,
            dec,
            emptyList(),
            configuration = configuration,
            commandStack = commandStack
        )
}