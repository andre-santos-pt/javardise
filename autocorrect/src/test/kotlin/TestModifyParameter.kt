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

class TestModifyParameter : BaseTest(
    """
        class Test {
           void myMethod(int n) {
           
           }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val param = method.parameters.first()
        val toChange = StaticJavaParser.parseType("booleen")
        val toExpect = param.clone().apply {
            setType(StaticJavaParser.parseType("boolean"))
        }
        stack.modifyCommand(param, StaticJavaParser.parseType("int"), toChange, param::setType)
        Assertions.assertEquals(toExpect, param)
    }
}