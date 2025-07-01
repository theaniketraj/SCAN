plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "1.2.1"
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.dokka") version "1.9.10"
}

group = "com.scan"
version = "1.0.0"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Gradle API
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    
    // YAML Processing
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
    
    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // File System Operations
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("commons-io:commons-io:2.14.0")
    
    // Regular Expressions Enhancement
    implementation("org.apache.commons:commons-text:1.11.0")
    
    // HTML Generation for Reports
    implementation("org.jsoup:jsoup:1.16.2")
    
    // Testing Dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Gradle Test Kit for Plugin Testing
    testImplementation(gradleTestKit())
    
    // Test Fixtures for Integration Tests
    testImplementation("org.gradle:gradle-tooling-api:8.4")
}

gradlePlugin {
    website.set("https://github.com/theaniketraj/SCAN")
    vcsUrl.set("https://github.com/theaniketraj/SCAN.git")
    
    plugins {
        create("scanPlugin") {
            id = "com.scan"
            displayName = "Security Scan Plugin"
            description = "A Gradle plugin for detecting security vulnerabilities and secrets in your codebase before version control"
            tags.set(listOf("security", "secrets", "vulnerability", "scanning", "ci-cd"))
            implementationClass = "com.scan.plugin.ScanPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Scan Gradle Plugin")
                description.set("A comprehensive Gradle plugin for detecting security vulnerabilities and secrets in your codebase")
                url.set("https://github.com/theaniketraj/SCAN")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("scan-team")
                        name.set("Scan Team")
                        email.set("scan-team@example.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/theaniketraj/SCAN.git")
                    developerConnection.set("scm:git:ssh://github.com/theaniketraj/SCAN.git")
                    url.set("https://github.com/theaniketraj/SCAN")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/theaniketraj/SCAN")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.geteSCANketraj
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

tasks.test {
    useJUnitPlatform()
    
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // Ensure test resources are available
    dependsOn("processTestResources")
}

tasks.check {
    dependsOn("validatePlugins")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Scan Team"
        )
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

// Custom task to validate plugin configuration
tasks.register("validatePlugins") {
    group = "verification"
    description = "Validates plugin configuration and metadata"
    
    doLast {
        val pluginFile = file("src/main/resources/META-INF/gradle-plugins/com.scan.properties")
        if (!pluginFile.exists()) {
            throw GradleException("Plugin descriptor file not found: ${pluginFile.path}")
        }
        
        val properties = java.util.Properties()
        pluginFile.inputStream().use { properties.load(it) }
        
        val implementationClass = properties.getProperty("implementation-class")
        if (implementationClass != "com.scan.plugin.ScanPlugin") {
            throw GradleException("Plugin implementation class mismatch in descriptor")
        }
        
        println("✓ Plugin configuration validated successfully")
    }
}

// Task to generate plugin usage documentation
tasks.register("generateDocs") {
    group = "documentation"
    description = "Generates plugin documentation"
    
    dependsOn("dokkaHtml")
    
    doLast {
        val docsDir = file("build/docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        
        val usageFile = file("build/docs/USAGE.md")
        usageFile.writeText(generateUsageDocumentation())
        
        println("✓ Documentation generated at: ${usageFile.path}")
    }
}

// Custom task for running integration tests
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs integration tests"
    
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    
    useJUnitPlatform()
    
    include("**/integration/**")
    
    systemProperty("gradle.user.home", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("plugin.version", version)
    
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Task to run performance benchmarks
tasks.register<Test>("performanceTest") {
    group = "verification"
    description = "Runs performance benchmark tests"
    
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    
    useJUnitPlatform()
    
    include("**/PerformanceTest*")
    
    systemProperty("scan.performance.test", "true")
    
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Ensure integration tests run as part of check
tasks.check {
    dependsOn("integrationTest")
}

// Configure build order
tasks.compileKotlin {
    dependsOn("processResources")
}

tasks.processResources {
    // Ensure pattern files are included
    from("src/main/resources") {
        include("**/*.yml")
        include("**/*.yaml")
        include("**/*.properties")
    }
}

// Helper function to generate usage documentation
fun generateUsageDocumentation(): String {
    return """
# Scan Gradle Plugin Usage

## Basic Usage

Apply the plugin to your project:

```kotlin
plugins {
    id("com.scan") version "$version"
}
```

## Configuration

Configure the plugin in your build script:

```kotlin
scan {
    // Enable or disable the plugin
    enabled = true
    
    // Fail build on security issues found
    failOnIssues = true
    
    // Configure scan scope
    includePatterns = listOf("**/*.kt", "**/*.java", "**/*.properties")
    excludePatterns = listOf("**/test/**", "**/build/**")
    
    // Configure detectors
    detectors {
        patterns {
            enabled = true
            customPatterns = file("custom-patterns.yml")
        }
        
        entropy {
            enabled = true
            threshold = 4.5
        }
        
        contextAware {
            enabled = true
            ignoreComments = true
            ignoreStrings = false
        }
    }
    
    // Configure reporting
    reports {
        console {
            enabled = true
            verbose = false
        }
        
        json {
            enabled = true
            outputFile = file("build/reports/scan/scan-results.json")
        }
        
        html {
            enabled = true
            outputFile = file("build/reports/scan/scan-results.html")
        }
    }
}
```

## Tasks

- `scan`: Run security scan on the project
- `scanCheck`: Run scan and fail build if issues found
- `scanReport`: Generate detailed scan reports

## For more information, visit: https://github.com/theaniketraj/SCAN
""".trimIndent()
}

// Configure Kotlin compilation options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
}

// Configure test source sets
sourceSets {
    test {
        resources {
            srcDir("src/test/resources")
        }
    }
}

// Clean task configuration
tasks.clean {
    delete("build")
    delete(".gradle")
}