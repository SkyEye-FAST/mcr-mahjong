import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import groovy.json.JsonSlurper
import java.io.DataInputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

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
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        filters {
            include { byNames.add("top.skyeyefast.mcr.**") }
            exclude { byNames.add("top.skyeyefast.mcr.internal.**") }
        }
    }
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
    from(listOf("README.md", "COMPATIBILITY.md", "CHANGELOG.md", "LICENSE", "NOTICE")) { into("guides") }
    from("tools/README.md") { into("guides/tools") }
    from("consumers/README.md") { into("guides/consumers") }
}

// Inspect publication files with JDK/Gradle APIs, without adding library dependencies.
tasks.register("verifyPublication") {
    group = "verification"
    description = "Verify JVM target, attribution, source/docs JARs, POM and module metadata."
    val archives = objects.fileCollection().from(
        tasks.named("jar"), tasks.named("sourcesJar"), tasks.named("javadocJar"),
    )
    val pom = layout.buildDirectory.file("publications/maven/pom-default.xml")
    val metadata = layout.buildDirectory.file("publications/maven/module.json")
    val expectedVersion = project.version.toString()
    inputs.files(archives, pom, metadata)
    inputs.property("artifactVersion", expectedVersion)
    dependsOn(archives, "generatePomFileForMavenPublication", "generateMetadataFileForMavenPublication")
    doLast {
        for (file in archives.files) ZipFile(file).use { zip ->
            fun text(path: String): String = zip.getInputStream(requireNotNull(zip.getEntry(path)) {
                "${file.name} is missing $path"
            }).bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(text("META-INF/LICENSE").contains("Jeff Wang")) { "Missing upstream MIT attribution" }
            check(text("META-INF/NOTICE").contains("44a178af08bf11f82a8993fddbe2fe8876ddd8f3"))
            val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
            check(entries.none { it.name.endsWith(".cpp") || it.name.endsWith(".exe") || it.name.endsWith(".dll") })
            when {
                file.name.endsWith("-sources.jar") -> check(entries.any { it.name.endsWith("/McrMahjong.kt") })
                file.name.endsWith("-javadoc.jar") -> {
                    check(text("index.html").contains("html", ignoreCase = true))
                    check(text("guides/COMPATIBILITY.md").isNotBlank())
                    check(entries.any { it.name.endsWith("/score.html") }) { "Missing generated score API documentation" }
                    check(entries.none { it.name.contains("top.skyeyefast.mcr.internal") })
                }
                else -> {
                    val classes = entries.filter { it.name.endsWith(".class") }
                    check(classes.isNotEmpty())
                    for (entry in classes) DataInputStream(zip.getInputStream(entry)).use { input ->
                        check(input.readInt() == 0xCAFEBABE.toInt())
                        input.readUnsignedShort()
                        check(input.readUnsignedShort() == 61) { "Not JVM 17: ${entry.name}" }
                    }
                }
            }
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val doc = factory.newDocumentBuilder().parse(pom.get().asFile)
        val xpath = XPathFactory.newInstance().newXPath()
        fun value(path: String): String = xpath.evaluate("string($path)", doc)
        check(value("/project/groupId") == "top.skyeyefast")
        check(value("/project/artifactId") == "mcr-mahjong")
        check(value("/project/version") == expectedVersion)
        check(value("/project/licenses/license/name") == "MIT License")
        check(value("/project/properties/mcr.scoring.profile") == "wmo-2006-en")
        check(value("/project/dependencies/dependency/artifactId") == "kotlin-stdlib")
        check(value("/project/dependencies/dependency/scope") == "compile")
        check(xpath.evaluate("count(/project/dependencies/dependency)", doc) == "1")
        val module = JsonSlurper().parse(metadata.get().asFile) as Map<*, *>
        check((module["component"] as Map<*, *>)["version"] == expectedVersion)
        val variants = (module["variants"] as List<*>).map { it as Map<*, *> }
        for (name in listOf("apiElements", "runtimeElements")) {
            val variant = variants.single { it["name"] == name }
            check((variant["attributes"] as Map<*, *>)["org.gradle.jvm.version"] == 17)
            val dependencies = (variant["dependencies"] as List<*>).map { it as Map<*, *> }
            check(dependencies.size == 1 && dependencies.single()["module"] == "kotlin-stdlib")
        }
        logger.lifecycle("Verified Maven publication: JVM 17, API dependencies, sources, Dokka and MIT attribution")
    }
}

tasks.check { dependsOn("verifyPublication") }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "MCR Mahjong"
                description = "Pure Kotlin/JVM Mahjong Competition Rules shanten and fan calculation."
                inceptionYear = "2026"
                properties.put("mcr.upstream.commit", "44a178af08bf11f82a8993fddbe2fe8876ddd8f3")
                properties.put("mcr.scoring.profile", "wmo-2006-en")
                properties.put("mcr.release.status", "rules-review-pending")
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
