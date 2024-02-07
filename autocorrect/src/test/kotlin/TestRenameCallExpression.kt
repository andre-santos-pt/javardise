import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestRenameCallExpression : BaseTest(
    """
        class Test {
            int foo(int n) {
                if(n < 0)
                    return test();
                else
                    return n+1;
            }
            
            int bar() {
                return -1;
            }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val iff = method.body.get().statements.first.get() as IfStmt
        val ret = iff.thenStmt as ReturnStmt
        val call = ret.expression.get() as MethodCallExpr
        stack.modifyCommand(call, call.name, SimpleName("baar"), call::setName)
        Assertions.assertEquals( MethodCallExpr("bar"), ret.expression.get())
    }
}