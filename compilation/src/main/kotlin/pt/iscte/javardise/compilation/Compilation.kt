package pt.iscte.javardise.compilation

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.printer.configuration.PrinterConfiguration
import pt.iscte.javardise.basewidgets.ICodeDecoration
import pt.iscte.javardise.basewidgets.addMark
import pt.iscte.javardise.findChild
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.*
import java.net.URI
import javax.tools.*
import javax.tools.JavaCompiler.CompilationTask


data class CompilationItem(val name: String, val src: String)

fun compile(folder: File) {
    require(folder.exists() && folder.isDirectory)
    folder.listFiles(FileFilter { it.name.endsWith(".java") })?.forEach {
    }
}

fun compile(items: List<ClassOrInterfaceDeclaration>): List<Diagnostic<*>> {
    val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val compilationUnits: MutableList<JavaFileObject> = mutableListOf()
    val standardFileManager = compiler.getStandardFileManager(null, null, null)
    val fileManager = MemoryFileManager(standardFileManager)

    for (i in items) {
        val writer = StringWriter()
        val out = PrintWriter(writer)
        out.println(i.toString())
        out.close()
        compilationUnits.add(JavaSource(i))
    }
    val task: CompilationTask =
        compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits)
    val success = task.call()
    println("Success: $success")
    return diagnostics.diagnostics
}


internal class JavaSourceFromString(val filename: String, val code: String) :
    SimpleJavaFileObject(
        URI.create(
            "string:///" + filename.replace(
                '.',
                '/'
            ) + JavaFileObject.Kind.SOURCE.extension
        ),
        JavaFileObject.Kind.SOURCE
    ) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return code
    }
}

internal class JavaSource(val model: ClassOrInterfaceDeclaration) :
    SimpleJavaFileObject(
        URI.create(
            "string:///" + model.nameAsString.replace(
                '.',
                '/'
            ) + JavaFileObject.Kind.SOURCE.extension
        ),
        JavaFileObject.Kind.SOURCE
    ) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return model.toString()
    }
}

fun compile(vararg files: File): List<Diagnostic<*>> {
    val compiler = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val standardFileManager = compiler.getStandardFileManager(null, null, null)
    val fileManager = MemoryFileManager(standardFileManager)

    val compilationUnits = standardFileManager.getJavaFileObjectsFromFiles(listOf(*files))
    val task = compiler.getTask(
        null, fileManager, diagnostics, null,
        null, compilationUnits
    )
    val success = task.call()
    fileManager.close()
    println("Success: $success")
    return diagnostics.diagnostics
}

data class Token(val line: Long, val col: Long, val offset: Int, val node: Node)

class TokenVisitor(val list: MutableList<Token>, conf: PrinterConfiguration) :
    DefaultPrettyPrinterVisitor(conf) {
    override fun visit(n: FieldDeclaration?, arg: Void?) {
        //println("F " + n)
        super.visit(n, arg)
    }

    override fun visit(n: SimpleName, arg: Void?) {
        super.visit(n, arg)
        addToken(n.parentNode.get())
        //println("V $n ${n.parentNode}")
    }

    override fun visit(n: Parameter, arg: Void?) {
        super.visit(n, arg)
        //println("P ${n.name} ${n.name.hashCode()} ${n.type}")
    }

    override fun visit(n: NameExpr, arg: Void?) {
        super.visit(n, arg)
        addToken(n)
    }

    override fun visit(n: ArrayAccessExpr, arg: Void?) {
        super.visit(n, arg)
        addToken(n)
    }

    override fun visit(m: MethodDeclaration, arg: Void?) {
        super.visit(m, arg)
        // addToken(m.type)
        // addToken(m.name)
        addToken(m)
    }

    override fun visit(n: DoubleLiteralExpr, arg: Void?) {
        super.visit(n, arg)
        addToken(n)
    }

    private fun addToken(n: Node) {
        val offset = n.toString().length
        val init =
            printer.cursor.column - offset + 1 // because cursor.column is zero-based
        val pos = Token(
            printer.cursor.line.toLong(),
            init.toLong(),
            offset,
            n
        )
        list.add(pos)
        println("$pos - ${pos.node.hashCode()}")
    }
}

