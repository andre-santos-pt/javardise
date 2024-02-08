import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestModifyAssign : BaseTest(
    """
        class Test {
            int myField;
            
            Test(int n) {
                field = n;
            }   
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val assign = (method.body.get().statements.first.get() as ExpressionStmt).expression as AssignExpr
        stack.modifyCommand(assign, assign.target, NameExpr("myfield"), assign::setTarget)
        Assertions.assertEquals(AssignExpr(NameExpr("myField"),NameExpr("n"),AssignExpr.Operator.ASSIGN), assign)
    }
}