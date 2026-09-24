tasks.register("verify") {
    group = "verification"
    description = "Run independent Java/POM and Kotlin/Gradle-metadata consumers."
    dependsOn(":java:run", ":kotlin:run")
}
