package pt.iscte.javardise.examples

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.AstObserver
import com.github.javaparser.ast.observer.AstObserverAdapter
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag
import com.github.javaparser.javadoc.description.JavadocDescription
import com.github.javaparser.javadoc.description.JavadocDescriptionElement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.external.*
import pt.iscte.javardise.observeListUntilDispose

class APIView(parent: Display, model: ClassOrInterfaceDeclaration) {
    val shell: Shell
    lateinit var col: Composite
    init {
        shell = shell(title = model.nameAsString + " API") {
            col = grid {
                val classDoc = multitext(model.javadoc.getOrNull?.description?.toText() ?: "")
                classDoc.layoutData = GridData(GridData.FILL_HORIZONTAL)
            }

            model.methods.filter {it.isPublic}.forEach {
                col.addMethod(it)
            }
            shell.pack()
        }

        model.members.register(
            object : AstObserverAdapter() {
                override fun listChange(
                    observedNode: NodeList<*>,
                    change: AstObserver.ListChangeType,
                    index: Int,
                    nodeAddedOrRemoved: Node
                ) {
                    if(change == AstObserver.ListChangeType.ADDITION && nodeAddedOrRemoved is MethodDeclaration && nodeAddedOrRemoved.isPublic) {
                        col.addMethod(nodeAddedOrRemoved)
                        col.requestLayout()
                        shell.pack()
                    }
                }
            })
    }

    private fun Composite.addMethod(m: MethodDeclaration) {
        val paramList = mutableListOf<Pair<Parameter, Text>>()
        val group = col.group(m.nameAsString) {
            layoutData = GridData(GridData.FILL_HORIZONTAL)
           layout = GridLayout()
            font = Font(Display.getDefault(), FontData("Menlo", 16, SWT.BOLD))
            val params = grid(2) {
            }
            m.javadoc.getOrNull?.let {

                m.parameters.forEach {p ->
                    val name = params.label(p.nameAsString)
                    val text = m.javadoc.get().blockTags.find { it.tagName == "param" && it.name.getOrNull == p.nameAsString }?.content?.toText() ?: ""
                    val desc = params.text(text)
                    paramList.add(Pair(p, desc))
                    p.observeProperty<SimpleName>(ObservableProperty.NAME) {
                        name.text = it.toString()
                        name.requestLayout()
                    }
                }
            }
            observeListUntilDispose(m.parameters, object : ListObserver<Parameter> {
                override fun elementAdd(
                    list: NodeList<Parameter>,
                    index: Int,
                    node: Parameter
                ) {
                    params.label(node.nameAsString)
                    val desc = params.text("")
                    desc.requestLayout()
                }
            })
        }

        val doc =
            group.multitext(m.javadoc.getOrNull?.description?.toText()
                ?.split(Regex("\\n"))?.filter { !it.trim().startsWith("@param") }?.joinToString(separator = "\n") {it} ?: "")
        doc.layoutData = GridData(GridData.FILL_HORIZONTAL)
        doc.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                val jdoc = Javadoc(JavadocDescription(listOf(
                    JavadocDescriptionElement {doc.text})))
                paramList.forEach {
                    jdoc.addBlockTag(JavadocBlockTag.createParamBlockTag(it.first.nameAsString, it.second.text))
                }
                m.setJavadocComment(jdoc)
            }
        })
        doc.addModifyListener {
            shell.pack()
        }
        m.observeProperty<SimpleName>(ObservableProperty.NAME) {
            group.text = it.toString()
            group.requestLayout()
        }
        observeListUntilDispose(m.modifiers, object : ListObserver<Modifier> {
            override fun elementAdd(
                list: NodeList<Modifier>,
                index: Int,
                node: Modifier
            ) {
                if(node.keyword == Modifier.Keyword.PRIVATE)
                    group.dispose()
            }

            override fun elementReplace(
                list: NodeList<Modifier>,
                index: Int,
                old: Modifier,
                new: Modifier
            ) {
                if(new.keyword == Modifier.Keyword.PRIVATE)
                    group.dispose()
            }
        })

    }

    fun open() = shell.open()
}


