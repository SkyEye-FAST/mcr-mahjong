# Changelog

## 0.1.0

Initial standalone Kotlin/JVM 17 release of the pinned MIT-licensed
mahjong-algorithm shanten and fan calculator. Provides typed tile/meld/hand and
win-context models, complete fan counts, structural shanten, effective tiles,
waits and discard analysis. Includes all upstream default rule switches and
documented upstream quirks rather than independently redefining them.

Release hardening makes aggregate result construction private, hides internal
packing helpers from Java source, rejects null collection elements and fan-point
overflow, and verifies immutable snapshots and concurrent evaluation. Kotlin
stdlib is an API dependency for Java callers of Kotlin-generated members.

The release includes generated Dokka API documentation, sources, Maven POM and
Gradle metadata, plus independent Java/POM and Kotlin/Gradle-metadata consumers.
Native-free regressions cover every enabled fan and targeted historical bugs;
the opt-in C++ suite checks pinned source fingerprints, the complete upstream
score-case inventory and deterministic structural/scoring corpora.

Prepared and verified for Maven Local. No public Maven deployment or Minecraft
integration is part of this release.
