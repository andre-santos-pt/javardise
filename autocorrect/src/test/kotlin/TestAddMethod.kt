import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestAddMethod : BaseTest(
    """
        class Test {
           
        }
    """
) {

    @Test
    fun test() {
        val clazz = unit.types.first.get()
        val toAdd = MethodDeclaration(NodeList(), StaticJavaParser.parseType("doble"), "myMethod")
        val toExpect = toAdd.clone().apply {
            setType(StaticJavaParser.parseType("double"))
        }
        stack.addCommand(clazz.members, clazz, toAdd)
        Assertions.assertEquals(toExpect, clazz.members.first.get())
    }
}