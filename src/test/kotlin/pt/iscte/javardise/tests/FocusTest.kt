package pt.iscte.javardise.tests

import org.junit.jupiter.api.Test
import java.awt.Robot
import java.awt.event.KeyEvent


class FocusTest : SWTTest(
        """
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
) {
    init {
        classWidget.addFocusObserver { member, statement, node ->
            println("${member!!::class.java.simpleName} $node")
        }
        classWidget.setFocus()
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
           }

        terminate()
    }




}