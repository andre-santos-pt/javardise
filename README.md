# About

Javardise is a research prototype consisting of a [Projectional Editor](https://en.wikipedia.org/wiki/Structure_editor) for Java, built on top of [JavaParser](http://javaparser.org), which serves as the abstract representation of the code (model).

The editor enforces the structure of code, ensuring that:
- the code is always well-formed and well-formated
- the Java language grammar *always* accepts the code (no lexical/parse errors)
- every code modification is represented in a well-defined command (e.g., add parameter, delete statement, modify identifier)

while allowing:
- editing *views* of model elements (e.g., methods in isolation of their class)
- editing multiple *views* of a same model elements
- performing editing comands programmatically through the model
