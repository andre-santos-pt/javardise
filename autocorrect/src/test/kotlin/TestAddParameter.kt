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

class TestAddParameter : BaseTest(
    """
        class Test {
           void myMethod() {
           
           }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val toAdd =  Parameter(StaticJavaParser.parseType("bite"), SimpleName("s"))
        val toExpect = toAdd.clone().apply {
            setType(StaticJavaParser.parseType("byte"))
        }
        stack.addCommand(method.parameters, method, toAdd)
        Assertions.assertEquals(toExpect, toAdd)
    }
}