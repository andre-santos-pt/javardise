# Setup (gradle tasks)
# dist/fatJar
# documentation/jar
# .../jar

java -XstartOnFirstThread -cp build/dist/javardise-macos.jar\
:documentation/build/libs/documentation.jar\
:debugger/build/libs/debugger.jar:debugger/libs/Strudel.jar\
 pt.iscte.javardise.editor.MainKt