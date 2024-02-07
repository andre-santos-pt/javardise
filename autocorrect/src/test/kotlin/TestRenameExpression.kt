import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestRenameExpression : BaseTest(
    """
        class Test {
            int x;
            
            int getX() {
                return HOLE;
            }   
        }
    """
) {

    @Test
    fun test() {
        val method = unit.types.first.get().methods.first()
        val ret = method.body.get().statements.first.get() as ReturnStmt
        stack.modifyCommand(ret, NameExpr("HOLE"), NameExpr("X"),ret::setExpression)
        Assertions.assertEquals(ReturnStmt(NameExpr("x")), method.body.get().statements.first.get())
    }
}