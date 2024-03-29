package pt.iscte.javardise.external

import com.github.javaparser.ast.Node
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.NodeWidget
import pt.iscte.javardise.basewidgets.TextWidget
import java.io.File
import java.util.*

fun Composite.add(action: Composite.() -> Unit) {
    action()
}

fun Composite.button(text: String, action: Button.() -> Unit): Button {
    val b = Button(this, SWT.PUSH)
    b.text = text
    b.addSelectionListener(object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent?) {
            action(b)
        }
    })
    return b
}

fun Composite.check(
    text: String,
    style: Int = SWT.NONE,
    action: (Boolean) -> Unit = {}
): Button {
    val b = Button(this, style or SWT.CHECK)
    b.text = text
    b.addSelectionListener(object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent?) {
            action(b.selection)
        }
    })
    return b
}

fun Composite.text(
    text: String = "",
    style: Int = SWT.BORDER,
    init: Text.() -> Unit = {}
): Text {
    val t = Text(this, style)
    t.text = text
    init(t)
    return t
}

fun Composite.multitext(
    text: String = "",
    style: Int = SWT.BORDER,
    init: Text.() -> Unit = {}
): Text {
    val text = text(text, style or SWT.MULTI or SWT.WRAP or SWT.V_SCROLL, init)
    text.addVerifyListener {
        if (it.character == SWT.TAB) {
            text.traverse(SWT.TRAVERSE_TAB_NEXT)
            it.doit = false
        }
    }
    return text
}

fun Composite.combo(
    style: Int = SWT.BORDER,
    content: () -> List<String>
): Combo {
    val t = Combo(this, style)
    t.setItems(*content().toTypedArray())
    return t
}

fun Control.onFocus(action: () -> Unit): Control {
    addFocusListener(object : FocusAdapter() {
        override fun focusGained(e: FocusEvent?) {
            action()
        }
    })
    return this
}

val GRID_FILL_HORIZONTAL = GridData(SWT.FILL, SWT.FILL, true, false)


fun Control.fillGridHorizontal() {
    require(parent.layout is GridLayout)
    layoutData = GRID_FILL_HORIZONTAL
}

fun Control.fillGrid() {
    require(parent.layout is GridLayout)
    layoutData = GridData(GridData.FILL_BOTH)
}

fun Composite.label(
    text: String,
    style: Int = SWT.WRAP,
    font: Font? = null,
    init: Label.() -> Unit = {}
): Label {
    val t = Label(this, style)
    t.text = text
    font?.let { t.font = font }
    init(t)
    return t
}

fun Composite.label(
    file: File,
    style: Int = SWT.NONE,
    font: Font? = null,
    init: Label.() -> Unit = {}
): Label {
    val s = Scanner(file)
    val buf = StringBuffer()
    while (s.hasNextLine())
        buf.appendLine(s.nextLine())
    s.close()
    return label(buf.toString(), style, font, init)
}

fun Composite.toggle(text: String, action: (Boolean) -> Unit = {}): Button {
    val t = Button(this, SWT.TOGGLE)
    t.text = text
    t.addSelectionListener(object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent?) {
            action(t.selection)
        }
    })
    return t
}

fun Composite.separator(): Label {
    val t = Label(this, SWT.SEPARATOR)
    return t
}


fun Composite.row(content: Composite.() -> Unit): Composite {
    val c = Composite(this, SWT.NONE)
    c.layout = ROW_LAYOUT_H_SHRINK
    c.font = this.font
    c.background = this.background
    c.foreground = this.foreground
    content(c)
    return c
}

fun Composite.column(
    margin: Boolean = false,
    content: Composite.() -> Unit
): Composite {
    val c = Composite(this, SWT.NONE)
    c.layout = if (margin) ROW_LAYOUT_V_SPACED else ROW_LAYOUT_V_SHRINK
    c.font = this.font
    c.background = this.background
    c.foreground = this.foreground
    content(c)
    return c
}

