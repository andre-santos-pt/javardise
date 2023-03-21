plugins {
    kotlin("jvm")
    application
}

group = "pt.iscte.javardise"
//version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
