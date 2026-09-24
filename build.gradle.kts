import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    `java-library`
    `maven-publish`
}

group = "top.skyeyefast"
version = "0.1.0-SNAPSHOT"

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

dependencies {
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
    val oracle = providers.gradleProperty("oraclePath")
    doFirst {
        require(oracle.isPresent) { "Supply -PoraclePath=/absolute/path/to/oracle (see tools/README.md)" }
        systemProperty("mcr.oracle", oracle.get())
    }
    outputs.upToDateWhen { false }
}

tasks.withType<Jar>().configureEach {
    from(listOf("LICENSE", "NOTICE")) { into("META-INF") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "MCR Mahjong"
                description = "Pure Kotlin/JVM Mahjong Competition Rules shanten and fan calculation."
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
