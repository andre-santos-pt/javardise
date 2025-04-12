import org.panteleyev.jpackage.ImageType

plugins {
    kotlin("jvm") version "1.8.10"
    application
    id("org.panteleyev.jpackageplugin") version "1.5.1"
}

group = "pt.iscte.javardise"
version = "1.2.1"

val mac = System.getProperty("os.name").lowercase().contains("mac")
val win = System.getProperty("os.name").lowercase().contains("windows")

val os = if (mac)
    "macos"
else if (win)
    "windows"
else
    "linux"

fun resolutionSwt(
    dependencyResolveDetails: DependencyResolveDetails,
    buildGradle: Build_gradle
) {
    if (dependencyResolveDetails.requested.name.contains("\${osgi.platform}")) {
        val platform = if (buildGradle.mac) "cocoa.macosx.x86_64"
        else if (buildGradle.win) "win32.win32.x86_64"
        else "gtk.linux.x86_64"
        dependencyResolveDetails.useTarget(
            dependencyResolveDetails.requested.toString()
                .replace("\${osgi.platform}", platform)
        )
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            resolutionSwt(this, this@Build_gradle)
        }
    }
}

subprojects {
    configurations.all {
        resolutionStrategy {
            eachDependency {
                resolutionSwt(this, this@Build_gradle)
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testApi("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    api("org.junit.platform:junit-platform-suite:1.9.2")
    api("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
    if (mac)
        api("org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:3.124.200")
    else if (win)
        api("org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:3.124.200")
    else
        api("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.124.200")
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    if(!win)
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.compileJava {
    // use the project's version or define one directly
    //options.javaModuleVersion.set(provider { project.version as String })
}

tasks {
    register<Jar>("fatJar") {
        group = "distribution"
        archiveFileName.set("javardise-$os-$version.jar")
        destinationDirectory.set(layout.buildDirectory.dir("dist"))
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        )
        archiveClassifier.set(os)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        // manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .filter { !it.name.contains("junit") && !it.name.contains("opentest") }
            .map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output
        from(contents) {
            exclude("**/*.RSA","**/*.SF","**/*.DSA")
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
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
    if(mac)
        jvmArgs = listOf("-XstartOnFirstThread")
}

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into(layout.buildDirectory.dir("jars"))
}

task("copyJar", Copy::class) {
    from(tasks.jar).into(layout.buildDirectory.dir("jars"))
}

task("copyPlugins", Copy::class) {
    from(project.project("compilation").layout.buildDirectory.file("libs/compilation.jar"))
        .into(layout.buildDirectory.dir("jars"))
}

tasks.jpackage {
    dependsOn("copyDependencies", "copyJar", "copyPlugins")

    input = layout.buildDirectory.dir("jars").get().asFile.absolutePath
    destination = layout.buildDirectory.dir("dist").get().asFile.absolutePath

    appName = "Javardise"

    vendor = "pt.iscte"

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = application.mainClass.get()

    mac {
        // Generic parameter value for OS X build
        //icon = "icon.icns"
        javaOptions = listOf("-Dfile.encoding=UTF-8","-XstartOnFirstThread")
    }

    windows {
        type = ImageType.APP_IMAGE
        winConsole = false
        javaOptions = listOf("-Dfile.encoding=UTF-8")
    }

    linux {
        javaOptions = listOf("-Dfile.encoding=UTF-8")
    }
}

