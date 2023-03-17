# Setup (gradle tasks)
# other/macJar
# documentation/jar
# .../jar

java -XstartOnFirstThread -cp build/libs/javardise-macos.jar\
:documentation/build/libs/documentation.jar\
:debugger/build/libs/debugger-0.1.jar:debugger/libs/Strudel.jar\
 pt.iscte.javardise.editor.MainKt