fun Composite.fill(content: Composite.() -> Unit): Composite {
    val c = Composite(this, SWT.NONE)
    c.layout = FillLayout()
    content(c)
    return c
}

fun Composite.grid(
    cols: Int = 1,
    equalWidth: Boolean = false,
    content: Composite.() -> Unit
): Composite {
    val c = Composite(this, SWT.NONE)
    c.layout = GridLayout(cols, equalWidth)
    content(c)
    return c
}

fun Composite.horizonalPanels(content: Composite.() -> Unit) {
    val sash = SashForm(this, SWT.HORIZONTAL)
    content(sash)
}

fun Composite.group(text: String, content: Composite.() -> Unit): Group {
    val g = Group(this, SWT.BORDER)
    g.layout = RowLayout()
    g.text = text
    content(g)
    return g
}

class StackComposite(parent: Composite) : Composite(parent, SWT.NONE) {
    init {
        layout = StackLayout()
    }

    fun set(index: Int) {
        (layout as StackLayout).topControl = children[index]
        requestLayout()
    }

    fun next() {
        val i =
            (children.indexOf((layout as StackLayout).topControl) + 1) % children.size
        set(i)
    }
}

fun Composite.stack(content: Composite.() -> Unit): StackComposite {
    val c = StackComposite(this)
    content(c)
    if (children.isNotEmpty())
        c.set(0)
    return c
}

// TODO
fun Composite.image(path: String, style: Int = SWT.NONE) {

}

fun Control.onClick(action: () -> Unit) {
    addMouseListener(object : MouseAdapter() {
        override fun mouseDown(e: MouseEvent?) {
            action()
        }
    })
}

fun Shell.messageOkAction(init: Shell.() -> (() -> Unit)?) {
    val s = shell(SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL, this) {
        layout = RowLayout(SWT.VERTICAL)
        val okAction = init(this)
        button("OK") {
            if (okAction != null) {
                okAction()
            }
            this@shell.close()
        }
    }
    s.pack()
    s.center()
    s.open()
}

fun Shell.message(init: Shell.() -> Unit) {
    val s = shell(SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL, this) {
        layout = RowLayout(SWT.VERTICAL)
        init(this)
        button("OK") {
            this@shell.close()
        }
    }
    s.pack()
    s.center()
    s.open()
}

fun Shell.prompt(title: String, message: String, action: (String) -> Unit) {
    val s = shell(SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL, this, title) {
        grid(3) {
            label(message)
            val t = text {

            }
            t.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.character == SWT.CR) {
                        action(t.text ?: "?")
                        this@shell.close()
                    }
                }
            })
            button("OK") {
                if (t.text?.isNotEmpty() == true)
                    action(t.text ?: "?")
                this@shell.close()
            }
        }

    }
    s.pack()
    s.center()
    s.open()
}

fun Shell.promptConfirmation(
    title: String,
    okMessage: String = "OK",
    cancelMessage: String = "Cancel",
    okAction: () -> Unit
) {
    val s = shell(SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL, this, "Confirmation") {
        grid(3) {
            label(title)
            button(okMessage) {
                okAction()
                this@shell.close()
            }
            button(cancelMessage) {
                this@shell.close()
            }
        }

    }
    s.pack()
    s.center()
    s.open()
}

fun shell(
    style: Int = SWT.NONE,
    parent: Shell? = null,
    title: String = "",
    content: Shell.() -> Unit
): Shell {
    val s = if (parent == null)
        Shell(Display.getDefault(), style)
    else
        Shell(parent, style)
    s.text = title
    s.layout = FillLayout()
    content(s)
    return s
}


fun Shell.center() {
    val primary: Monitor = Display.getDefault().primaryMonitor
    val bounds: Rectangle = parent.bounds
    val dim: Point = computeSize(SWT.DEFAULT, SWT.DEFAULT)
    val x: Int = bounds.x + bounds.width / 2 - dim.x / 2
    val y: Int = bounds.y + bounds.height / 2 - dim.y / 2
    setLocation(x, y)
}

