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
import pt.iscte.javardise.findChild
import pt.iscte.javardise.widgets.members.ClassWidget
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import javax.tools.*
import javax.tools.JavaCompiler.CompilationTask


data class CompilationItem(val name: String, val src: String)

fun compile(items: List<CompilationItem>): List<Diagnostic<*>> {
    val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val compilationUnits: MutableList<JavaFileObject> = mutableListOf()

    for (i in items) {
        val writer = StringWriter()
        val out = PrintWriter(writer)
        out.println(i.src)
        out.close()
        val file: JavaFileObject =
            JavaSourceFromString(i.name, writer.toString())
        compilationUnits.add(file)
    }
    val task: CompilationTask =
        compiler.getTask(null, null, diagnostics, null, null, compilationUnits)
    val success = task.call()
    println("Success: $success")
    return diagnostics.diagnostics
}


internal class JavaSourceFromString(name: String, val code: String) :
    SimpleJavaFileObject(
        URI.create(
            "string:///" + name.replace(
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

fun checkErrors(models: List<Pair<ClassOrInterfaceDeclaration, ClassWidget>>) {
    val nodeMap =
        mutableMapOf<ClassOrInterfaceDeclaration, MutableList<Token>>()
    for (i in models) {
        val model = i.first
        val widget = i.second

        val tokenList = mutableListOf<Token>()
        val printer = DefaultPrettyPrinter(
            { TokenVisitor(tokenList, it) },
            DefaultPrinterConfiguration()
        )
        val src = printer.print(model)
    }
    val errors = compile(models.map {
        CompilationItem(
            it.first.nameAsString,
            it.toString()
        )
    })
    for (e in errors) {

        println(
            "ERROR line ${e.lineNumber} ${e.columnNumber} ${
                e.getMessage(
                    null
                )
            }"
        )
//        // zero-based in java compiler
//        val t =
//            tokenList.find { it.line == e.lineNumber && it.col == e.columnNumber }
//        t?.let {
//            val child = widget.findChild(t.node)
//            child?.let {
//                child.traverse {
//                    it.background = widget.configuration.errorColor
//                    true
//                }
//                child.toolTipText = e.getMessage(null)
//                child.requestLayout()
//            }
//        }
        // TODO show not handled
    }

}

