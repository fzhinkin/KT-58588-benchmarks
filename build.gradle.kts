import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    kotlin("jvm") version "1.9.20-Beta"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.9"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

benchmark {
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            this.jmhVersion = "1.37"
        }
    }
}