fun Shell.launch() {
    open()
    while (!isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    display.dispose()
}

fun font(face: String, size: Int, style: Int = SWT.NONE) =
    Font(Display.getDefault(), FontData(face, size, style))

private val marginLayout = GridLayout().apply {
    marginTop = 10
    marginLeft = 10
}

// only works when parent has FillLayout
fun <T : Composite> Composite.scrollable(
    style: Int = SWT.H_SCROLL or SWT.V_SCROLL,
    create: (Composite) -> T
): T {

    val scroll = ScrolledComposite(this, style).apply {
        layout = marginLayout
        setMinSize(100, 100)
        expandHorizontal = true
        expandVertical = true
        background = this.background
        foreground = this.foreground
        showFocusedControl = true
    }

    val content = create(scroll)
    scroll.content = content

    val list = PaintListener {
        if (!scroll.isDisposed) {
            val size = content.computeSize(SWT.DEFAULT, SWT.DEFAULT)
            scroll.setMinSize(size)
            scroll.requestLayout()
        }
    }

    content.addPaintListener(list)
    addDisposeListener {
        removePaintListener(list)
    }
    return content
}


fun Control.isChildOf(comp: Composite): Boolean =
    if (this == comp)
        true
    else if (parent == null)
        false
    else if (parent == comp)
        true
    else
        parent.isChildOf(comp)

fun Control.moveAbove(widget: TextWidget) = moveAbove(widget.widget)

fun create(style: Int, top: Int = 0, spacing: Int = 0): RowLayout {
    val layout = RowLayout(style)
    layout.marginLeft = 0
    layout.marginRight = 0
    layout.marginTop = top
    layout.marginBottom = 0
    layout.spacing = spacing
    return layout
}

val ROW_LAYOUT_H_CALL = create(SWT.HORIZONTAL, spacing = 0)
val ROW_LAYOUT_H_STRING = create(SWT.HORIZONTAL, spacing = -2)
val ROW_DATA_STRING = RowData(3, SWT.DEFAULT)

val ROW_LAYOUT_H_SHRINK = create(SWT.HORIZONTAL, spacing = 1, top = 0)
val ROW_LAYOUT_H_ZERO = create(SWT.HORIZONTAL, 2)
val ROW_LAYOUT_H = create(SWT.HORIZONTAL, 3)
val ROW_LAYOUT_H_DOT = create(SWT.HORIZONTAL, 0)
val ROW_LAYOUT_V_ZERO = create(SWT.VERTICAL, 2)
val ROW_LAYOUT_V_SPACED = create(SWT.VERTICAL, 20)
val ROW_LAYOUT_V_SHRINK = create(SWT.VERTICAL, spacing = 1, top = 0)


fun Control.traverse(visit: (Control) -> Boolean) {
    val enter = visit(this)
    if (this is Composite && enter)
        this.children.forEach { it.traverse(visit) }
}

val Text.isNumeric: Boolean
    get() {
        val regex = "-?\\d*(\\.\\d*)?".toRegex()
        return text.matches(regex)
    }

fun Control.menu(content: Menu.() -> Unit = {}) {
    menu = Menu(this)
    content(menu)
}

fun Menu.item(text: String, action: MenuItem.() -> Unit) {
    val item = MenuItem(this, SWT.PUSH)
    item.text = text
    item.addSelectionListener(object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent?) {
            action(item)
        }
    })
}

fun <T : Control> T.focusGained(action: T.() -> Unit) {
    val c = this
    addFocusListener(object : FocusAdapter() {
        override fun focusGained(e: FocusEvent?) {
            action(c)
        }
    })
}

fun <T : Control> T.focusLost(action: T.() -> Unit) {
    val c = this
    addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent?) {
            action(c)
        }
    })
}

fun Composite.findChild(accept: (Control) -> Boolean): Control? {
    var n: Control? = null
    traverse {
        if (accept(it)) {
            n = it
            return@traverse false
        }
        return@traverse true
    }
    return n
}


