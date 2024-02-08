# Setup (gradle tasks)
# dist/fatJar
# documentation/jar
# .../jar

java -XstartOnFirstThread -cp build/dist/javardise-macos.jar\
:compilation/build/libs/compilation.jar\
:autocorrect/build/libs/autocorrect.jar\
 pt.iscte.javardise.editor.MainKt