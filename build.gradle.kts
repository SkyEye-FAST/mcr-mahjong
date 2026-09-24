import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.dokka") version "2.2.0"
    `java-library`
    `maven-publish`
}

group = "top.skyeyefast"
version = "0.1.0"

repositories { mavenCentral() }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

dependencies {
    // Kotlin-generated public members (for example enum entries) expose stdlib types to Java.
    api(kotlin("stdlib"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform { excludeTags("differential") }
}

tasks.register<Test>("differentialTest") {
    description = "Compare the Kotlin port with an explicitly supplied, pinned C++ oracle."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("differential") }
    systemProperty("mcr.oracle", providers.gradleProperty("oraclePath").getOrElse(""))
    doFirst {
        val oracle = (this as Test).systemProperties["mcr.oracle"].toString()
        require(oracle.isNotBlank() && File(oracle).isFile) {
            "Supply -PoraclePath=/absolute/path/to/oracle (see tools/README.md)"
        }
    }
    outputs.upToDateWhen { false }
}

tasks.withType<Jar>().configureEach {
    from(listOf("LICENSE", "NOTICE")) { into("META-INF") }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dokka {
    dokkaPublications.html {
        offlineMode.set(true)
        failOnWarning.set(true)
    }
    dokkaSourceSets.main {
        jdkVersion.set(17)
        perPackageOption {
            matchingRegex.set("top\\.skyeyefast\\.mcr\\.internal(\\..*)?")
            suppress.set(true)
        }
    }
}

// Real generated API reference, using Dokka HTML in the standard javadoc classifier.
// Dokka is a build dependency only; it is never part of the published runtime.
tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    from(listOf("README.md", "COMPATIBILITY.md", "LICENSE", "NOTICE")) { into("guides") }
    from("tools/README.md") { into("guides/tools") }
    from("consumers/README.md") { into("guides/consumers") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "MCR Mahjong"
                description = "Pure Kotlin/JVM Mahjong Competition Rules shanten and fan calculation."
                inceptionYear = "2026"
                properties.put("mcr.upstream.commit", "44a178af08bf11f82a8993fddbe2fe8876ddd8f3")
                developers {
                    developer {
                        id = "SkyEye-FAST"
                        name = "SkyEye_FAST"
                        email = "skyeyefast@foxmail.com"
                        url = "https://github.com/SkyEye-FAST"
                    }
                }
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/license/mit/"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}
