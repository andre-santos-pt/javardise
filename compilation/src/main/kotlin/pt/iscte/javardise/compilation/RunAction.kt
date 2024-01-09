package pt.iscte.javardise.compilation

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream


class RunAction : Action {
    override val name: String = "Run main method"

    override val iconPath: String = "play.png"

    private fun MethodDeclaration.isMain() =
        nameAsString == "main" &&
        modifiers.contains(Modifier.staticModifier()) &&
                type.isVoidType &&
                (parameters.isEmpty() || parameters.size == 1 && parameters[0].typeAsString == "String[]")


    override fun isEnabled(editor: CodeEditor): Boolean =
        compileNoOutput(editor.folder).first.isEmpty() &&
                editor.classOnFocus?.node?.methods?.any { it.isMain() } ?: false

    override fun run(editor: CodeEditor, toggle: Boolean) {
        val compilation = compileNoOutput(editor.folder)
        val classLoader: ClassLoader = ByteArrayClassLoader(compilation.second)

        val mainClass = classLoader.loadClass(editor.classOnFocus?.node?.nameAsString)
        val mainMethod = mainClass.declaredMethods.find {
            it.name == "main" && java.lang.reflect.Modifier.isStatic(it.modifiers) && it.returnType.name == "void"
        }

        if(mainMethod != null) {
            mainMethod.trySetAccessible()
            val outputStream = ByteArrayOutputStream()
            val customPrintStream = Interceptor(outputStream, editor)
            val originalSystemOut = System.out
            System.setOut(customPrintStream)
            // TODO infinite loop
            System.setProperty("user.dir", editor.folder.absolutePath)
            editor.consoleClear()
            if (mainMethod.parameters?.isEmpty() == true)
                mainMethod.invoke(null)
            else
                mainMethod.invoke(null, emptyArray<String>())

            System.setOut(originalSystemOut)
        }
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
        editor.consoleAppend(s.toString())
    }

    override fun print(i: Int) {
        editor.consoleAppend(i.toString())
    }

    override fun println(i: Int) {
        editor.consoleAppend(i.toString()+ System.lineSeparator())
    }

    override fun println(s: String?) {
        editor.consoleAppend(s.toString() + System.lineSeparator())
    }
}
