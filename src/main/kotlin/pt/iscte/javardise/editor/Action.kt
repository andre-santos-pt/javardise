package pt.iscte.javardise.editor

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File

interface Action {
    val name: String
    val iconPath: String?
        get() = null
    val toggle: Boolean
        get() = false

    fun isEnabled(facade: Facade): Boolean = true

    fun init(editor: CodeEditor) {}
    fun run(facade: Facade, toggle: Boolean)
}

interface Facade {
    val file: File?
    val model: ClassOrInterfaceDeclaration?
    val classWidget: ClassWidget?

}

//interface Plugin {
//    val actions: kotlin.collections.List<Action>
//
//    fun modelChange(model: ClassOrInterfaceDeclaration)
//
//    fun selectionChange(node: Node)
//}