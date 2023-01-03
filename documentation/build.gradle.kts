plugins {
    kotlin("jvm")
}

group = "pt.iscte.javardise"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
   // implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(project(":"))

    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")
    val os = System.getProperty("os.name").toLowerCase()
    if (os.contains("mac")) {
        implementation(files("../libs/swt-macos.jar"))
    } else if (os.contains("windows")) {
        implementation(files("../libs/swt-windows.jar"))
    }

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
