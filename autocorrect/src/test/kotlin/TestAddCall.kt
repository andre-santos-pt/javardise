import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestAddCall : BaseTest(
    """
        class Test {
            int myField;
            
            int setField() {
                ;
            }
            
            void reset() {
                myField = 0;
            }
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        stack.replaceCommand(
            method.body.get().statements,
            method,
            method.body.get().statements.first.get(),
            ExpressionStmt(MethodCallExpr("resett"))
        )
        Assertions.assertEquals(ExpressionStmt(MethodCallExpr("reset")), method.body.get().statements.first.get())
    }
}