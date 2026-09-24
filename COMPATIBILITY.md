# Compatibility contract

The public API is the documented `top.skyeyefast.mcr` package. The
`top.skyeyefast.mcr.internal` package, packed integers, enum ordinals and test
protocol are implementation details and are not serialization contracts.
Consumers should store tile/fan names rather than internal numbers.

## Two separate contracts

**Public scoring profile: `wmo-2014-zh`.** `McrMahjong.score` uses the WMO Chinese
*麻将竞赛规则* edition identified below. This is not a claim of interchangeability
with the original 1998 national rules or later online-house-rule profiles. `Fan` contains
81 identifiers; their numeric enum positions are not official rulebook numbers.

**Upstream compatibility baseline:** the exact commit and default switches in
NOTICE. Internal `UpstreamFan` retains all 82 entries, including the five-point
mixed-kong extension. The C++ protocol, frozen C++ fixtures and differential suite
exercise this unnormalized baseline. Passing them proves the port's compatibility
with that source revision, not conformity of public scores to a rules standard.

`StandardMcr` is a narrow candidate-scoring adapter. Decomposition, pattern
detection and traversal remain in `FanCalculator`; sourced score corrections run
on every candidate before choosing the maximum. Normalizing only the upstream
winner is wrong: `44556m445566s55p6m` on self-draw has a 49-point raw regular
candidate and a 52-point seven-pairs candidate under this profile. No selectable
house-rule switches or legacy public profile are exposed.

### Rule sources

