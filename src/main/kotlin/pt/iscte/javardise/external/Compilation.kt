package pt.iscte.javardise.external

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
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
import pt.iscte.javardise.basewidgets.addNote
import pt.iscte.javardise.findChild
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.io.PrintWriter
import java.io.StringWriter
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

    for (i in items) {
        val writer = StringWriter()
        val out = PrintWriter(writer)
        out.println(i.toString())
        out.close()
        compilationUnits.add(JavaSource(i))
    }
    val task: CompilationTask =
        compiler.getTask(null, null, diagnostics, null, null, compilationUnits)
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
    val fileManager = compiler.getStandardFileManager(diagnostics, null, null)
    val compilationUnits =
        fileManager.getJavaFileObjectsFromFiles(listOf(*files))
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
    }
}

fun <K, V> MutableMap<K, MutableList<V>>.putPair(key: K, value: V) {
    if(containsKey(key))
        get(key)?.add(value)
    else
        put(key, mutableListOf(value))
}


fun buildNodeSourceMap(model: ClassOrInterfaceDeclaration) : MutableList<Token> {
    val tokenList = mutableListOf<Token>()
    val printer = DefaultPrettyPrinter(
        { TokenVisitor(tokenList, it) },
        DefaultPrinterConfiguration()
    )
    printer.print(model)
    return tokenList
}

//data class CompileError(val type: ClassOrInterfaceDeclaration, val errors: List<ICodeDecoration<*>>)
typealias CompileErrors = MutableMap<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>
fun checkCompileErrors(models: List<Pair<ClassOrInterfaceDeclaration, ClassWidget?>>) : CompileErrors {
    val errorDecs = mutableMapOf<ClassOrInterfaceDeclaration, MutableList<ICodeDecoration<*>>>()
    val nodeMap = mutableMapOf<ClassOrInterfaceDeclaration, MutableList<Token>>()
    for (i in models) {
        val model = i.first
        nodeMap[model] = buildNodeSourceMap(model)
    }
    val errors = compile(models.map { it.first })
    for (e in errors) {

        println("${(e.source as JavaSource).name} ERROR line ${e.lineNumber} ${e.columnNumber} ${e.kind} ${e.code} ${
                e.getMessage(
                    null
                )
            }"
        )
        // zero-based in java compiler
        val m = (e.source as JavaSource).model
        val t = nodeMap[m]?.find { it.line == e.lineNumber && it.col == e.columnNumber }
        val widget = models.find { it.first == m}?.second
        t?.let {
            val child = widget?.findChild(t.node)
            child?.let {
                errorDecs.putPair(m, child.addMark(widget.configuration.errorColor))
                //child.addNote(e.getMessage(null), ICodeDecoration.Location.TOP).show()
            }
            // TODO errors not mapped
        }

    }
    return errorDecs

}

