package pt.iscte.javardise.tests

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.VoidType
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.javardise.Commands
import pt.iscte.javardise.addCommand
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.modifyCommand
import pt.iscte.javardise.widgets.members.ClassWidget

class CommandTests : SWTTest(TEST_SPEED) {

    lateinit var c: ClassOrInterfaceDeclaration
    lateinit var code: Text

    override fun addContent(shell: Shell) {
        c = ClassOrInterfaceDeclaration(NodeList(), false, "Test")
        val sash = SashForm(shell, SWT.HORIZONTAL)
        val scrollable = sash.scrollable {
            ClassWidget(it, c)
        }

        code = Text(sash, SWT.MULTI)
    }

    @Test
    fun testAuto() {
        val actions: List<(Node)-> Node> = listOf(
            { c.modifyCommand(c.name, SimpleName("AutoTest"), c::setName); c},
            {
                val m = MethodDeclaration(NodeList(), VoidType(),"method")
                c.members.addCommand(c, m); m
            },
            { c.modifyCommand(c.name, SimpleName("AutoTest2"), c::setName); c},
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

        var prev:Node = c

        actions.forEachIndexed { i, a ->
            step {
                prev = a(prev)
                code.text = c.toString()
                assertEquals(i+1, Commands.stackSize)
            }
        }

        actions.forEachIndexed { i, _ ->
            step {
                Commands.undo()
                code.text = c.toString()
                assertEquals(actions.size-(i+1), Commands.stackSize)
                //println("undo $i")
            }
        }

        step {
            assertEquals(0, Commands.stackSize)
        }
        terminate()
    }
}