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
    implementation(files("../libs/strudel-0.8.2-standalone.jar"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
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
