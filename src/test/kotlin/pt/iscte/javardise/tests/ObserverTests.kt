package pt.iscte.javardise.tests

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.PrimitiveType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ObserverTests : SWTTest(
    ClassOrInterfaceDeclaration(NodeList(), false, "Test")
) {
    val finalCode = """
        class AutoTest {
            static int fact(int n) {
                if(n == 1) {
                    return 1;
                }
                else {
                    return n * fact(n-1);
                }
            }
        }
    """

    @Test
    fun writeFactorial() {
        val steps: List<(Node?)-> Node> = listOf(
            { classModel.name = SimpleName("AutoTest"); classModel},
            {
                (it as ClassOrInterfaceDeclaration).addMethod("method")
            },
            {
                (it as MethodDeclaration).name = SimpleName("fact"); it
            },
            {
                (it as MethodDeclaration).type = PrimitiveType(PrimitiveType.Primitive.INT); it
            },
            {
                (it as MethodDeclaration).modifiers.add(Modifier.staticModifier()); it
            },
            {
                (it as MethodDeclaration).addParameter(Parameter(PrimitiveType(PrimitiveType.Primitive.INT), "i")); it
            },
            {
                (it as MethodDeclaration).parameters.removeAt(0); it
            },
            {
                (it as MethodDeclaration).addParameter(Parameter(PrimitiveType(PrimitiveType.Primitive.INT), "n")); it
            },
            {
                val iff = IfStmt(BinaryExpr(NameExpr("n"),
                    IntegerLiteralExpr("1"),
                    BinaryExpr.Operator.EQUALS), BlockStmt(), null)
                (it as MethodDeclaration).body.get().statements.add(iff); iff
            },
            {
                (it as IfStmt).thenStmt.asBlockStmt().statements.add(
                    ReturnStmt(NameExpr("1"))
                ); it
            },
            {
                (it as IfStmt).setElseStmt(BlockStmt())
            },
            {
                val ret = ReturnStmt(NameExpr("n"))
                (it as IfStmt).elseStmt.get().asBlockStmt().statements.add(
                    ret
                ); ret;
            },
            {
                (it as ReturnStmt).removeExpression()
            },
            {
                (it as ReturnStmt).setExpression(
                    BinaryExpr(NameExpr("n"),
                        MethodCallExpr("fact",
                        BinaryExpr(NameExpr("n"), IntegerLiteralExpr("1"),
                            BinaryExpr.Operator.MINUS)), BinaryExpr.Operator.MULTIPLY)
                )
                it
            }
        )

        var prev:Node? = null
        steps.forEachIndexed { i, a ->
            step {
                try {
                    prev = a(prev)
                }
                catch (e: Exception) {
                    fail(classModel.toString())
                }
            }
        }

        step {
            val parse = StaticJavaParser.parse(finalCode)
            assertEquals(parse.types[0], classModel)
        }
        terminate()
    }

}