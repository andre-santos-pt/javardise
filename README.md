# About

Javardise is a research prototype consisting of a [Projectional Editor](https://en.wikipedia.org/wiki/Structure_editor) for a mini-Java, a subset of the language primitives that roughly suffices for introductory programming in Java.

> The editor is not meant to be a full-featured editor, but rather a base for experimentation and research on structured/projectional editors.

The editor interaction enforces the structure of code, ensuring that:
- the code is always well-formed and well-formatted
- the Java language grammar *always* accepts the code (no lexical/parse errors)
- every code modification is represented in a well-defined 
command (e.g., add parameter, delete statement, modify identifier)

while allowing:
- editing *views* of model elements (e.g., methods in isolation of their class)
- editing multiple *views* of a same model elements
- performing commands that programmatically transform the model

# Download

A standalone distribution (no JVM required) may be obtained through the following links. This distribution includes an extension to compile Java using the SDK Compiler API.

- [Windows](https://home.iscte-iul.pt/~alssl/javardise/Javardise-1.1.0.zip)

- [Mac OS](https://home.iscte-iul.pt/~alssl/javardise/Javardise-1.1.0.dmg) - because the application is currently not signed, one has to trick the OS to be able to execute it. After extracting the application from the DMG, execute: ```xattr -d com.apple.quarantine Javardise.app``` On the first time, open the application with the popup menu action in order to force the OS to open it (you might have to do it twice).

# Usage
![Javardise](docimages/javardiseShot.png?raw=true)

1. A dialog will ask for a folder, which will be used as a workspace. Currently, Javardise only reads the .java contained at the root, while subfolders are ignored. Any changes in Javardise settings will be stored in a **.javardise** file in the workspace folder.


2. All the Java files contained in the workspace folder will be opened (there is no file explorer). As code is edited, it is automatically saved to disk. You may add new files or delete existing ones. However, note that external changes in the workspace folder will not be automatically reflected in the application without restart.


3. Javardise expects syntactically well-formed Java files (that is, which are accepted by the grammar). If for some reason a file cannot be parsed, it will be opened as raw text, which can be edited, and in turn one may attempt to reload.

# Implementation
Javardise is built using:
- [JavaParser](https://javaparser.org): abstract representation of the code (model)
- [Standard Widget Toolkit (SWT)](https://www.eclipse.org/swt): GUI Toolkit

There are some core Java primitives for which there is **no editing support**, but which might be supported in the future:

**structural**:
- package declaration
- imports
- method throws directive
- generics
- annotations
- implements/extends
- enums
- records
- multiple class declarations in a single file

**statements**:
- try-catch
- concurrency primitives
- this call in constructors

Support for new statements can be implemented as a pluggable features (see an example with the [assert statement](https://github.com/andre-santos-pt/javardise/blob/master/src/main/kotlin/pt/iscte/javardise/widgets/statements/AssertWidget.kt)). The statement feature can be plugged in in the [configuration oject](https://github.com/andre-santos-pt/javardise/blob/master/src/main/kotlin/pt/iscte/javardise/Configuration.kt) (following up the exampe, look for *AssertFeature*).

**expressions**:
- lambda expressions
- member references

# Builds (requires Gradle 8.4)

## Executable JAR (requires JRE 17+)
Run the task **fatJar** (*distribution* category) to produce a standalone executable JARs for the respective platform. This will output a JAR file like **javardise-OS.jar** stored in *build/dist*, which can be executed. This option requires a JRE installed.

- Windows: ``java -jar javardise-windows.jar``
- MacOS: ``java -XstartOnFirstThread -jar javardise-macos.jar``

## Standalone application (with embedded JRE)
Run the task **jpackage** (*distribution* category) to produce an installable bundle for your operating system, without requiring Java previously installed. The output file will be stored in *build/dist*. The plugin for compilation will be packaged.


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
An example of using a widget to edit a method in isolation. This example also illustrates the possibility of multiple views of a same element of the model (method in this case).

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

## Compilation plugin

There is one subproject for supporting compilation of Java that is implemented as a [plugin](https://github.com/andre-santos-pt/javardise/tree/master/compilation) to the base editor.

This plugin provides compilation errors using the standard Java compiler API.
