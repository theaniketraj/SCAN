import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.java.gradle.plugin)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.versions)
    alias(libs.plugins.dependency.analysis)
}

group = "io.github.theaniketraj"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Kotlin Core
    implementation(libs.bundles.kotlin.core)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.bundles.kotlin.serialization)

    // Configuration Management
    implementation(libs.bundles.config)

    // JSON Processing
    implementation(libs.bundles.jackson)

    // Utilities
    implementation(libs.bundles.commons)
    implementation(libs.guava)

    // File Processing
    implementation(libs.bundles.file.processing)

    // HTML Generation
    implementation(libs.kotlinx.html)

    // Logging
    implementation(libs.bundles.logging)

    // Testing
    testImplementation(libs.bundles.testing)

    // Performance Testing
    testImplementation(libs.bundles.performance)

    // Test Runtime
    testRuntimeOnly(libs.junit.jupiter.engine)
}

gradlePlugin {
    website.set("https://github.com/theaniketraj/SCAN")
    vcsUrl.set("https://github.com/theaniketraj/SCAN.git")

    plugins {
        create("scanPlugin") {
            id = "io.github.theaniketraj.scan"
            implementationClass = "com.scan.plugin.ScanPlugin"
            displayName = "SCAN Security Plugin"
            description = "A comprehensive security scanning plugin for Gradle projects"
            tags.set(listOf("security", "scanning", "secrets", "static-analysis"))
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    // Set system properties for tests
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

    // Configure test JVM
    jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
}

spotless {
    kotlin {
        target("src/main/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
            .editorConfigOverride(mapOf(
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled"
            ))
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(mapOf(
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
            ))
    }
}

detekt {
    config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
    buildUponDefaultConfig = true
    allRules = false

    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt/detekt.html"))
        sarif.required.set(true)
        sarif.outputLocation.set(file("build/reports/detekt/detekt.sarif"))
    }
} // Documentation
tasks.dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}

// Version updates check
tasks.dependencyUpdates {
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

// Custom tasks for the plugin
tasks.register("publishToLocalRepo") {
    group = "publishing"
    description = "Publishes the plugin to local repository for testing"
    dependsOn("publishToMavenLocal")
}

tasks.register("runSecurityScan") {
    group = "verification"
    description = "Runs security scan on the plugin source code"
    doLast {
        println("Running security scan on plugin source...")
        // This would invoke our own plugin on itself
    }
}

// JMH Performance benchmarks
tasks.register("benchmark") {
    group = "benchmark"
    description = "Runs JMH performance benchmarks"
    dependsOn("jmh")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
