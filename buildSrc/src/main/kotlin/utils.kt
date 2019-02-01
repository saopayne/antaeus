import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

const val koinVersion = "1.0.2"
const val junitVersion = "5.3.2"

/**
 * Configures the current project as a Kotlin project by adding the Kotlin `stdlib` as a dependency.
 */
fun Project.kotlinProject() {
    dependencies {
        // Kotlin libs
        "implementation"(kotlin("stdlib-jdk8"))

        // Koin
        "implementation"("org.koin:koin-core:$koinVersion")
        "implementation"("org.koin:koin-core-ext:$koinVersion")
        "testImplementation"("org.koin:koin-test:$koinVersion")
        "implementation"("org.koin:koin-java:$koinVersion")

        // Mockk
        "testImplementation"("io.mockk:mockk:1.9")

        // JUnit 5
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        "runtime"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }
}