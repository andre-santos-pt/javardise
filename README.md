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
