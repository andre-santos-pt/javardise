plugins {
    kotlin("jvm")
    application
}

group = "pt.iscte.javardise"
//version = "1.1.0"

val win = System.getProperty("os.name").lowercase().contains("windows")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    implementation("com.google.guava:guava:33.0.0-jre")
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    if(!win)
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.test {
    useJUnitPlatform()
}