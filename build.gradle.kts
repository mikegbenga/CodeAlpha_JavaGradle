plugins {
    application
    jacoco
}

group = "com.codealpha"
// CI passes -PappVersion=1.0.<run number>; local builds are "dev"
version = (findProperty("appVersion") as String?) ?: "1.0.0-dev"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Build with whatever JDK 17+ is installed, but always produce Java 17 bytecode
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

application {
    mainClass = "com.codealpha.tasks.App"
}

// ---------- tests + coverage ----------
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()   // build fails under 80% line+branch coverage
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ---------- packaging ----------
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

// Single runnable jar with Gson inside: java -jar build/libs/codealpha-task-api-<version>-all.jar
val fatJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Assembles a runnable jar that includes all runtime dependencies."
    archiveClassifier = "all"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.from(tasks.jar.get().manifest)
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.map { cp -> cp.map { if (it.isDirectory) it else zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class")
}

tasks.assemble {
    dependsOn(fatJar)
}
