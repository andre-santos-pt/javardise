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


# Builds (Gradle)

## Executable JAR (requires JRE)
Run the task **fatJar** (*distribution* category) to produce a standalone executable JARs for the respective platform. This will output a JAR file like **javardise-OS.jar** stored in *build/dist*, which can be executed. This option requires a JRE installed.

- Windows: ``java -jar javardise-windows.jar``
- MacOS: ``java -XstartOnFirstThread -jar javardise-macos.jar``

## Standalone application (embedded JRE)
Run the task **jpackage** (*distribution* category) to produce an installable bundle for your operating system, without requiring Java previously installed. The output file will be stored in *build/dist*.


## Integration in other projects

### Dependencies (Gradle)
Include the JAR resulting from **jar** (*build* category) as a dependency, replacing *%OS* with appropriate values. Because of SWT dependencies resolution, we also need to tweak the process (resolution strategy). Below is an example Gradle configuration for Windows.

```kotlin
dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")
    implementation("org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:3.123.0")
    implementation(files("javardise-1.0.2.jar"))
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (this.requested.name.contains("\${osgi.platform}")) {
                this.useTarget(
                    this.requested.toString()
                        .replace("\${osgi.platform}", "win32.win32.x86_64")
                )
            }
        }
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
Another integration possibility is developing plugins to the main editor. This can be achieved through the Java *services* infrastructure. Modules will need to create a **META-INF** folder, containing a **services** folder.

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
