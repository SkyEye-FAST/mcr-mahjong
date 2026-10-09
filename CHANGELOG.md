# Changelog

## 0.1.0 — 2026-09-24

Initial Kotlin/JVM 17 release, based on the MIT-licensed `mahjong-algorithm`
revision recorded in [NOTICE](NOTICE).

- Adds typed tiles, melds, pre-win hands and win contexts; fan scoring; structural
  shanten, effective tiles, waits and discard analysis across five hand forms.
- Defines the `wmo-2014-zh` public scoring profile with 81 fans. Applies mixed-kong,
  concealed-kong, self-draw and fan-combination adjustments before candidate
  selection, with a validated six-point mixed-kong subtotal. Records the adopted
  Knitted Straight residual-wait interpretation in the [rule review](RULES_REVIEW.md).
- Provides validated inputs, immutable snapshots and stateless evaluation, with
  Kotlin and Java entry points.
- Adds deterministic regressions, independent public-rule assertions, opt-in
  C++ differential testing, generated ABI checks and compiled artifact consumers.
- Publishes library, source and Dokka documentation JARs with Maven POM and Gradle
  module metadata. Adds Central Portal publishing and OpenPGP artifact signing.
