import com.github.javaparser.StaticJavaParser
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

class TestModifyBinaryExpression : BaseTest(
    """
        class Test {
            int first;
            int second;
            
            int set() {
                if(first > 0)
                    return first;
                else
                    return second;
            }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val iff = method.body.get().statements.first.get() as IfStmt
        val guard = iff.condition as BinaryExpr
        stack.modifyCommand(guard, guard.left, NameExpr("secnd"), guard::setLeft)
        Assertions.assertEquals( BinaryExpr(NameExpr("second"), IntegerLiteralExpr("0"), BinaryExpr.Operator.GREATER), iff.condition)
    }
}