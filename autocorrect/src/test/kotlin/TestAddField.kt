import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestAddField : BaseTest(
    """
        class Test {
           
        }
    """
) {

    @Test
    fun test() {
        val clazz = unit.types.first.get()
        val toAdd = FieldDeclaration(NodeList(), VariableDeclarator(StaticJavaParser.parseType("bolean"), "myField"))
        val toExpect = toAdd.clone().apply {
            variables[0].setType(StaticJavaParser.parseType("boolean"))
        }
        stack.addCommand(clazz.members, clazz, toAdd)
        Assertions.assertEquals(toExpect, clazz.members.first.get())
    }
}