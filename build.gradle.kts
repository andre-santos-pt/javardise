import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    application
}

group = "pt.iscte.javardise"
//version = "0.2"


repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    implementation("org.junit.platform:junit-platform-suite:1.9.1")

    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")

    val os = System.getProperty("os.name").toLowerCase()
    if (os.contains("mac")) {
        implementation(files("libs/swt-macos.jar"))
    } else if (os.contains("windows")) {
        implementation(files("libs/swt-windows.jar"))
    }
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.compileJava {
    // use the project's version or define one directly
    //options.javaModuleVersion.set(provider { project.version as String })
}

tasks {
    val macJar = register<Jar>("macJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // We need this for Gradle optimization to work
        archiveClassifier.set("macos") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        // manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .filter { !it.name.contains("junit") && !it.name.contains("opentest") }
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    val winJar = register<Jar>("winJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // We need this for Gradle optimization to work
        archiveClassifier.set("windows") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        // manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { File(it.absolutePath.replace("macos", "windows")) }
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }

    build {
       // dependsOn(macJar) // Trigger fat jar creation during build
       // dependsOn(winJar)
    }
}


tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
        //attributes["Automatic-Module-Name"] = "pt.iscte.javardise"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    //from(configurations.runtimeClasspath)
    from(sourceSets.main.get().output) {
        include("resources/**")
        into("resources")
    }
}



tasks {
    test {
        useJUnitPlatform()
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf("-XstartOnFirstThread")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
