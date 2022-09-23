import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("project-report")
    application
}

group = "org.example"
version = "0.1"



repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.4")
    implementation (files("libs/swt.jar"))
}

//configurations.all {
//    resolutionStrategy {
//        dependencySubstitution {
//            val os = System.getProperty("os.name").toLowerCase()
//            val osgi = "org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"
//            if (os.contains("windows")) {
//                substitute(module(osgi))
//                    .using(module("org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:3.114.0"))
//            }
//            else if (os.contains("linux")) {
//                substitute(module(osgi))
//                    .using(module("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.114.0"))
//            }
//            else if (os.contains("mac")) {
//                substitute(module(osgi))
//                    .using(module("org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:4.6.1"))
//            }
//        }
//    }
//}

application {
    mainClass.set("TestKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
