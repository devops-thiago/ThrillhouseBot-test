plugins {
    kotlin("jvm") version "2.0.20"
    application
}

group = "com.thrillhouse"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.thrillhouse.costalloc.MainKt")
}

tasks.jar {
    archiveBaseName.set("costalloc")
}

tasks.test {
    useJUnitPlatform()
}
