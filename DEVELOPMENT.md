# Development

Use JDK 17 or later and the checked-in Gradle wrapper. Commands below run from
the repository root; use `gradlew.bat` on Windows. Kotlin 2.3.20 and Dokka 2.2.0
are pinned in the build. The library targets Java 17 APIs and bytecode.

## Build and verification

```shell
./gradlew build
./gradlew publishToMavenLocal
./gradlew -p consumers clean verify
```

These are also the normal CI steps on JDK 17. `build` runs the native-free tests,
public ABI check and `verifyPublication`. The publication check inspects JVM
class versions, attribution, API documentation, source contents, Maven POM and
Gradle module metadata. It also checks that publishing and signing tasks are
configured. Remote publication is a separate operation.

The [independent consumers](consumers/README.md) resolve the locally published
artifact: Java uses the Maven POM, while Kotlin uses Gradle module metadata.
Consumer resolution requires the Maven Local artifact and keeps these checks
independent of the library's source sets.

| Verification | Evidence |
| --- | --- |
| Frozen upstream regressions | Complete fan tables, special-form shanten/effective tiles/waits, and discard-output digests match recorded C++ results. |
| Public-rule tests | Public fan values, candidate selection and rule decisions match the [review record](RULES_REVIEW.md#implementation-and-test-map). |
| API tests and compiled consumers | Input validation, immutable boundaries, concurrency and Java/Kotlin use behave as documented. |
| Generated ABI check | Supported public signatures match the reviewed dump. |
| Publication checks | Generated artifacts contain the expected metadata, documentation and attribution. |
| Opt-in C++ differential tests | Complete outputs match the specified upstream revision and switches; official-rule conformance is evaluated separately through the rule review. |

Routine tests are deterministic and run entirely on the JVM. Keep Minecraft,
Python and native runtimes out of the library and normal test suite.

## C++ differential testing

The native suite is an explicit developer check, separate from `build` and normal
CI. [tools/README.md](tools/README.md) provides checkout/build commands, oracle
protocol details and fixture-generation procedures. It requires CMake 3.20+ and
a C++17 compiler in addition to the JVM toolchain.

The upstream revision is recorded in [NOTICE](NOTICE), with nine normalized
source/license hashes in `tools/upstream.sha256` (UTF-8, optional BOM removed,
CRLF converted to LF). CMake and the differential suite verify those hashes;
the running oracle also reports a manifest fingerprint and its fan values.
The checkout and oracle stay in ignored `.reference/` and communicate with the
JVM over standard input/output.

When changing the upstream pin, update NOTICE, the hash manifest and the frozen
fixtures together, preserving the MIT attribution. Generate expected values from
the pinned C++ source and review the differences. Keep public-rule expectations
independent of the upstream fixture generator.

## Public ABI

Kotlin's built-in ABI validator tracks `top.skyeyefast.mcr` in the generated
`api/mcr-mahjong.api` dump, with the `internal` package excluded. `checkKotlinAbi`
runs under `check` and `build`. After a deliberate API change:

```shell
./gradlew updateKotlinAbi
```

Review the generated diff, then run the checks in a separate invocation:

```shell
./gradlew checkKotlinAbi
./gradlew publishToMavenLocal
./gradlew -p consumers clean verify
```

Keep the dump machine-generated and reviewed in Git; CI checks it without
regenerating it. ABI checks cover signatures. Changes to fan values or scoring
behavior need rule regressions even when signatures remain the same.

## Documentation and artifacts

The build produces the library JAR, sources JAR, Dokka HTML in the `javadoc`
classifier, Maven POM and Gradle module metadata. Each JAR includes LICENSE and
NOTICE in `META-INF`. The documentation JAR includes the project guides under
`guides/`. Keep its guide list in `build.gradle.kts` aligned with document moves,
and check links in both the repository and packaged guides.

The POM records the scoring profile, upstream commit and project rule-review
status. These describe the artifact's contract; release availability is tracked
by the repository tag and Maven Central publication. Use the changelog for actual
version changes, the rule review for scoring evidence, and this guide for
maintenance procedures.

## Release process

Version `0.1.1` has a signed `v0.1.1` Git tag and is available from
[Maven Central](https://central.sonatype.com/artifact/top.skyeyefast/mcr-mahjong/0.1.1).
A future release should follow these steps:

1. Review the intended API and scoring changes. Keep the profile, rule review and
   changelog aligned; resolve any newly recorded rule questions before tagging
   or publishing a changed profile.
2. Update the artifact version in the library build, Central Portal publication
   name, consumer dependencies and installation examples together.
3. Run the normal build and both independent consumers. Run the opt-in differential
   suite when changing the pinned algorithm, raw compatibility path or fixtures.
4. Inspect the generated artifacts and metadata. Commit with a Git signature and
   create a signed version tag for the reviewed revision.
5. Submit the signed bundle to Central Portal, review it there, and release it.
   Verify the published coordinates and artifacts before announcing availability.

Upload releases manually through Central Portal's
[Publish Component form](https://central.sonatype.org/publish/publish-portal-upload/).
Prepare a ZIP bundle using the Maven repository layout:
`top/skyeyefast/mcr-mahjong/<version>/`. Include the library JAR, sources JAR,
API documentation JAR, POM and Gradle module metadata, with an ASCII-armored
OpenPGP detached signature (`.asc`) and MD5/SHA-1 checksums for each artifact.
Sign the artifacts with your local GPG agent and verify each signature before
packaging. Publish the public key through a
[key server supported by Sonatype](https://central.sonatype.org/publish/requirements/gpg/).

In Central Portal, select **Publish Component**, enter the release coordinates
as the deployment name, and upload the ZIP. After validation, select **Publish**
and wait for **Published**. Verify the artifacts in Maven Central against the
local release bundle.
