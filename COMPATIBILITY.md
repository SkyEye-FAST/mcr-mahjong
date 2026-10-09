# Compatibility contract

## Public scoring profile

`McrMahjong.score` uses `wmo-2014-zh`: the WMO Chinese *麻将竞赛规则*, first edition
and first printing, December 2014, ISBN `978-7-5143-3183-7`. The identifier names
that Chinese edition and the project decisions documented in
[RULES_REVIEW.md](RULES_REVIEW.md), which preserves the PDF fingerprint, printed
page references and the relationship to the English Green Book and other sources.

The public `Fan` vocabulary contains 81 identifiers. Scoring recognizes applicable
fans, applies exclusions and evaluates competing candidates. `ScoreResult.Winning`
reports the selected fan breakdown, `totalFan` including flowers, `nonFlowerFan`
and `meetsMinimum` (`nonFlowerFan >= 8`). A winning shape below eight non-flower
points remains `Winning`. The caller handles full-game legality and settlement.

The project's recorded review covers the scoring decisions and software checks below.
The POM field `mcr.rules.review.status=complete` refers to this project review,
not WMO certification. The review adopts the documented Knitted Straight
residual-wait interpretation. The scope of Nine Gates terminal-pung exclusion
remains open; current behavior and competing readings are recorded in the review.

### Differences from upstream scoring

| Area | Public handling |
| --- | --- |
| One exposed and one concealed kong | Six points under `TWO_MELDED_KONGS`, marked by `FanCount.isMixedKongPair`; use `FanCount.points` for the subtotal. |
| Two Concealed Kongs | Eight points. |
| Three/Four Kongs | Apply the concealed-kong clauses and the concealed-pung combinations recorded in the review. |
| Special forms on self-draw | Add Fully Concealed Hand where the Chinese clauses specify it. |
| Compound Seven Pairs | Absorb one inevitable Tile Hog for All Green/All Terminals; additional quads score. |
| All Green | Retain the permitted Half Flush or Full Flush combination. |
| All Terminals | Retain independent Double Pungs, with Triple Pung absorbing its constituent pairs. |

`StandardMcr` applies sourced corrections to every scoring candidate before choosing
the highest public score. Equal public scores retain upstream traversal/tie
preferences. The [review record](RULES_REVIEW.md#scoring-decisions) gives the raw
upstream behavior, source clauses and implemented handling for each decision.

## Upstream baseline

The algorithm baseline is Jeff Wang's `mahjong-algorithm` at
`44a178af08bf11f82a8993fddbe2fe8876ddd8f3`; see [NOTICE](NOTICE).
The raw engine retains 82 entries, including the upstream five-point mixed-kong
extension and its six-point Two Concealed Kongs value. Frozen fixtures and C++
differential tests check compatibility with this revision and profile. Public-rule
tests separately check the scoring decisions above.

| Fixed switch | Meaning and public treatment |
| --- | --- |
| `SUPPORT_CONCEALED_KONG_AND_MELDED_KONG=1` | Preserve the internal mixed-kong entry for C++ parity; public scoring awards six points. |
| `KNITTED_STRAIGHT_BODY_WITH_ECS=1` | Evaluate waits after removing the knitted body, using the residual-structure interpretation in the rule review. |
| `DISTINGUISH_PURE_SHIFTED_CHOWS=0` | One public three-shifted and one four-shifted fan cover their permitted step sizes. |
| `NINE_GATES_WHEN_BLESSING_OF_HEAVEN=1` | Preserve the initial-hand branch, including its `r = 2` extra-nine quirk, in the raw engine. Public scoring takes an explicit pre-win hand and winning tile. |
| `SUPPORT_BLESSINGS=0` | Use the standard 81-fan public scope. |
| `STRICT_98_RULE` undefined | Retain the pinned historical upstream policy; public rule adjustments are applied separately. |
| `MAHJONG_ALGORITHM_ENABLE_SHANTEN` defined | Enable structural shanten and effective-tile analysis. |
| C++ `MAX_DIVISION_CNT=20` | Upstream buffer size; Kotlin stores divisions in a per-call list. |

Decomposition and structural shanten/effective-tile/wait calculations retain the
upstream algorithms. Structural results include theoretical zero-copy waits.
Last-copy and kong-involved flags retain upstream's physical-input corrections.
The public API exposes the fixed profile described here.

## API boundary

Invalid hands and null elements in Java-supplied collections raise
`IllegalArgumentException`. A null passed for a non-null parameter uses Kotlin's
standard `NullPointerException`. Value objects validate construction and Kotlin
`copy` calls; `FanCount` also checks multiplication overflow. Obtain aggregate
results through `McrMahjong` in both Kotlin and Java. Internal packing helpers on
public types use `@JvmSynthetic` to keep them out of Java source access.

Returned collections are immutable snapshots. `Hand` equality includes tile and
meld order. Each evaluation keeps its own state and snapshots retained inputs;
callers keep mutable collections unchanged while the snapshot is taken.

`HandAnalysis.forms` includes only applicable forms. The native test adapter uses
`INT_MAX` and an empty effective-tile table for unavailable forms, guarding the
upstream honors-and-knitted wrapper's undefined buffer in that case.

The upstream discard callback uses `-1` when the discarded tile completes the
original hand. `DiscardAnalysis.shanten` describes the remaining 13-tile hand,
and `completesForms` preserves the completion information. Differential tests
reconstruct the upstream stream from these results. The public API returns all
applicable forms for callers to filter; the upstream `form_flag | FLAG` filter
expression does not restrict them.

Visible-tile accounting and eight-point qualification are public API conveniences
built on the upstream calculations. Applications supply game chronology, discarder
eligibility, wall state and call legality.

## Versions and supported surface

The library version (`0.1.1`) identifies the artifact and its API/implementation.
The scoring profile (`wmo-2014-zh`) identifies the rule source and the documented
interpretation. The upstream commit identifies the port's algorithm baseline.
These identifiers serve separate purposes: a library bug fix may retain the same
rule source, while adopting a different rule edition or policy requires an explicit
profile decision. Behavior changes belong in [CHANGELOG.md](CHANGELOG.md), with
updated rule evidence and tests where relevant.

The supported API is `top.skyeyefast.mcr`. Consumers should store tile/fan names;
packed integers, enum ordinals, the `internal` package and the test protocol are
implementation details. Public API changes require review and compiled consumer
checks. The generated ABI baseline detects signature changes; rule tests cover
scoring behavior. [DEVELOPMENT.md](DEVELOPMENT.md#public-abi) describes both checks.

Java 17 is the bytecode and Java API baseline. Kotlin's standard library is the
sole direct runtime dependency and is published in Maven `compile` scope because
Kotlin-generated public members expose its types to Java callers.
