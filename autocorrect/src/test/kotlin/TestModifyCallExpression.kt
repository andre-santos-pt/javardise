import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestModifyCallExpression : BaseTest(
    """
        class Test {
            int foo(int n) {
                if(n < 0)
                    return 0;
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
        stack.modifyCommand(ret, ret.expression.get(), MethodCallExpr("baar"), ret::setExpression)
        Assertions.assertEquals( MethodCallExpr("bar"), ret.expression.get())
    }
}