fun <K, V> MutableMap<K, MutableList<V>>.putPair(key: K, value: V) {
    if (containsKey(key))
        get(key)?.add(value)
    else
        put(key, mutableListOf(value))
}


fun buildNodeSourceMap(model: ClassOrInterfaceDeclaration): MutableList<Token> {
    val tokenList = mutableListOf<Token>()
    val printer = DefaultPrettyPrinter(
        { TokenVisitor(tokenList, it) },
        DefaultPrinterConfiguration()
    )
    printer.print(model)
    return tokenList
}

typealias CompileErrors = MutableMap<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>

fun checkCompileErrors(models: List<Pair<ClassOrInterfaceDeclaration, ClassWidget>>): CompileErrors {
    if (models.isEmpty())
        return mutableMapOf()

    val errorDecs =
        mutableMapOf<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>()
    val nodeMap =
        mutableMapOf<ClassOrInterfaceDeclaration, MutableList<Token>>()
    for (i in models) {
        val model = i.first
        nodeMap[model] = buildNodeSourceMap(model)
    }
    val errors = compile(models.map { it.first })
    for (e in errors) {

        println(
            "${(e.source as JavaSource).name} ERROR line ${e.lineNumber} ${e.columnNumber} ${e.kind} ${e.code} ${
                e.getMessage(
                    null
                ) 
            }"
        )
        // zero-based in java compiler
        val m = (e.source as JavaSource).model
        val t =
            nodeMap[m]?.find { it.line == e.lineNumber && it.col == e.columnNumber }
        val widget = models.find { it.first == m }?.second
        if (t != null) {
            val child = widget?.findChild(t.node)
            if (child != null) {
                errorDecs.putPair(
                    m,
                    child.addMark(
                        widget.configuration.errorColor,
                        e.getMessage(null)
                    )
                )
                //child.addNote(e.getMessage(null), ICodeDecoration.Location.TOP).show()
            } else {
                errorDecs.putPair(
                    m,
                    widget!!.addMark(
                        widget.configuration.errorColor,
                        e.getMessage(null)
                    )
                )
            }
        } else {
            errorDecs.putPair(
                m,
                widget!!.addMark(
                    widget.configuration.errorColor,
                    e.getMessage(null)
                )
            )
        }


    }
    return errorDecs

}

internal class MemoryFileManager(fileManager: JavaFileManager?) :
    ForwardingJavaFileManager<JavaFileManager?>(fileManager) {
    private val classBytes: MutableMap<String, ByteArrayOutputStream> =
        HashMap()

    @Throws(IOException::class)
    override fun getJavaFileForOutput(
        location: JavaFileManager.Location,
        className: String,
        kind: JavaFileObject.Kind,
        sibling: FileObject
    ): JavaFileObject {

        return MemoryJavaFileObject(className, kind)
    }

    val classes: Map<String, ByteArray>
        get() {
            val classMap: MutableMap<String, ByteArray> = HashMap()
            for (className in classBytes.keys) {
                classMap[className] = classBytes[className]!!.toByteArray()
            }
            return classMap
        }

    private inner class MemoryJavaFileObject  constructor(
        private val name: String, kind: JavaFileObject.Kind
    ) :
        SimpleJavaFileObject(
            URI.create(
                "string:///" +
                name.replace(
                    "\\.".toRegex(),
                    "/"
                ) + kind.extension
            ), kind
        ) {
        private val byteCode: ByteArrayOutputStream = ByteArrayOutputStream()

        @Throws(IOException::class)
        override fun openOutputStream(): ByteArrayOutputStream {
            return byteCode
        }

        override fun delete(): Boolean {
            classBytes.remove(name)
            return true
        }

        val bytes: ByteArray
            get() = byteCode.toByteArray()
    }
}