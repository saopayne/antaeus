plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    implementation("io.github.microutils:kotlin-logging:1.6.24")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")

}