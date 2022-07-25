import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.GenericVisitor
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import java.io.File

fun main() {
    val cu = StaticJavaParser.parse("class A {}")
    val a = cu.getClassByName("A")
    a.get().addField("int", "i")

    val solver = JavaParserTypeSolver(File("src"))

    val src = StaticJavaParser.parse(File("src/main/kotlin/TestExample.java"))
    val dec : ClassOrInterfaceDeclaration = src.getClassByName(src.primaryTypeName.get()).get()
    val me = dec.methods[0].body.get().statements[1]
    val clone = dec.methods[0].body.get().statements[1].clone()
    dec.members.register(object : AstObserverAdapter() {
        override fun listChange(
            observedNode: NodeList<*>?,
            type: AstObserver.ListChangeType?,
            index: Int,
            nodeAddedOrRemoved: Node?
        ) {
            println("NEW " + nodeAddedOrRemoved)
        }
    })
    val m = dec.addMethod("m3", Modifier.Keyword.PRIVATE)
    m.createBody().addStatement("return 3;")


    m.name = SimpleName("m4")
    dec.methods.forEach {
        println(it)

    }
  //

    dec.accept(object : VoidVisitorAdapter<Any>() {
        override fun visit(n: MethodDeclaration?, arg: Any?) {
            println("DEC ${n!!::class}")
            super.visit(n, arg)
        }
        override fun visit(n: MethodCallExpr?, arg: Any?) {
            val solve: SymbolReference<ResolvedMethodDeclaration> = JavaParserFacade.get(solver).solve(n)
            val typeOfTheNode: ResolvedType? = JavaParserFacade.get(solver).getType(n)
            println("!!" + solve.correspondingDeclaration.toAst().get())
        }
    }, null)
}
