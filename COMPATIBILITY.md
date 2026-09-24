# Compatibility contract

The public API is the documented `top.skyeyefast.mcr` package. The
`top.skyeyefast.mcr.internal` package, packed integers, enum ordinals and test
protocol are implementation details and are not serialization contracts.
Consumers should store tile/fan names rather than internal numbers.

## Two separate contracts

**Public scoring profile: `wmo-2006-en`.** `McrMahjong.score` uses the WMO 2006
English *Mahjong Competition Rules* text identified below, with the documented
knitted-wait interpretation. This is not a claim of interchangeability with the
original 1998 Chinese rules or later online-house-rule profiles. `Fan` contains
81 identifiers; their numeric enum positions are not official rulebook numbers.

**Upstream compatibility baseline:** the exact commit and default switches in
NOTICE. Internal `UpstreamFan` retains all 82 entries, including the five-point
mixed-kong extension. The C++ protocol, frozen C++ fixtures and differential suite
exercise this unnormalized baseline. Passing them proves the port's compatibility
with that source revision, not conformity of public scores to a rules standard.

`StandardMcr` is a narrow candidate-scoring adapter. The original decomposition,
pattern detection and traversal remain in `FanCalculator`; an internal callback
normalizes each candidate before choosing the highest score. Normalizing only the
upstream winner is wrong: `44556m445566s55p6m` on self-draw has a 49-point regular
upstream winner, but a 52-point seven-pairs candidate in this public profile.
No user-selectable collection of house-rule switches or compatibility fallback is exposed.

### Rule sources

