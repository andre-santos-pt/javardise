import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestModifyUnaryExpression : BaseTest(
    """
        class Test {
            boolean flag;
            
            boolean set() {
                if(!true)
                    return false;
                else
                    return flag;
            }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val iff = method.body.get().statements.first.get() as IfStmt
        val guard = iff.condition as UnaryExpr
        stack.modifyCommand(guard, guard.expression, NameExpr("flg"), guard::setExpression)
        Assertions.assertEquals(UnaryExpr(NameExpr("flag"), UnaryExpr.Operator.LOGICAL_COMPLEMENT), iff.condition)
    }
}