
import java.io.OutputStream
import java.net.URI
import javax.tools.*

class CompilationResult(val messages: List<Diagnostic<*>>) {

    val success: Boolean
        get() = messages.none { it.kind == Diagnostic.Kind.ERROR }

    val fail: Boolean
        get() = !success

    companion object {
        fun compile(
            className: String,
            code: String
        ): CompilationResult {
            val src = object : SimpleJavaFileObject(
                URI.create(
                    "string:///" + className + "." + JavaFileObject.Kind.SOURCE.extension
                ),
                JavaFileObject.Kind.SOURCE
            ) {
                override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                    return code
                }

                override fun openOutputStream(): OutputStream = OutputStream.nullOutputStream()

            }

            val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
            val diagnostics = DiagnosticCollector<JavaFileObject>()
            val compilationUnits: List<JavaFileObject> = listOf(src)
            val task = compiler.getTask(
                null,
                null,
                diagnostics,
                listOf("-implicit:none"),  // AINDA NAO CONSEGUI QUE A COMPILACAO NAO GERASSE FICHEIROS .class ....
                null,
                compilationUnits
            )
            task.call()
            return CompilationResult(diagnostics.diagnostics)
        }
    }
}


fun main() {
    val comp = CompilationResult.compile("Solution", """
        class Solution {
          static void replaceFirst(int a, int b, int[] array) {
            for(int i = 0; i < array.length; i++) {
                if(array[i] == a) {
                  array[i] = b;
                  break;
                }
            }
          }
        }
    """)
    println("compiles: " + comp.success)
    comp.messages.forEach {
        println(it.getMessage(null) +
                "\n\tline/col/length: " + it.lineNumber + " : " + it.columnNumber + " : " + (it.endPosition - it.startPosition))
    }
}