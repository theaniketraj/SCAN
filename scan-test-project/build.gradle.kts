plugins {
    id("java")
    id("io.github.theaniketraj.scan")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

// SCAN plugin configuration
scan {
    failOnSecrets.set(false)
    verbose.set(true)
    generateHtmlReport.set(true)
    generateJsonReport.set(true)  // Re-enable JSON
    entropyThreshold.set(4.5)
    
    // Test exclude patterns
    excludePatterns.set(setOf(
        "**/Application.java"  // This file should NOT be scanned
    ))
}
