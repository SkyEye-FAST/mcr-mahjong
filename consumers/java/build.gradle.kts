plugins { application }

repositories {
    mavenLocal {
        content { includeModule("top.skyeyefast", "mcr-mahjong") }
        // Exercise the Maven POM, not Gradle's .module file or a project dependency.
        metadataSources {
            mavenPom()
            ignoreGradleMetadataRedirection()
        }
    }
    mavenCentral { content { excludeModule("top.skyeyefast", "mcr-mahjong") } }
}

dependencies { implementation("top.skyeyefast:mcr-mahjong:0.1.0") }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
application { mainClass.set("example.JavaConsumer") }
