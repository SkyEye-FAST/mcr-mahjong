# Changelog

## 0.1.0 (unreleased; rules review pending)

Separate the public revised-WMO-English profile (2013 postscript) from the pinned upstream compatibility
engine. Remove the five-point mixed-kong public enum entry and the public initial-hand
flag before release. Apply the sourced mixed-kong, concealed-kong, self-draw and
fan-combination adjustments to each candidate before maximum-score selection.
The complete upstream oracle vocabulary and its frozen regression tables are retained.
The fixed scoring profile is exposed as `McrMahjong.SCORING_PROFILE`. Mixed kongs
use the standard rulebook entry with a validated `FanCount.isMixedKongPair` marker
and an explicit six-point subtotal, not an extra fan or fictitious extra kong.

Add generated Kotlin/JVM ABI checks, publication-content verification and Maven
Local plus both consumer verifications to normal CI. Native C++ differential tests
remain opt-in. No formal version tag or public publication is made by this change.

Initial standalone Kotlin/JVM 17 candidate based on the pinned MIT-licensed
mahjong-algorithm shanten and fan calculator. Provides typed tile/meld/hand and
win-context models, complete fan counts, structural shanten, effective tiles,
waits and discard analysis. The raw compatibility engine retains upstream switches
and quirks; public scoring uses the separately documented rules adapter.

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
