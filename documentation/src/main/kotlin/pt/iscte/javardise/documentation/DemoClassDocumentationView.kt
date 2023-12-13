package pt.iscte.javardise.documentation

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.external.*
import pt.iscte.javardise.widgets.members.ClassWidget

/**
 * Demonstrates the Class Documentation View. The code editor and the documentation view work over the same model.
 * The right-most view is the actual source code that is obtained from the model.
 */
fun main() {
    val src = """
        /**
        * What a great student.
        *
        * Lorem ipsum dolor sit amet, consectetuer adipiscing elit. 
        * Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, 
        * nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat 
        * massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut,
        * imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. 
        */
public class Student {
	private int number;
	private String name;
	
/**
*   Creates a new student record.
*/
	public Student(int number, String name) {
		this.number = number;
		this.name = name;
	}

/**
* Student number.
*
* This is a number that has several digits.
 */
	public int getNumber() {
		return number;
	}

    /**
    * Student name.
    * 
    * With all the letters.
    * As well with all the spaces.
    */
	public String getName() {
		return name;
	}
}
""".trimIndent()

    val model = loadCompilationUnit(src)
    val clazz = model.findMainClass()!!

    val display = Display()
    var code: Text? = null
    val shell = shell {
        //text = "Demonstrates Documentation View"
        grid {
            grid(2) {
                grid {
                    label("Changes in the public methods of the class are reflected in the documentation view.")
                    label("Changes in the documentation view modify the model.")
                }
                check("Only public") {

                    message {
                        label("TODO: if unselected, the documentation of all members can be edited")
                    }
                }.selection = true
            }
            horizonalPanels {
                layoutData = GridData(GridData.FILL_BOTH)
                scrollable {
                    ClassWidget(it, clazz)
                }
                scrollable {
                    ClassDocumentationView(it, clazz)
                }
                fill {
                    code = multitext(style=SWT.V_SCROLL) {
                        editable = false
                    }
                }
            }
        }
    }
    Display.getDefault().addFilter(SWT.FocusIn) {
        code?.editable = true
        code?.text = model.toString()
        code?.editable = false
    }

    shell.pack()
    shell.open()

    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}
