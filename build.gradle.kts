import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.java.gradle.plugin)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
    alias(libs.plugins.dependency.analysis)
}

group = "com.scan"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Kotlin Core
    implementation(libs.bundles.kotlin.core)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.bundles.kotlin.serialization)
    
    // Gradle API
    implementation(libs.gradle.api)
    
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
    testImplementation(libs.gradle.testkit)
    
    // Performance Testing
    testImplementation(libs.bundles.performance)
    
    // Test Runtime
    testRuntimeOnly(libs.junit.jupiter.engine)
    
    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}")
}

gradlePlugin {
    website.set("https://github.com/theaniketraj/SCAN")
    vcsUrl.set("https://github.com/theaniketraj/SCAN.git")
    
    plugins {
        create("scanPlugin") {
            id = "com.scan"
            implementationClass = "com.scan.plugin.ScanPlugin"
            displayName = "SCAN Security Plugin"
            description = "A comprehensive security scanning plugin for Gradle projects"
            tags.set(listOf("security", "scanning", "secrets", "static-analysis"))
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
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

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get()).setUseExperimental(true)
        licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
    }
    
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

// Documentation
tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
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