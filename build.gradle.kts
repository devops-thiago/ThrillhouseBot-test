plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

group = "com.thrillhouse"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.postgresql:postgresql:42.7.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.thrillhouse.meterfold.MainKt")
}

tasks.jar {
    archiveBaseName.set("meterfold")
    manifest {
        attributes("Main-Class" to "com.thrillhouse.meterfold.MainKt")
    }
}

tasks.test {
    useJUnitPlatform()
}
