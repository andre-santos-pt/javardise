package pt.iscte.javardise.autocorrect

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.UnsolvedSymbolException
import org.eclipse.swt.SWT
import org.eclipse.swt.events.VerifyListener
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.Command
import pt.iscte.javardise.CommandKind
import pt.iscte.javardise.basewidgets.TextWidget
import pt.iscte.javardise.basewidgets.TextWidget.Companion.findAncestorOfType
import pt.iscte.javardise.editor.Action
import pt.iscte.javardise.editor.CodeEditor
import pt.iscte.javardise.widgets.expressions.CallExpressionWidget
import pt.iscte.javardise.widgets.expressions.SimpleExpressionWidget
import pt.iscte.javardise.widgets.expressions.VariableDeclarationWidget
import pt.iscte.javardise.widgets.members.MethodWidget


class AutoCorrectLiveAction : Action {
    override val name: String
        get() = "Auto-correct as you type"

    override val iconPath: String
        get() = "correctlive.png"

    override val toggle: Boolean
        get() = true

    override val toggleDefault: Boolean
        get() = true

    val autocorrect = AutoCorrectLookup()

    var editor: CodeEditor? = null
    val types = PRIMITIVE_TYPES.toMutableSet()

    override fun init(editor: CodeEditor) {
        this.editor = editor
        editor.allClasses().mapTo(types) { it.nameAsString }
    }

    fun modifyText(node: Node, prevText: String, newText: String, getWidget: () -> TextWidget) {
        class ModifyIdentifierCommand : Command {
            override val target: Node = node
            override val kind: CommandKind = CommandKind.MODIFY
            override val element: Any = target
            override fun run() {
                getWidget().text = newText
                getWidget().setAtRight()
            }

            override fun undo() {
                getWidget().text = prevText
                getWidget().setAtRight()
            }
        }
        editor?.classOnFocus?.commandStack?.execute(ModifyIdentifierCommand())
    }

    val listener = VerifyListener { e ->
        if (!e.character.isLetterOrDigit())
            return@VerifyListener
        val t = e.widget as Text
        val uncorrectedText = t.text.substring(0 until e.start) + e.character.toString() + t.text.substring(e.end)
        if (t.parent is SimpleExpressionWidget && (t.parent as SimpleExpressionWidget).node is NameExpr) {
            val simpleExp = t.parent as SimpleExpressionWidget
            val node = simpleExp.node as NameExpr
            autocorrect.findSubOption(node, uncorrectedText)?.let {
                e.doit = false
                modifyText(node, uncorrectedText, it.id) { simpleExp.expression }
            }
        } else if (t.parent is CallExpressionWidget) {
            val callExp = t.parent as CallExpressionWidget
            val node = callExp.node
            if (node.scope.isPresent) {
                try {
                    val scopeType = node.scope.get().calculateResolvedType()
                    // TODO results are cached
                    val lookup = scopeType.asReferenceType().allMethods.map { it.name }
                    autocorrect.matchSub(uncorrectedText, lookup)?.let {
                        e.doit = false
                        modifyText(callExp.node, uncorrectedText, it) { callExp.methodName }
                    }
                } catch (_: UnsolvedSymbolException) {

                }
            } else {
                val lookup = node.findClass()?.methods?.map { it.nameAsString } ?: emptyList()
                autocorrect.matchSub(uncorrectedText, lookup)?.let {
                    e.doit = false
                    modifyText(callExp.node, uncorrectedText, it) { callExp.methodName }
                }
            }
        } else if (t.data is Type) {
            val type = t.data as Type
            try {
                type.resolve()
            } catch (_: UnsolvedSymbolException) {
                autocorrect.matchSub(uncorrectedText, types)?.let {
                    if (t.parent is MethodWidget.ParamListWidget.ParamWidget) {
                        modifyText(type, uncorrectedText, it) {
                            (t.parent as MethodWidget.ParamListWidget.ParamWidget).type
                        }
                        e.doit = false
                    } else {
                        val varDec = t.findAncestorOfType<VariableDeclarationWidget>()
                        if(varDec != null) {
                            modifyText(type, uncorrectedText, it) {
                                varDec.type
                            }
                            e.doit = false
                        }
                        else {
                            val method = t.findAncestorOfType<MethodWidget>()
                            if (method != null && method.node.isMethodDeclaration) {
                                modifyText(type, uncorrectedText, it) {
                                    method.type!!
                                }
                                e.doit = false
                            }
                        }
                    }
                    // TODO Field Declaration
                }
            }
        }
    }

    private var currentWidget: Text? = null

    val filter = Listener { event ->
        if (event.widget is Text) {
            val t = event.widget as Text
            currentWidget = t
            t.addVerifyListener(listener)
        }
    }

    val remfilter = Listener { event ->
        if (event.widget is Text) {
            val t = event.widget as Text
            t.removeVerifyListener(listener)
        }
    }

    override fun run(editor: CodeEditor, toggle: Boolean) {
        if (toggle) {
            Display.getDefault().addFilter(SWT.FocusIn, filter)
            Display.getDefault().addFilter(SWT.FocusOut, remfilter)
        } else {
            Display.getDefault().removeFilter(SWT.FocusIn, filter)
            Display.getDefault().removeFilter(SWT.FocusOut, remfilter)
            currentWidget?.removeVerifyListener(listener)
        }
    }
}
