import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.util.*
import javax.tools.*
import javax.tools.JavaCompiler.CompilationTask


fun compile(cuName: String, src: String) : List<Diagnostic<*>> {
    val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val writer = StringWriter()
    val out = PrintWriter(writer)
    out.println(src)
//    out.println("public class HelloWorld {")
//    out.println("  public static void main(String args[]) {")
//    out.println("    ;")
//    out.println("    System.out.println(\"This is in another java file\");")
//    out.println("  }")
//    out.println("}")
    out.close()
    val file: JavaFileObject = JavaSourceFromString(cuName, writer.toString())
    val compilationUnits: Iterable<JavaFileObject> = listOf(file)
    val task: CompilationTask = compiler.getTask(null, null, diagnostics, null, null, compilationUnits)
    val success = task.call()
//    for (diagnostic in diagnostics.diagnostics) {
//        println(diagnostic.code)
//        println(diagnostic.kind)
//        println(diagnostic.position)
//        println(diagnostic.startPosition)
//        println(diagnostic.endPosition)
//        println(diagnostic.source)
//        println(diagnostic.getMessage(null))
//    }
    println("Success: $success")
    if (success) {
        try {
            Class.forName("HelloWorld").getDeclaredMethod("main", *arrayOf<Class<*>>(Array<String>::class.java))
                .invoke(null, *arrayOf(null))
        } catch (e: ClassNotFoundException) {
            System.err.println("Class not found: $e")
        } catch (e: NoSuchMethodException) {
            System.err.println("No such method: $e")
        } catch (e: IllegalAccessException) {
            System.err.println("Illegal access: $e")
        } catch (e: InvocationTargetException) {
            System.err.println("Invocation target: $e")
        }
    }
    return diagnostics.diagnostics
}


internal class JavaSourceFromString(name: String, val code: String) :
    SimpleJavaFileObject(
        URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
        JavaFileObject.Kind.SOURCE
    ) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return code
    }
}

fun compile(file: File) : List<Diagnostic<*>>{
    val compiler = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val fileManager = compiler.getStandardFileManager(diagnostics, null, null)
    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(listOf(file))
    val task = compiler.getTask(
        null, fileManager, diagnostics, null,
        null, compilationUnits
    )
    val success = task.call()
    fileManager.close()
    println("Success: $success")
    return diagnostics.diagnostics
}
