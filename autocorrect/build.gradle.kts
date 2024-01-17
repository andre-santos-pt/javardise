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
    implementation(project(":compilation"))
    testApi("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    api("org.junit.platform:junit-platform-suite:1.9.2")
    //implementation("com.google.guava:guava:33.0.0-jre")
}

application {
    mainClass.set("pt.iscte.javardise.editor.MainKt")
    if(!win)
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}