import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    idea
    kotlin("jvm") version "1.3.20" apply false
}

allprojects {
    group = "io.pleo"
    version = "1.0"

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
}
