package pt.iscte.javardise.editor

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

interface Action {
    val name: String
    val iconPath: String?
        get() = null
    val toggle: Boolean
        get() = false

    fun init(editor: CodeEditor) {}
    fun run(model: ClassOrInterfaceDeclaration?, toggle: Boolean)
}


//interface Plugin {
//    val actions: kotlin.collections.List<Action>
//
//    fun modelChange(model: ClassOrInterfaceDeclaration)
//
//    fun selectionChange(node: Node)
//}