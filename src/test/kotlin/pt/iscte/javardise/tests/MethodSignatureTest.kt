package pt.iscte.javardise.tests

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.type.PrimitiveType
import org.junit.jupiter.api.Test

class MethodSignatureTest : SWTTest(
    """
        class AutoTest {
            int fact(int n) {
            }
        }
        """
) {

    @Test
    fun nameTypeChange() {
        val m =  classModel.methods[0]
        step {
            m.setName("factorial")
        }
        step {
            m.setType("double")
        }
        step {
            m.setType("int")
        }
        terminate()
    }

    @Test
    fun paramsChange() {
        val m =  classModel.methods[0]
        step {
            m.addParameter(PrimitiveType(PrimitiveType.Primitive.BOOLEAN), "test")
        }
        step {
            m.parameters[1].setName("test1")
        }
        step {
            m.addParameter(PrimitiveType(PrimitiveType.Primitive.CHAR), "test2")
        }
        step {
            m.parameters.removeAt(1)
        }
        step {
            m.parameters.removeAt(0)
        }
        step {
            m.parameters.removeAt(0)
        }
        step {
            m.addParameter(PrimitiveType(PrimitiveType.Primitive.DOUBLE), "new")
        }
        step {
            m.parameters[0].setName("n")
        }
        step {
            m.parameters[0].setType("int")
        }
        terminate()
    }

    @Test
    fun modifiersChange() {
        val m = classModel.methods[0]
        step {
            m.modifiers.add(Modifier.staticModifier())
        }
        step {
            m.modifiers.add(0, Modifier.publicModifier())
        }
        step {
            m.modifiers.remove(Modifier.publicModifier())
        }
        step {
            m.modifiers.removeAt(0)
        }
        step {
            m.modifiers.add(Modifier.staticModifier())
        }
        terminate()
    }
}