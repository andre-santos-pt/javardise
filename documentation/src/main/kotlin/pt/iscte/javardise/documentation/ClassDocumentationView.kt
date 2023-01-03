package pt.iscte.javardise.documentation

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.SimpleName
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
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.ObserverWidget
import pt.iscte.javardise.external.*
import pt.iscte.javardise.observeListUntilDispose

// TODO dynamic parameter for Public / Non-Public filtering
// TODO fields, constructors, methods

class ClassDocumentationView(
    parent: Composite,
    override val node: ClassOrInterfaceDeclaration
) : ObserverWidget<ClassOrInterfaceDeclaration>(parent) {

    val fontLarge = Font(Display.getDefault(), FontData("Arial", 24, SWT.BOLD))
    val fontMedium = Font(Display.getDefault(), FontData("Arial", 18, SWT.BOLD))
    val fontSmall = Font(Display.getDefault(), FontData("Arial", 14, SWT.NONE))

    val Javadoc.header
        get() = if (description.toText().isEmpty()) ""
        else description.toText().split(Regex("\\n"))[0]

    val Javadoc.details
        get() = if (!description.toText().contains('\n')) ""
        else description.toText()
            .split(Regex("\\n"), 2)[1].trimStart { it == '\n' }

    init {
        layout = FillLayout()
        grid {
            handleClassHeader()
            handleMethods()
        }
    }

    private fun Composite.handleClassHeader() {
        label(node.nameAsString) {
            font = fontLarge
            observeProperty<SimpleName>(ObservableProperty.NAME) {
                text = it.toString()
                requestLayout()
            }
        }
        val header =
            text(node.javadoc.getOrNull?.header ?: "", style = SWT.NONE) {
                font = fontSmall
                layoutData = GridData(GridData.FILL_HORIZONTAL)
                background = Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
                addModifyListener {
                    (it.widget as Control).requestLayout()
                }
            }
        val details = multitext(node.javadoc.getOrNull?.details ?: "") {
            layoutData = GridData(GridData.FILL_HORIZONTAL)
            addModifyListener {
                (it.widget as Control).requestLayout()
            }
        }

        fun setClassJavadoc() {
            node.setJavadocComment(header.text + "\n\n" + details.text)
        }
        header.focusLost {
            setClassJavadoc()
        }

        details.focusLost {
            setClassJavadoc()
        }
    }

    private fun Composite.handleMethods() {
        node.methods.filter { it.isPublic }.forEach {
            addMethod(it)
        }

        observeListUntilDispose(
            node.members,
            object : ListObserver<BodyDeclaration<*>> {
                override fun elementAdd(
                    list: NodeList<BodyDeclaration<*>>,
                    index: Int,
                    node: BodyDeclaration<*>
                ) {
                    if (node is MethodDeclaration && node.modifiers.contains(
                            Modifier.publicModifier()
                        )
                    ) {
                        parent.addMethod(node)
                        parent.requestLayout()
                    }
                }

                override fun elementRemove(
                    list: NodeList<BodyDeclaration<*>>,
                    index: Int,
                    node: BodyDeclaration<*>
                ) {
                    TODO()
                }

                override fun elementReplace(
                    list: NodeList<BodyDeclaration<*>>,
                    index: Int,
                    old: BodyDeclaration<*>,
                    new: BodyDeclaration<*>
                ) {
                    TODO()
                }
            })
    }


    private fun Composite.addMethod(m: MethodDeclaration) {
        val paramList = mutableListOf<Pair<Parameter, Text>>()
        val group = fill {
            layoutData = GridData(GridData.FILL_HORIZONTAL)
            layout = GridLayout()

            label(m.nameAsString) {
                font = fontMedium
                observeProperty<SimpleName>(
                    ObservableProperty.NAME,
                    target = m
                ) {
                    text = it.toString()
                    requestLayout()
                }
            }

            val params = grid(2) {
            }
            m.javadoc.getOrNull?.let {
                m.parameters.forEach { p ->
                    val name = params.label(p.nameAsString)
                    val text =
                        m.javadoc.get().blockTags.find { it.tagName == "param" && it.name.getOrNull == p.nameAsString }?.content?.toText()
                            ?: ""
                    val desc = params.text(text)
                    paramList.add(Pair(p, desc))
                    p.observeProperty<SimpleName>(ObservableProperty.NAME) {
                        name.text = it.toString()
                        name.requestLayout()
                    }
                }
            }
            observeListUntilDispose(
                m.parameters,
                object : ListObserver<Parameter> {
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
                ?.split(Regex("\\n"))
                ?.filter { !it.trim().startsWith("@param") }
                ?.joinToString(separator = "\n") { it } ?: "") {
                font = fontSmall
                background = Display.getDefault()
                    .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
                addModifyListener {
                    (it.widget as Control).requestLayout()
                }
            }
        doc.layoutData = GridData(GridData.FILL_HORIZONTAL)
        doc.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                val jdoc = Javadoc(
                    JavadocDescription(
                        listOf(
                            JavadocDescriptionElement { doc.text })
                    )
                )
                paramList.forEach {
                    jdoc.addBlockTag(
                        JavadocBlockTag.createParamBlockTag(
                            it.first.nameAsString,
                            it.second.text
                        )
                    )
                }
                m.setJavadocComment(jdoc)
            }
        })



        observeListUntilDispose(m.modifiers, object : ListObserver<Modifier> {
            override fun elementAdd(
                list: NodeList<Modifier>,
                index: Int,
                node: Modifier
            ) {
                if (node.keyword == Modifier.Keyword.PRIVATE)
                    group.dispose()
            }

            override fun elementReplace(
                list: NodeList<Modifier>,
                index: Int,
                old: Modifier,
                new: Modifier
            ) {
                if (new.keyword == Modifier.Keyword.PRIVATE)
                    group.dispose()
            }

            override fun elementRemove(
                list: NodeList<Modifier>,
                index: Int,
                node: Modifier
            ) {
                if (node.keyword == Modifier.Keyword.PUBLIC) {
                    group.dispose()
                    requestLayout()
                }

            }
        })

    }

    override val control: Control
        get() = this

    override fun setFocusOnCreation(firstFlag: Boolean) {
        setFocus()
    }
}


