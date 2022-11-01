package pt.iscte.javardise.tests

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.type.PrimitiveType
import org.junit.jupiter.api.Test

class ForTest : SWTTest(
    """
        class ForTest {
            static int iterate(int n) {
                int s = 0;
                for(int i = 0; i < n; i++) {
                    s += i;
                }
                 
            }
        }
        """
) {

    @Test
    fun dummy() {
    }

}