The source is the World Mahjong Organization's Chinese-only *麻将竞赛规则*,
compiled by 世界麻将组织 and published by 北京现代出版社: first edition and first
printing, December 2014, ISBN `978-7-5143-3183-7`. The WMO [rules index](https://www.mindmahjong.com/info/showinfo.asp?id=131)
lists the Chinese and trilingual texts separately, and its [Chinese-edition notice](https://www.mindmahjong.com/info/showinfo.asp?id=925)
identifies the Chinese edition as 2014. The [exact official PDF](https://www.mindmahjong.com/adobe/ZC2021.pdf)
is 2,933,388 bytes, 38 PDF pages containing printed pages 1–62, SHA-256
`edec145f378fdd0f692cb1096802588397437257252aef4eb90e4c07a57d7a71`.

The edition is identified by the WMO Chinese-edition listing and the book's
printed CIP/publication record (`2014年12月第一版`, `2014年12月第1次印刷`),
not by the downloadable filename `ZC2021.pdf` or by the 2013 date in its postscript.
This audit does not combine it with the separately hosted trilingual text or the
different `z20140402.pdf` scan. The Chinese preface says Chinese governs translation
or interpretation differences. Rule 1.2.2 (printed p. 2) says changes during
implementation are separately prescribed by WMCC. As of 2026-09-24, no standalone
WMO corrigendum was linked from the official rules index or found in official rule
notices during this review; tournament clarifications are not treated as WMO
corrigenda.

References below are printed page numbers in this selected edition. A spread
in the PDF can contain two printed pages, so PDF page indexes are not substituted
for printed page references.

Tournament-level supplements, including the 2024 championship organizer's
clarifications, are scoped to their named event and do not amend this WMO edition.

### Audited scoring differences

Each row records the Chinese rule clause/page, the fixed raw upstream result, the
public result before this review, and the final handling. Chinese quotations are
the basis for rule decisions.

| Chinese clause and printed page | Pinned upstream behavior | Public behavior before this review | Final handling |
| --- | --- | --- | --- |
| 附件一第57番「双明杠」, p. 40: “一明杠与一暗杠计6分”; fan table, p. 14 | Emits the private five-point mixed-kong extension. | One `TWO_MELDED_KONGS` count worth 6; no individual-kong fans. | Keep the six-point award. `FanCount.isMixedKongPair` marks the exception; construction and `copy` validate that it occurs only on one `TWO_MELDED_KONGS` entry. `FanCount.points` is 6, while `Fan.points` remains the ordinary two-exposed-kong base value 4. The breakdown reports no fictitious extra or individual kong fans, and `Winning.totalFan` sums the awarded `FanCount.points`. |
| 第八条分值表及附件八「双暗杠」, pp. 11, 14: eight-point group | `TWO_CONCEALED_KONGS` is 6. | Public value is 8. | Keep 8; verify public details and the raw six-point oracle separately. |
| 附件一第17番「三杠」, p. 28: “暗杠加计，三暗杠加计三暗刻分”; 第33番「三暗刻」, p. 33: “三副暗刻（暗杠）”; fan values, pp. 12–14 | Three-kong candidates use the raw concealed-pung series; this omits the separate concealed-kong award for one/two concealed kongs. | One concealed kong added `CONCEALED_KONG`; two added `TWO_CONCEALED_KONGS` and removed the overlapping `TWO_CONCEALED_PUNGS`; three retained the Three/Four Concealed Pungs result. | Apply those adjustments before candidate selection. A concealed kong also qualifies in the concealed-pung series: with one hidden kong plus a concealed pung, retain Two Concealed Pungs; with two hidden kongs plus a concealed pung, retain Three Concealed Pungs; three hidden kongs add Three Concealed Pungs, or Four Concealed Pungs when the fourth set is also concealed. Do not add the mixed-two-kong exception to a subset. |
| 附件一第5番「四杠」, p. 24: “暗杠加计。不计单调将分”; concealed-kong fan values, pp. 12–14; concealed-pung clauses, pp. 27, 33 | Four kongs with one concealed kong omit its concealed-kong fan; two concealed kongs are counted as Two Concealed Pungs (2), not Two Concealed Kongs (8). Three/four hidden kongs use the concealed-pung series. | No correction was applied to Four Kongs. | Add Concealed Kong for one concealed kong; add Two Concealed Kongs and remove the overlapping Two Concealed Pungs for two; retain Three/Four Concealed Pungs for three/four. The raw C++ expectations remain unchanged. |
| 附件一第4、6、7、12、19、20、34番, pp. 23–35: each says self-draw “加计不求人分” | The special-hand path normally contributes the one-point Self Drawn fan. | Public scoring normalized Nine Gates, Seven Shifted Pairs, Seven Pairs, Thirteen Orphans, Four Concealed Pungs, and Greater/Lesser Honors and Knitted Tiles to Fully Concealed Hand. | Keep the source-backed four-point addition and do not also award Self Drawn. Add a distinct Seven Shifted Pairs regression. |
| 附件一第3番「绿一色」, p. 23: “可加计清一色、混一色分” | The raw exclusion pass removes Half Flush. | Restore Half Flush when green dragons are present; Full Flush remains when there are no honors. | Keep both source-permitted cases and test them separately. |
| 附件一第8番「清幺九」, p. 26: excludes 碰碰和、全带幺、幺九刻、无字; examples permit “两个双同刻” or “三同刻” | Removes Double Pung whenever All Terminals is present. | Restore separate Double Pungs unless their pungs are already used by Triple Pung; do not invent pungs for Seven Pairs. | Keep the correction. Triple Pung absorbs its constituent pairs under the non-repetition/non-splitting principles (printed p. 19). |
| 附件一第4番「九莲宝灯」, p. 24: excludes 清一色、门前清、幺九刻; 第6番「连七对」, pp. 24–25: excludes 清一色、门前清、单调将; 第19番「七对」, p. 31: excludes 门前清、单调将 | These special paths do not emit those excluded fans. | Public scoring retained those exclusions. | Keep them; independent assertions cover the high-impact special-hand and self-draw cases. |
| 附件一第20番「七星不靠」, p. 31, and第34番「全不靠」, p. 34: self-draw adds 不求人; both exclude 五门齐、门前清 | Raw special-form scoring does not add Fully Concealed Hand. | Public normalization adds it on self-draw and leaves the named exclusions absent. | Keep the source-backed handling. |
| 附件一第35番「组合龙」, p. 35: its example permits 五门齐、门前清、箭刻、单调将; wait-fan definitions, p. 45 | The raw knitted path can award an edge/closed/single wait when the winning tile overlaps the special body and an ordinary wait interpretation. | Public scoring retained that upstream wait behavior and previously cited a tournament clarification. | This source confirms a residual Single Wait combination, but does not settle every body-overlap interpretation. The tournament clarification is scoped to its event and is not evidence for this profile. The overlapping wait remains a release blocker; it must not be certified from English wording or raw oracle output. |

#### Other exclusion relationships checked

These printed clauses were checked against `FanCalculator.finalAdjust`; no
additional public normalization was found necessary for these exclusions:

| Chinese clauses and pages | Checked relationship |
| --- | --- |
| 大四喜 p. 22; 大三元 p. 22; 小四喜 and小三元 pp. 26–27 | The named Wind/Dragon fans suppress Three Winds/Two Dragon Pungs and constituent Pung fans where each clause says “不计”. |
| 字一色 and四暗刻 pp. 26–27; 一色双龙会 pp. 26–27; 一色四节高 p. 28; 混幺九 p. 29 | Higher patterns suppress the explicitly listed All Pungs, Outside Hand, Pung of Terminals/Honors, Concealed Hand, flush and lower combination fans. |
| 全双刻 p. 30; 全大、全中、全小 pp. 32–33; 三色双龙会 p. 32; 九莲宝灯 p. 24 | The explicit “不计” clauses for All Pungs, All Simples, No Honors, Full Flush and terminal-pung fans match the scorer's exclusions. |
| 妙手回春 and杠上开花 p. 36; 抢杠和 p. 36; 四杠 p. 24; 全求人 p. 40 | Special win-method exclusions for Self Drawn, Last Tile and Single Wait match the Chinese clauses. |

The pre-release `WinContext.initial` option and mixed-kong enum entry were removed.
The initial-hand/nine-gates branch, including its `r = 2` extra-nine quirk, remains
only in the raw oracle-compatible engine. The public contract takes an explicit
pre-win hand and winning tile and does not offer a blessing/initial-hand house-rule
option.

### Upstream feature switches, classified

| Fixed switch | Classification | Public treatment |
| --- | --- | --- |
| `SUPPORT_CONCEALED_KONG_AND_MELDED_KONG=1` | Historical upstream extension/encoding | Preserve five-point internal entry for C++ parity; apply the sourced six-point public combination above. |
| `KNITTED_STRAIGHT_BODY_WITH_ECS=1` | Upstream interpretation of waits when the winning tile overlaps the knitted body | The 2014 Chinese text confirms the residual Single Wait combination in its example, but does not settle every overlapping body/edge/closed/single interpretation. Do not use the event-scoped 2024 clarification as authority; this behavior remains a release blocker. |
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
review is pending. The remaining blocker is the Chinese text's treatment of waits
when the winning tile overlaps a Combination Dragon body; the confirmed residual
Single Wait example does not settle all edge/closed/single interpretations. Do not
resolve this from English wording, pinned upstream output or a tournament supplement.
Passing upstream differential or ABI checks alone cannot clear this semantic hold.
