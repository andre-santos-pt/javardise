package pt.iscte.javardise.tests

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.Robot
import java.awt.event.KeyEvent


class ArrowsUpDownTest : SWTTest(
    """
        class Arrays {

	static int[] subArray(int a, int b, int[] array) {
		int[] sub = new int[b - a + 1];
		int i = 0;
		while(i < sub.length) {
			sub[i] = array[a + i];
			i = i + 1;
		}
		return sub;
	}

	static int[] firstHalf(int[] array, boolean includeMiddle) {
		if(array.length % 2 == 0) {
			return subArray(0, array.length / 2 - 1, array);
		}
		else if(includeMiddle) {
			return subArray(0, array.length / 2, array);
		}
		else {

			return subArray(0, array.length / 2 - 1, array);
		}
	}
}

    """
) {
    val robot = Robot()

    init {
        classWidget.setFocus()
    }

    fun down(expectedToken: String) {
        step {
            Assertions.assertEquals(
                classWidget.getChildOnFocus()?.text,
                expectedToken
            )
            robot.keyPress(KeyEvent.VK_DOWN)
        }
    }

    fun up(expectedToken: String) {
        step {
            Assertions.assertEquals(
                classWidget.getChildOnFocus()?.text,
                expectedToken
            )
            robot.keyPress(KeyEvent.VK_UP)
        }
    }


    @Test
    fun dummy() {}

    @Test
    fun focus() {
        listOf("class", "static", "int[]", "int", "while", "sub[i]", "i", "}", "return", "}").forEach {
            down(it)
        }

        up("static")
        up("}")

        down("return")
        down("}")

        listOf("static", "if", "return", "}", "else", "if", "return", "}", "else", "return", "}", "}", "}", "}").forEach {
            down(it)
        }

        listOf("class", "}", "}", "}", "}", "return", "else", "}", "if", "else", "}", "return", "if", "static").forEach{
            up(it)
        }
        terminate()
    }
}