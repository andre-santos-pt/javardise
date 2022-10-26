package pt.iscte.javardise.tests

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.VoidType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.javardise.Commands
import pt.iscte.javardise.addCommand
import pt.iscte.javardise.modifyCommand

class CommandTests : SWTTest(
    ClassOrInterfaceDeclaration(NodeList(), false, "Test")
) {

    @Test
    fun testAuto() {
        val actions: List<(Node)-> Node> = listOf(
            { classModel.modifyCommand(classModel.name, SimpleName("AutoTest"), classModel::setName); classModel},
            {
                val m = MethodDeclaration(NodeList(), VoidType(),"method")
                classModel.members.addCommand(classModel, m); m
            },
            { classModel.modifyCommand(classModel.name, SimpleName("AutoTest2"), classModel::setName); classModel},
//            {
//                (it as MethodDeclaration).name = SimpleName("fact"); it
//            },
//            {
//                (it as MethodDeclaration).type = PrimitiveType(PrimitiveType.Primitive.INT); it
//            },
//            {
//                (it as MethodDeclaration).addParameter(Parameter(PrimitiveType(PrimitiveType.Primitive.INT), "i")); it
//            },
//            {
//                (it as MethodDeclaration).parameters.removeAt(0); it
//            },
//            {
//                (it as MethodDeclaration).addParameter(Parameter(PrimitiveType(PrimitiveType.Primitive.INT), "n")); it
//            },
//            {
//                val iff = IfStmt(BinaryExpr(NameExpr("n"),
//                    IntegerLiteralExpr("1"),
//                    BinaryExpr.Operator.EQUALS), BlockStmt(), null)
//                (it as MethodDeclaration).body.get().statements.add(iff); iff
//            },
//            {
//                (it as IfStmt).thenStmt.asBlockStmt().statements.add(
//                    ReturnStmt(NameExpr("n"))
//                ); it
//            },
//            {
//                (it as IfStmt).setElseStmt(BlockStmt())
//            },
//            {
//                val ret = ReturnStmt(NameExpr("n"))
//                (it as IfStmt).elseStmt.get().asBlockStmt().statements.add(
//                    ret
//                ); ret;
//            },
//            {
//                (it as ReturnStmt).removeExpression()
//            },
//            {
//                (it as ReturnStmt).setExpression(
//                    BinaryExpr(NameExpr("n"),
//                        MethodCallExpr("fact",
//                        BinaryExpr(NameExpr("n"), IntegerLiteralExpr("1"),
//                            BinaryExpr.Operator.MINUS)), BinaryExpr.Operator.MULTIPLY)
//                )
//                it
//            }
        )

        var prev:Node = classModel

        actions.forEachIndexed { i, a ->
            step {
                prev = a(prev)
                assertEquals(i+1, Commands.stackSize)
            }
        }

        actions.forEachIndexed { i, _ ->
            step {
                Commands.undo()
                assertEquals(actions.size-(i+1), Commands.stackSize)
            }
        }

        step {
            assertEquals(0, Commands.stackSize)
        }
        terminate()
    }
}