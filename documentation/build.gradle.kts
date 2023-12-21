plugins {
    kotlin("jvm")
    application
}

group = "pt.iscte.javardise"
//version = "0.1"

val win = System.getProperty("os.name").lowercase().contains("windows")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

//configurations.addAll(project(":").configurations)

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    if(!win)
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

//val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//    jvmTarget = "1.8"
//}
//
//val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
//compileTestKotlin.kotlinOptions {
//    jvmTarget = "1.8"
//}
