import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("project-report")
    application
}

group = "pt.iscte"
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
    mainClass.set("pt.iscte.javardise.JavardiseKt")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
