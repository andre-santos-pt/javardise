package pt.iscte.javardise.tests

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.eclipse.swt.widgets.Shell
import org.junit.jupiter.api.Test
import pt.iscte.javardise.widgets.members.ClassWidget
import java.awt.Robot
import java.awt.event.KeyEvent


class FocusTest : SWTTest(TEST_SPEED) {

    override fun addContent(shell: Shell) {
        val code = """
        class AutoTest {
            static int fact(int n) {
                if(n == 1) {
                    return 1;
                }
                else {
                    return n * fact(n-1);
                }
            }
        }
    """
        val model = StaticJavaParser.parse(code)
        val w = ClassWidget(shell, model.types[0] as ClassOrInterfaceDeclaration)
        w.addFocusObserver { member, node ->
            println("${member!!::class.java.simpleName} $node")
        }
        w.setFocus()
    }

    @Test
    fun focus() {
        val robot = Robot()
        step {
           //robot.delay(2000)
        }
        for(i in 1..30)
           step {
               robot.keyPress(KeyEvent.VK_TAB)
               //println(it::class.java)
           }

        terminate()
    }
}