The primary reference actually inspected is the [WMO 2006 English MCR booklet,
September 2006 copy](https://ftp.space.dtu.dk/pub/fch/mahjong/mje0906-a5bog.pdf).
It is a two-pages-per-sheet booklet: use the named fan definitions and printed
page numbers, not PDF indices. The [Danish association's rules page](https://mahjong.dk/regler)
identifies the September 2006 English translation and warns about unclear wording.
This candidate must not be presented as independently certified compliance with
the original 1998 Chinese rules, or as a verified implementation of every later WMO edition.

The [2024 World Mahjong Championship organizer's published clarifications](https://www.mahjong-ca.org/%E8%A1%A5%E5%85%85%E8%A7%84%E5%88%99%E5%92%8C%E8%AE%A1%E5%88%86%E8%A1%A8/)
provide a direct reference for the retained knitted-body wait interpretation and
non-implied fan combinations. They are identified as tournament clarifications,
not retroactively described as wording present in the 1998 rules.

### Audited scoring differences

| Case | Pinned upstream | Public `wmo-2006-en` treatment | Reference |
| --- | --- | --- | --- |
| One concealed and one exposed kong | Separate five-point entry | Six points under the Two Melded Kongs entry's explicit mixed-kong clause | fan 57, printed p. 21 |
| Two concealed kongs | Six points | Eight points | Two Concealed Kongs, eight-point group |
| Self-drawn mandatory-concealed forms | Normally the one-point Self Drawn addition | Fully Concealed Hand where explicitly allowed; no duplicate Self Drawn | fans 4, 6, 7, 12, 19, 20, 34 |
| All Green with green dragons | Half Flush removed | Half Flush retained | fan 3, printed p. 33, including the seven-pairs self-draw example |
| All Terminals | Double Pung removed | Permitted pairs of pungs restored, except where absorbed by Triple Pung; never manufactured for a seven-pairs decomposition | fan 8 |

The mixed-kong case is represented by one `FanCount` for the rulebook's
`TWO_MELDED_KONGS` entry with `isMixedKongPair == true` and `points == 6`.
The flag explicitly distinguishes it from two physically exposed kongs. Neither
individual Melded Kong nor Concealed Kong is awarded again. `Fan.points` remains
the entry's base value (4); applications must use `FanCount.points` for the awarded
subtotal and the flag when displaying the mixed case. This avoids both an 82nd fan
and a misleading breakdown that reports two exposed kongs plus a concealed kong.
The flag is rejected on every other fan or when its count is not one.

The two-kong adjustment does not apply to subsets of Three/Four Kongs. Their
concealed-pung combination handling remains the raw engine's existing handling;
it is not independently expanded from other editions' wording. The 2006 Three
Kongs definition explicitly allows Three Concealed Pungs when all three are concealed.

Source copies must not be silently merged: the September 2006 booklet's fan 17
(printed p. 38) and the differently edited [Dutch-hosted text](https://mahjongbond.org/wp-content/uploads/2015/07/NMB-MCR-Groene-boekje.pdf)
(printed p. 39) do not have identical concealed-kong combination wording. The latter
is not the normative source for this named profile. Changing that choice requires
an explicit profile/source review and independent expected results, not a change
to the raw C++ fixtures.

The [WMO 2021 document](https://www.mindmahjong.com/adobe/MCR2021.pdf) has not been
fully retrieved and audited here. It is not a normative source for this candidate,
and no conformance claim to that edition is made. The corrections above are backed
by the inspected September 2006 booklet, not inferred from search snippets about
other editions.

The former public `WinContext.initial` and mixed-kong enum entry are removed
before the first release. The initial-hand/nine-gates branch, including its
`r = 2` extra-nine quirk, remains only in the raw oracle-compatible engine.
The public contract takes an explicit pre-win hand and winning tile and does not
offer a blessing/initial-hand house-rule option.

### Upstream feature switches, classified

| Fixed switch | Classification | Public treatment |
| --- | --- | --- |
| `SUPPORT_CONCEALED_KONG_AND_MELDED_KONG=1` | Historical upstream extension/encoding | Preserve five-point internal entry for C++ parity; apply the sourced six-point public combination above. |
| `KNITTED_STRAIGHT_BODY_WITH_ECS=1` | Interpretation of standard wait combinations, clarified at tournament level | Retain the body-overlap edge/closed/single interpretation, with the 2024 organizer source above; not a new fan. |
| `DISTINGUISH_PURE_SHIFTED_CHOWS=0` | Standard fan taxonomy; optional split is an upstream extension | One public three-shifted and one four-shifted fan; permitted step sizes do not become separate public fans. See fans 16 and 30. |
| `NINE_GATES_WHEN_BLESSING_OF_HEAVEN=1` | Upstream initial-hand interpretation and historical implementation quirk | Keep only in raw compatibility path; public context has no initial-hand flag. |
| `SUPPORT_BLESSINGS=0` | Standard 81-fan scope; enabled alternative adds house-rule fans | No Heaven/Earth/Human blessing fans in the public API. |
| `STRICT_98_RULE` undefined | Historical upstream policy selector, not a conformance certificate | Retain raw engine behavior; public semantic corrections are explicit above. Turning this macro on/off alone does not establish compliance with either edition. |
| `MAHJONG_ALGORITHM_ENABLE_SHANTEN` defined | Build/algorithm availability, not a scoring rule | Shanten and useful-tile analysis are available; the flag does not authorize a win or select a rules edition. |
| `MAX_DIVISION_CNT=20` in C++ | Buffer size, not a rule | Kotlin keeps a per-call list of divisions; no artificial 20-division rules limit is exposed. |

Structural shanten/useful/wait tables retain upstream behavior, including theoretical
zero-copy waits. They are shape analysis, not legal-win authorization. Last-copy
and kong-involved flags keep upstream's physical-input corrections. Equal normalized
scores keep upstream's deterministic traversal/tie preferences.

## API and ABI maintenance

Kotlin Gradle plugin 2.3.20's built-in ABI validator is explicitly enabled. The
machine-generated `api/mcr-mahjong.api` dump freezes the supported public package while excluding
`top.skyeyefast.mcr.internal`. `checkKotlinAbi` runs under `check`/`build`.
Generate updates with `updateKotlinAbi` in a **separate invocation**, review the
diff, then run `checkKotlinAbi` and the consumers. Never edit the dump by hand or
regenerate it in CI. The plugin's configuration DSL is opt-in/experimental; the
project pins its Kotlin plugin version instead of depending on an unversioned tool.

This pre-release ABI baseline detects accidental symbol/signature changes. It
does not certify rule values, behavior or every Kotlin/Java source-compatibility
property. Independent rule tests and compiled consumers cover those other concerns.

## Intentional API-boundary differences

Invalid hands throw `IllegalArgumentException` before calculation rather than
exposing C++ error codes, invalid memory access or undefined output buffers.
Null elements in Java-supplied collections are rejected with `IllegalArgumentException`;
null passed for a non-null parameter uses Kotlin's standard `NullPointerException`.
Input value objects validate both construction and Kotlin `copy` calls. `FanCount`
also rejects multiplication overflow. Aggregate result constructors are private:
obtain results through `McrMahjong` in both Kotlin and Java. Internal packing and
array helpers on public types are hidden from Java source with `@JvmSynthetic`;
the entire `internal` package remains outside the supported API.

All returned collections are immutable snapshots. Hand equality includes tile
and meld order, rather than testing structural equivalence. Evaluation retains no
caller-owned mutable collections or shared evaluation state; callers must not
modify a mutable input collection concurrently while a call is taking its snapshot.

Unavailable special forms are omitted from `HandAnalysis.forms`; the native
adapter represents them as `INT_MAX` with an empty useful table without invoking
upstream on an unsupported count. In particular, upstream's honors-and-knitted
wrapper can otherwise copy an uninitialized buffer.

The upstream discard callback uses `-1` when returning a tile that completes the
original hand. Here `DiscardAnalysis.shanten` consistently describes the remaining
13-tile hand, and `completesForms` preserves that extra information. Differential
tests reconstruct and compare the exact upstream stream. Its ineffective
`form_flag | FLAG` filtering is not exposed as a working public option: all
applicable forms are returned, and callers can filter the typed result.

Visible-tile accounting and the eight-point qualification convenience property
are wrapper functionality, not changes to upstream's shanten or fan algorithms.
The library does not validate a full game's chronology, discarder eligibility,
wall availability or all circumstances of a legal call.

## Dependencies and publishing

Java 17 is both the bytecode target and the Java API baseline. The sole direct
library dependency is Kotlin's standard library; JetBrains annotations is its
transitive dependency. JUnit and the optional C++ process are test-only.
The C++ oracle communicates over standard input/output; no native code is loaded
into the JVM or bundled in Maven artifacts.

The initial artifact is `top.skyeyefast:mcr-mahjong:0.1.0`. Public API
changes must be explicit and accompanied by updated consumer tests. There is no
promise that internal Kotlin/JVM-mangled members are stable API.

The standard library is published in Maven `compile` scope because Kotlin-generated
public members expose its types to Java callers. The `javadoc` artifact contains
Dokka HTML API reference, not just Markdown; the `sources` artifact contains the
actual Kotlin sources. Both include MIT attribution. No native oracle, reference
checkout, consumer code or documentation generator is a library runtime dependency.

The `0.1.0` coordinate remains a local, unpublished release candidate. Normal CI
runs build (including ABI and publication-file checks), Maven Local publication,
then the independent Java/POM and Kotlin/Gradle-metadata consumers. Native C++
differential testing remains explicit opt-in, not a normal CI prerequisite.

### Release hold

No formal `v0.1.0` tag or public Maven deployment is authorized while the rules
review is pending. The supported rules edition and the fan-57 scoring representation
must be accepted before release; Chinese-version translation disputes must not be
silently resolved by mixing editions. A remote repository can be created for review,
but actual project/SCM URLs and the target repository's signing requirements still
need to be supplied for public distribution. Passing upstream differential or ABI
checks alone cannot clear this semantic release hold.
