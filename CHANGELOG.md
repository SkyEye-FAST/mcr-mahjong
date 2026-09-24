# Changelog

## 0.1.0 (unreleased)

Pins public scoring to the WMO Chinese *麻将竞赛规则*, first edition/first printing,
December 2014 (`wmo-2014-zh`), separately from the pinned upstream compatibility
engine. Removes the five-point mixed-kong public enum entry and the public initial-hand
flag. Applies sourced mixed-kong, concealed-kong, self-draw and fan-combination
adjustments to each candidate before maximum-score selection. Adds the missing Four
Kongs concealed-kong corrections; the complete upstream oracle vocabulary and frozen
regression tables remain unchanged. Resolves Combination Dragon waits from the
Chinese non-splitting clause, its explicit Single Wait example and the unique-wait
definitions, with separate public tests for overlapping and ambiguous residual waits.
The exact interpretation and pages are pinned in COMPATIBILITY.md, and the rule review
for this profile is complete.
The fixed scoring profile is exposed as `McrMahjong.SCORING_PROFILE`. Mixed kongs
use the standard rulebook entry with a validated `FanCount.isMixedKongPair` marker
and an explicit six-point subtotal, not an extra fan or fictitious extra kong.

Adds generated Kotlin/JVM ABI checks, publication-content verification and Maven
Local plus both consumer verifications to normal CI. Native C++ differential tests
remain opt-in. The candidate has no version tag and has not been published to Maven
Central; remote CI and the signed release steps remain to be completed.

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

Prepared and verified for Maven Local. Adds Central Portal publishing and OpenPGP
artifact-signing configuration without storing credentials in the repository.
No Maven Central deployment or Minecraft integration has occurred.
