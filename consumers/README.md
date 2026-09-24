# Independent consumers

These projects resolve `top.skyeyefast:mcr-mahjong:0.1.0` from Maven Local.
They are not included in the library build, do not access its source sets, and
have no project/composite-build dependency on it. The parent wrapper is only
used to launch Gradle.

From the repository root, on JDK 17:

```shell
./gradlew publishToMavenLocal
./gradlew -p consumers clean verify
```

Use `gradlew.bat` on Windows. `java` has no Kotlin plugin or manually declared
standard-library dependency; it deliberately ignores Gradle metadata to verify
the Maven POM and its transitive dependencies. `kotlin` uses normal Gradle
module metadata and checks Kotlin's sealed-result and named-argument APIs.

These are smoke tests of a published artifact, not a duplicate algorithm suite.
