package pt.iscte.javardise.compilation

import com.github.javaparser.ast.Modifier
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream


class RunAction : Action {
    override val name: String = "Run main method"

    override val iconPath: String = "play.png"

    override fun isEnabled(editor: CodeEditor): Boolean =
        compileNoOutput(editor.folder).first.isEmpty() &&
                editor.classOnFocus?.node?.methods?.any {
                    it.modifiers.contains(Modifier.staticModifier()) &&
                            it.type.isVoidType &&
                            it.nameAsString == "main"
                } ?: false

    override fun run(editor: CodeEditor, toggle: Boolean) {
        val compilation = compileNoOutput(editor.folder)
        println(compilation.second)
        val classLoader: ClassLoader = ByteArrayClassLoader(compilation.second)

        val mainClass = classLoader.loadClass(editor.classOnFocus?.node?.nameAsString)
        val mainMethod = mainClass.declaredMethods.find {
            it.name == "main" && java.lang.reflect.Modifier.isStatic(it.modifiers)
        }
        mainMethod?.trySetAccessible()
        val outputStream = ByteArrayOutputStream()
        val customPrintStream = Interceptor(outputStream,editor)
        val originalSystemOut = System.out
        System.setOut(customPrintStream)
        // TODO infinite loop
        System.setProperty("user.dir", editor.folder.absolutePath)
        mainMethod?.invoke(null)
        System.setOut(originalSystemOut)
    }
}


private class ByteArrayClassLoader(val byteCodes: Map<String, ByteArray>) : ClassLoader() {
    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        if (!byteCodes.containsKey(name))
            throw ClassNotFoundException(name)

        return defineClass(name, byteCodes[name], 0, byteCodes[name]!!.size)
    }
}

private class Interceptor(out: OutputStream, val editor: CodeEditor) :
    PrintStream(out, true) {

        // TODO other types
    override fun print(s: String?) {
        editor.console(s.toString())
    }

    override fun print(i: Int) {
        editor.console(i.toString())
    }

    override fun println(i: Int) {
        editor.console(i.toString()+ System.lineSeparator())
    }

    override fun println(s: String?) {
        editor.console(s.toString() + System.lineSeparator())
    }
}
