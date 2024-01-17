import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import pt.iscte.javardise.CommandStack
import pt.iscte.javardise.autocorrect.AutoCorrectCommand
import pt.iscte.javardise.autocorrect.AutoCorrectHandler
import pt.iscte.javardise.autocorrect.PRIMITIVE_TYPES
import pt.iscte.javardise.editor.setupSymbolSolver
import java.io.File


open class BaseTest(src: String) {
    val unit: CompilationUnit
    val stack: CommandStack

    init {
        setupSymbolSolver(File(""))
        unit = StaticJavaParser.parse(src)
        val types = PRIMITIVE_TYPES.toMutableSet()
        unit.types.mapTo(types) {it.nameAsString}
        stack = CommandStack.CommandStackImpl(null)
        stack.addObserver { command, _ ->
            if (command !is AutoCorrectCommand) {
                AutoCorrectHandler(stack, types).checkCommand(command)
            }
        }
    }
}

