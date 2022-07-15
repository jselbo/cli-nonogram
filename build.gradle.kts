import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "me.jselbo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jline:jline:3.21.0")
    // Needed for Windows support
    runtimeOnly("org.fusesource.jansi:jansi:2.4.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

task<DefaultTask>("printClasspath") {
    doFirst {
        val properties = System.getProperties()
        val pathSeparator = properties.getProperty("path.separator")
        println(sourceSets.main.get().runtimeClasspath.files.joinToString(pathSeparator))
    }
}
