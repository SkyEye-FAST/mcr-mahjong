# Scoring rule review

This record connects the `wmo-2014-zh` scoring decisions to the selected Chinese
rulebook, implementation and independent public-rule tests. The compatibility
contract and review status are in [COMPATIBILITY.md](COMPATIBILITY.md).

## Source edition

The source is the World Mahjong Organization's Chinese *麻将竞赛规则*, compiled
by 世界麻将组织 and published by 北京现代出版社, first edition and first printing,
December 2014, ISBN `978-7-5143-3183-7`. The WMO
[rules index](https://www.mindmahjong.com/info/showinfo.asp?id=131) lists the
Chinese and trilingual texts separately; its
[Chinese-edition notice](https://www.mindmahjong.com/info/showinfo.asp?id=925)
identifies the Chinese edition as 2014.

The edition record retained from the 2026-09-24 review identifies the
[official PDF](https://www.mindmahjong.com/adobe/ZC2021.pdf) as 2,933,388 bytes,
38 PDF pages containing printed pages 1–62, with SHA-256
`edec145f378fdd0f692cb1096802588397437257252aef4eb90e4c07a57d7a71`.
Its printed publication record reads `2014年12月第一版` and
`2014年12月第1次印刷`. The filename `ZC2021.pdf` and the 2013 postscript date
identify different aspects of the document; the profile uses its publication record.
All page references below are printed page numbers. One PDF spread may contain two
printed pages.

### Related sources

The English *Green Book* is identified as WMO 2006 by the
[European Mahjong Association](https://mahjong-europe.org/portal/index.php?Itemid=167&id=31&option=com_content&view=article).
The WMO also hosts a trilingual text and a separate
[Chinese scan, `z20140402.pdf`](https://www.mindmahjong.com/adobe/z20140402.pdf).
These are distinct source documents. Publication dates, translation revisions and
substantive rule changes must be established from the respective texts; a newer
filename or translation date alone does not establish a changed scoring clause.
This review uses the identified 2014 Chinese edition throughout.

The Chinese preface gives the Chinese text precedence for translation or
interpretation differences. Rule 1.2.2 (p. 2) assigns changes during implementation
to WMCC, and 1.2.3 assigns rule interpretation to WMCC. The 2026-09-24 review
recorded no standalone WMO corrigendum in the official index and notices it examined.

Tournament supplements apply within the issuing organizer's stated scope.
Community explanations can help identify questions and examples; adopting one
requires tracing its reasoning to the selected rules. Earlier project notes cited
2024 championship organizer clarifications for Knitted Straight waits. The
current decision below rests on the Chinese clauses and records the residual-wait
step as a project interpretation.

## Scoring decisions

The upstream column refers to the pinned implementation and switches in
[COMPATIBILITY.md](COMPATIBILITY.md#upstream-baseline). The public column records
the implemented decision. Chinese quotations preserve the basis of the original review.

| Chinese clause and printed page | Pinned upstream behavior | Public handling and rationale |
| --- | --- | --- |
| 附件一第57番「双明杠」, p. 40: “一明杠与一暗杠计6分”; fan table, p. 14 | Emits the private five-point mixed-kong extension. | Award six points. `FanCount.isMixedKongPair` marks the exception; construction and `copy` validate that it occurs only on one `TWO_MELDED_KONGS` entry. `FanCount.points` is 6, while `Fan.points` remains the ordinary two-exposed-kong base value 4. The breakdown reports no fictitious extra or individual kong fans, and `Winning.totalFan` sums the awarded `FanCount.points`. |
| 第八条分值表及附件八「双暗杠」, pp. 11, 14: eight-point group | `TWO_CONCEALED_KONGS` is 6. | Award 8; verify public details and the raw six-point oracle separately. |
| 附件一第17番「三杠」, p. 28: “暗杠加计，三暗杠加计三暗刻分”; 第33番「三暗刻」, p. 33: “三副暗刻（暗杠）”; fan values, pp. 12–14 | Three-kong candidates use the raw concealed-pung series; this omits the separate concealed-kong award for one/two concealed kongs. | For one concealed kong, add Concealed Kong; for two, add Two Concealed Kongs and remove the overlapping Two Concealed Pungs. Apply these adjustments before candidate selection. A concealed kong also qualifies in the concealed-pung series: with one hidden kong plus a concealed pung, retain Two Concealed Pungs; with two hidden kongs plus a concealed pung, retain Three Concealed Pungs; three hidden kongs add Three Concealed Pungs, or Four Concealed Pungs when the fourth set is also concealed. Do not add the mixed-two-kong exception to a subset. |
| 附件一第5番「四杠」, p. 24: “暗杠加计。不计单调将分”; concealed-kong fan values, pp. 12–14; concealed-pung clauses, pp. 27, 33 | Four kongs with one concealed kong omit its concealed-kong fan; two concealed kongs are counted as Two Concealed Pungs (2), not Two Concealed Kongs (8). Three/four hidden kongs use the concealed-pung series. | Add Concealed Kong for one concealed kong; add Two Concealed Kongs and remove the overlapping Two Concealed Pungs for two; retain Three/Four Concealed Pungs for three/four. The raw C++ expectations remain unchanged. |
| 附件一第4、6、7、12、19、20、34番, pp. 23–35: each says self-draw “加计不求人分” | The special-hand path normally contributes the one-point Self Drawn fan. | Award Fully Concealed Hand (4) in place of Self Drawn for Nine Gates, Seven Shifted Pairs, Thirteen Orphans, Four Concealed Pungs, Seven Pairs, and Greater/Lesser Honors and Knitted Tiles. Seven Shifted Pairs has a separate regression case. |
| 附件一第3番「绿一色」, p. 23: “可加计清一色、混一色分” | The raw exclusion pass removes Half Flush. | Restore Half Flush when green dragons are present; retain Full Flush when there are no honors. Each case has a separate test. |
| 附件一第8番「清幺九」, p. 26: excludes 碰碰和、全带幺、幺九刻、无字; examples permit “两个双同刻” or “三同刻” | Removes Double Pung whenever All Terminals is present. | Restore Double Pungs from the candidate's actual pungs; a Seven Pairs candidate supplies no pungs. Triple Pung absorbs its constituent pairs under the non-repetition/non-splitting principles (printed p. 19). |
| 附件一第4番「九莲宝灯」, p. 24: excludes 清一色、门前清、幺九刻; 第6番「连七对」, pp. 24–25: excludes 清一色、门前清、单调将; 第19番「七对」, p. 31: excludes 门前清、单调将 | These special paths do not emit those excluded fans. | Retain these exclusions; independent assertions cover the high-impact special-hand and self-draw cases. |
| 附件一第20番「七星不靠」, p. 31, and 第34番「全不靠」, p. 34: self-draw adds 不求人; both exclude 五门齐、门前清 | Raw special-form scoring does not add Fully Concealed Hand. | Add Fully Concealed Hand on self-draw and retain the named exclusions. |
| 附件一第35番「组合龙」, p. 35, definition and example 1; 3.9.1(6) 不拆移原则, p. 19 (“不拆开互相组成其他番种”); 附件一第77–79番等待定义, pp. 45–46 (“只能听和123的3或789的7”, “只能听和顺子中间的牌”, “调单张牌作将和牌”) | With `KNITTED_STRAIGHT_BODY_WITH_ECS=1`, the raw path removes the knitted body and evaluates the wait in the residual structure. It can count edge/closed/single wait when the winning tile kind also occurs in the body, provided another physical copy completes the residual wait. | Retain the upstream residual-wait behavior under the project interpretation derived below. Tests cover overlapping edge, closed and single waits, body-only completion and residuals with two winning tile kinds. |

### Other exclusion relationships

These printed clauses were checked against `FanCalculator.finalAdjust`; no
additional public normalization was found necessary for these exclusions:

| Chinese clauses and pages | Checked relationship |
| --- | --- |
| 大四喜 p. 22; 大三元 p. 22; 小四喜 and 小三元 pp. 26–27 | The named Wind/Dragon fans suppress Three Winds/Two Dragon Pungs and constituent Pung fans where each clause says “不计”. |
| 字一色 and 四暗刻 pp. 26–27; 一色双龙会 pp. 26–27; 一色四节高 p. 28; 混幺九 p. 29 | Higher patterns suppress the explicitly listed All Pungs, Outside Hand, Pung of Terminals/Honors, Concealed Hand, flush and lower combination fans. |
| 全双刻 p. 30; 全大、全中、全小 pp. 32–33; 三色双龙会 p. 32; 九莲宝灯 p. 24 | The explicit “不计” clauses for All Pungs, All Simples, No Honors, Full Flush and terminal-pung fans match the scorer's exclusions. |
| 妙手回春 and 杠上开花 p. 36; 抢杠和 p. 36; 四杠 p. 24; 全求人 p. 40 | Special win-method exclusions for Self Drawn, Last Tile and Single Wait match the Chinese clauses. |

## Knitted Straight residual waits

The project applies these clauses together:

1. The Knitted Straight definition and example 1 (p. 35) allocate nine tiles to
   the knitted body; the example explicitly includes Single Wait.
2. The non-splitting principle, 3.9.1(6) (p. 19), keeps that allocation fixed when
   identifying additional fans.
3. The wait definitions (pp. 45–46) supply the edge, closed and single-wait shapes.
   The project applies the unique-wait wording to the residual structure after
   removing the body.

The third step extends the example into an algorithm for residual waits. In
particular, awarding Edge Wait or Closed Wait when the winning tile kind also
occurs in the knitted body is an inference from these clauses, rather than an
explicit ruling on those exact hands. The implementation uses separate physical
copies: one allocated to the body and another completing the residual wait.
A win completing only the body receives no wait fan; a residual with two winning
tile kinds also receives no wait fan.

This interpretation is adopted for `wmo-2014-zh`. Its regression cases establish
that the software follows the decision, while the cited clauses and this derivation
allow the decision itself to be reviewed.

## Implementation and test map

Source links below point to the repository. Symbol names identify the relevant
branches without depending on line numbers.

- [StandardMcr.kt](https://github.com/SkyEye-FAST/mcr-mahjong/blob/main/src/main/kotlin/top/skyeyefast/mcr/internal/StandardMcr.kt):
  `calculate` adjusts each candidate's fan table and supplies its public score to
  `FanCalculator.calculate` before maximum-score selection.
- [FanCalculator.kt](https://github.com/SkyEye-FAST/mcr-mahjong/blob/main/src/main/kotlin/top/skyeyefast/mcr/internal/FanCalculator.kt):
  `knittedFan` handles the knitted body and residual waits;
  `finalAdjust` applies the shared exclusion relationships.
- [Fan.kt](https://github.com/SkyEye-FAST/mcr-mahjong/blob/main/src/main/kotlin/top/skyeyefast/mcr/Fan.kt):
  `Fan` supplies public values, `FanCount.points` handles the mixed-kong subtotal,
  and `ScoreResult.Winning` separates flower points from minimum qualification.

The following methods are in
[StandardMcrTest.kt](https://github.com/SkyEye-FAST/mcr-mahjong/blob/main/src/test/kotlin/top/skyeyefast/mcr/StandardMcrTest.kt).
Their expectations are independent of the C++ fixture generator.

| Decision | Test method |
| --- | --- |
| Public 81-fan vocabulary and raw 82-entry values | `publicVocabularyIsStandardAndOracleVocabularyIsNotChanged` |
| Mixed-kong six-point exception and marker validation | `mixedKongsUseTheSixPointClauseAndCanReachMinimum` |
| Two Concealed Kongs and flower accounting | `twoConcealedKongsAreEightAndFlowersStaySeparate` |
| Special forms and Fully Concealed Hand | `mandatoryConcealedFormsCanCombineFullyConcealedOnSelfDraw` |
| Corrected candidate selection: regular raw 49 versus public Seven Pairs 52 | `normalizationPrecedesBestDecompositionSelection` |
| Overlapping, body-only and ambiguous residual waits | `knittedDragonWaitFansUseOnlyTheUniqueResidualWait` |
| All Terminals, Double Pung and Triple Pung | `allTerminalsMayAddDoublePungsButDoesNotDoubleCountATriple` |
| All Green with Half Flush or Full Flush | `greenHandsRetainTheirExplicitHalfFlushCombination` |
| Three Kongs and concealed-kong awards | `threeKongsApplyTheChineseConcealedKongClause` |
| Three Kongs with additional concealed pungs | `threeKongsCombineConcealedKongsWithTheConcealedPungSeries` |
| Four Kongs, concealed sets and Single Wait exclusion | `fourKongsAddConcealedKongFansAndKeepTheFourConcealedPungCombination` |

The additional exclusion rows above record clause-to-code checks in `finalAdjust`;
the method table identifies the dedicated public-rule regression coverage.
