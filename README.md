# About

Javardise is a research prototype consisting of a [Projectional Editor](https://en.wikipedia.org/wiki/Structure_editor) 
for Java, built using:
- [JavaParser](http://javaparser.org): abstract representation of the code (model)
- [Standard Widget Toolkit (SWT)](https://www.eclipse.org/swt): GUI technology

The editor enforces the structure of code, ensuring that:
- the code is always well-formed and well-formatted
- the Java language grammar *always* accepts the code (no lexical/parse errors)
- every code modification is represented in a well-defined 
command (e.g., add parameter, delete statement, modify identifier)

while allowing:
- editing *views* of model elements (e.g., methods in isolation of their class)
- editing multiple *views* of a same model elements
- performing editing commands programmatically through the model


# Setup using IntelliJ projects

## Javardise build (Gradle)
1. Download SWT library JAR from the [official website](https://download.eclipse.org/eclipse/downloads/drops4/R-4.25-202208311800/)
for your Operating System. Place the **swt.jar** file in the directory **/libs** of the project.
2. Run Gradle task **build jar**, which will output the **javardiseJP-VERSION.jar** to the directory **/build/libs**.


## Integration in other projects
The Javardise base editor may run standalone.  However, this project has also the following goals:
- Provide reusable widgets to manipulate the code elements that may be used by other applications as a library
- Enable the integration of extensions to the base editor.

### Dependencies (Gradle)
Include the following dependencies in the **build.gradle.kts**, replacing *%PATH* and *%VERSION* with appropriate values.

```kotlin
dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.4")
    implementation (files("%PATH/swt.jar"))
    implementation (files("%PATH/javardiseJP-%VERSION".jar"))
}
```


## Examples
In this repo there are few examples of using Javardise components.

### ClassWidget
An example of using an widget to edit a whole class.

[pt.iscte.javardise.examples.DemoClassEditor](https://github.com/andre-santos-pt/JavardiseJP/blob/master/src/main/kotlin/pt/iscte/javardise/examples/DemoClassEditor.kt)

### MethodWidget (multiple views)
An example of using a widget to edit a method in isolation. This example also ilustrates the possibility of multiple views of a same element of the model (method in this case).

[pt.iscte.javardise.examples.DemoMethodMVC](https://github.com/andre-santos-pt/JavardiseJP/blob/master/src/main/kotlin/pt/iscte/javardise/examples/DemoMethodMVC.kt)


### Documentation view

An example of using the class documentation view, editing code and documentation in parallel over the same model.

[pt.iscte.javardise.examples.DemoClassDocumentationView](https://github.com/andre-santos-pt/JavardiseJP/blob/master/src/main/kotlin/pt/iscte/javardise/examples/DemoClassDocumentationView.kt)

