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


## Your project build  (Gradle)

1. Include the following dependencies in the **build.gradle.kts**, replacing *%PATH* and *%VERSION* with appropriate values.

```kotlin
dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.4")
    implementation (files("%PATH/swt.jar"))
    implementation (files("%PATH/javardiseJP-%VERSION".jar"))
}
```


## Examples

### ClassWidget

### MethodWidget (multiple views)



