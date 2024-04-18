plugins {
    kotlin("jvm")
    application
}

group = "pt.iscte.javardise"
version = "1.2.0"

val win = System.getProperty("os.name").lowercase().contains("windows")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    if(!win)
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

