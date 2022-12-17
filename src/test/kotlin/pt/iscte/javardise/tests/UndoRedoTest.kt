package pt.iscte.javardise.tests

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.type.VoidType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.javardise.CommandStack

class UndoRedoTest : SWTTest(
    ClassOrInterfaceDeclaration(NodeList(), false, "Test")
) {

    @Test
    fun testSteps() {
        val commandStack = CommandStack.create()
        val actions: List<(Node) -> Node> = listOf(
            {
                val m = MethodDeclaration(NodeList(), VoidType(), "method")
                commandStack.addCommand(classModel.members, classModel, m); m
            },
            {
                val block = BlockStmt()
                val w = WhileStmt(NameExpr("expression"), block)
                commandStack.addCommand(
                    (it as MethodDeclaration).body.get().statements,
                    it,
                    w
                ); block
            },
            {
                val ass = AssignExpr(
                    NameExpr("i"),
                    IntegerLiteralExpr("1"),
                    AssignExpr.Operator.ASSIGN
                )
                commandStack.addCommand(
                    (it as BlockStmt).statements,
                    it.parentNode.get(),
                    ExpressionStmt(ass)
                )
                it
            }
        )

        var prev: Node = classModel

        actions.forEachIndexed { i, a ->
            step {
                prev = a(prev)
                assertEquals(i + 1, commandStack.stackSize)
            }
        }

        var copy: ClassOrInterfaceDeclaration? = null
        step {
            copy = classModel.clone()
        }

        repeat(3) {
            step {
                commandStack.undo()
            }
        }

        repeat(3) {
            step {
                commandStack.redo()
            }
        }

        step {
            assertEquals(copy, classModel)
        }
        terminate()
    }

    @Test
    fun testCleanAfterExec() {
        val commandStack = CommandStack.create()
        val actions: List<(Node) -> Node> = listOf(
            {
                val m = MethodDeclaration(NodeList(), VoidType(), "method")
                commandStack.addCommand(classModel.members, classModel, m); m
            },
            {
                val block = BlockStmt()
                val w = WhileStmt(NameExpr("expression"), block)
                commandStack.addCommand(
                    (it as MethodDeclaration).body.get().statements,
                    it,
                    w
                ); block
            },
            {
                val ass = AssignExpr(
                    NameExpr("i"),
                    IntegerLiteralExpr("1"),
                    AssignExpr.Operator.ASSIGN
                )
                commandStack.addCommand(
                    (it as BlockStmt).statements,
                    it.parentNode.get(),
                    ExpressionStmt(ass)
                )
                it
            }
        )

        var prev: Node = classModel

        actions.forEachIndexed { i, a ->
            step {
                prev = a(prev)
                assertEquals(i + 1, commandStack.stackSize)
            }
        }

        repeat(2) {
            step {
                commandStack.undo()
            }
        }

        step {
            val block = BlockStmt()
            val w = IfStmt(NameExpr("test"), block, null)
            commandStack.addCommand(
                (classModel.members[0] as MethodDeclaration).body.get().statements,
                classModel.members[0], w
            )
        }

        var copy: ClassOrInterfaceDeclaration? = null
        step {
            copy = classModel.clone()
        }

        step {
            assertEquals(commandStack.stackTop, commandStack.stackSize - 1)
        }

        step {
            commandStack.redo()
        }

        step {
            assertEquals(copy, classModel)
        }

        step {
            commandStack.undo()
        }

        terminate()
    }
}