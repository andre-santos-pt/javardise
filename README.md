# About

Javardise is a research prototype consisting of a [Projectional Editor](https://en.wikipedia.org/wiki/Structure_editor) 
for Java, built using:
- [JavaParser](http://javaparser.org): abstract representation of the code (model)
- [Standard Widget Toolkit (SWT)](https://www.eclipse.org/swt): GUI Toolkit

The editor enforces the structure of code, ensuring that:
- the code is always well-formed and well-formatted
- the Java language grammar *always* accepts the code (no lexical/parse errors)
- every code modification is represented in a well-defined 
command (e.g., add parameter, delete statement, modify identifier)

while allowing:
- editing *views* of model elements (e.g., methods in isolation of their class)
- editing multiple *views* of a same model elements
- performing editing commands programmatically through the model


# Setup (Gradle)

## Setup
1. Download SWT library JAR from the [official website](https://download.eclipse.org/eclipse/downloads/drops4/R-4.25-202208311800/)
for your Operating System. 
- Windows: place the JAR renamed as **swt-windows.jar** in the directory **libs** of the project.
- MacOS: place the JAR renamed as **swt-macos.jar** in the directory **libs** of the project. 
2. Run **jar** task to obtain a JAR named **javardise-VERSION.jar**. (does not contain dependencies)

## Standalone application
Run either **winJar** and **macJar** task (*other* category) to produce a standalone executable JARs for the respective platform. This will output a JAR file like **javardise-VERSION-OS.jar**, which can be executed.
- Windows: ``java -jar javardise-VERSION-windows.jar``
- MacOS: ``java -XstartOnFirstThread -jar javardise-VERSION-windows.jar``


## Integration in other projects

### Dependencies (Gradle)
Include the  dependencies in the **build.gradle.kts**, replacing *%VERSION* and *%OS* with appropriate values.

```kotlin
dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")
    implementation(files("libs/swt-%OS.jar"))
    implementation(files("libs/javardise-%VERSION.jar"))
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread") // if MacOS
}
}
```

## Using widgets as a library
The Javardise base editor may run standalone.  However, one may use Javardise widgets to manipulate the code elements in applications developed in SWT. In this repo there are few examples of using Javardise widgets.

### ClassWidget
An example of using an widget to edit a whole class.

[pt.iscte.javardise.examples.DemoClassEditor](https://github.com/andre-santos-pt/JavardiseJP/blob/master/src/main/kotlin/pt/iscte/javardise/examples/DemoClassEditor.kt)

### MethodWidget (multiple views)
An example of using a widget to edit a method in isolation. This example also ilustrates the possibility of multiple views of a same element of the model (method in this case).

[pt.iscte.javardise.examples.DemoMethodMVC](https://github.com/andre-santos-pt/JavardiseJP/blob/master/src/main/kotlin/pt/iscte/javardise/examples/DemoMethodMVC.kt)


### Documentation view

An example of using the class documentation view, editing code and documentation in parallel over the same model.

[pt.iscte.javardise.documentation.DemoClassDocumentationView](https://github.com/andre-santos-pt/JavardiseJP/blob/master/documentation/src/main/kotlin/pt/iscte/javardise/documentation/DemoClassDocumentationView.kt)


## Developing plugins
Another integration possibility is by developing plugins to the main editor. This can be achieved through the Java *services* infrastructure. We need to create a **META-INF** folder, containing a **services** folder.

### Actions
In order to plug-in an action (toolbar), we need to write a class that provide the behavior and configure it as a service.

1. Implement a class that implements *pt.iscte.javardise.editor.Action*
2. Create a file named **pt.iscte.javardise.editor.Action** in the **META-INF** folder, containing one line with the class name of (1)

When running the editor, this contribution will be detected and a button will appear in the toolbar. See subprojects as examples of plugins.

## Subprojects

There are currently 3 subprojects that work as plugins to the base editor of Javardise:

- [Compilation](https://github.com/andre-santos-pt/javardise/tree/master/compilation): support for compiling using the standard Java compiler API
- [Debugger](https://github.com/andre-santos-pt/javardise/tree/master/debugger): execution and debugging using [Strudel](https://github.com/andre-santos-pt/strudel)
- [Documentation](https://github.com/andre-santos-pt/javardise/tree/master/documentation): alternative views to edit documentation in isolation
