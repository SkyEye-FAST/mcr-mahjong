# mcr-mahjong

A standalone Kotlin/JVM library for Mahjong Competition Rules (国标麻将), with
Java 17 as its bytecode and Java API baseline.

The library calculates fan scores, structural shanten, effective tiles, waits and
discard options. Public scoring uses the WMO Chinese *麻将竞赛规则*, December 2014
first edition, identified by `McrMahjong.SCORING_PROFILE == "wmo-2014-zh"`.
See the [compatibility contract](COMPATIBILITY.md) for the profile and its differences
from the underlying algorithm.

This project ports Jeff Wang's MIT-licensed
[mahjong-algorithm](https://github.com/summerinsects/mahjong-algorithm), pinned to
`44a178af08bf11f82a8993fddbe2fe8876ddd8f3`. Attribution is in [NOTICE](NOTICE)
and [LICENSE](LICENSE).

## Features and scope

- All 81 public fans, with fan exclusions and selection among competing decompositions.
- Analysis of regular hands, Seven Pairs, Thirteen Orphans, honors-and-knitted
  tiles (全不靠 / 七星不靠), and Knitted Straight (组合龙).
- Effective-tile counts using the hand, fixed melds and additional known tiles;
  structural waits and one analysis per distinct discard.
- Immutable input snapshots and results, with stateless calculations.

The library evaluates hand shapes and scores. Applications manage turns, legal
calls, wall state, penalties and player-to-player settlement. Structural readiness,
a winning shape and the eight-point scoring threshold are separate results.
The runtime uses Kotlin's standard library (with JetBrains annotations transitively)
and runs entirely on the JVM.

## Installation

The current release version is `0.1.1`, with coordinates
`top.skyeyefast:mcr-mahjong:0.1.1`. Maven Central publication is in progress.
With Gradle Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
}
dependencies {
    implementation("top.skyeyefast:mcr-mahjong:0.1.1")
}
```

With Maven:

```xml
<dependency>
    <groupId>top.skyeyefast</groupId>
    <artifactId>mcr-mahjong</artifactId>
    <version>0.1.1</version>
</dependency>
```

Use JDK 17 or later. The artifact is compiled with Kotlin 2.3.20; Kotlin consumers
need a compiler compatible with Kotlin 2.3 metadata. Java consumers use the normal
Maven or Gradle dependency without a Kotlin build plugin.

## Kotlin

```kotlin
import top.skyeyefast.mcr.*

val hand = Hand(Tiles.parse("19m19s19pESWNCFP"))
val analysis = McrMahjong.analyze(hand)
check(analysis.shanten == 0)
check(analysis.remainingCount == 39)

val result = McrMahjong.score(hand, Tile.NORTH)
when (result) {
    is ScoreResult.Winning -> {
        check(result.totalFan == 88)
        check(result.meetsMinimum)
        println(result.fans)
    }
    ScoreResult.NotWinning -> println("Not a winning shape")
}

// Additional visible tiles exclude the hand, fixed melds and drawn tile.
val discards = McrMahjong.discards(hand, Tile.NORTH, visibleTiles = listOf(Tile.M1))
for (discard in discards) {
    println("${discard.discard}: ${discard.shanten}, ${discard.remainingCount} copies")
}
```

### Tiles, melds and winning context

`m` means characters (万), `s` bamboo (索), and `p` dots (筒).
Honor notation is `E S W N C F P`: east, south, west, north, red, green, white.
`Tiles.parse` accepts grouped ranks such as `123m456s789pEE`; `Tile.parse`
accepts exactly one tile. The parser uses these 34 tile kinds. Represent melds
with `Meld` objects and flowers with `WinContext.flowerCount` (`0..8`).

`Hand` represents the position **before drawing or winning**:
`concealedTiles.size + 3 * melds.size == 13`. A kong has four physical tiles but
counts as one structural meld. Use `Meld.Chow` with its middle tile,
`Meld.Pung` for an exposed triplet, or `Meld.Kong`. For a kong, `from = null`
means concealed; specify a `RelativePlayer` for an exposed kong and
`promoted = true` for an added kong. Concealed chows and pungs remain individual
tiles in `concealedTiles`.

```kotlin
val handWithMeld = Hand(
    Tiles.parse("258m1477s369p"),
    listOf(Meld.Chow(Tile.M4, ChowPosition.HIGH)),
)
val score = McrMahjong.score(
    handWithMeld,
    Tile.S7,
    WinContext(
        method = WinMethod.DISCARD,
        prevalentWind = Wind.EAST,
        seatWind = Wind.SOUTH,
        flowerCount = 2,
    ),
)
```

`WinContext` defaults to a discard win, east prevalent/seat winds, zero flowers
and false flags. `lastTile` means the last available copy (和绝张); `wallLast`
means the last wall tile. `kongInvolved` denotes replacement-tile self-draw or
robbing a kong according to `method`. The scorer applies the pinned upstream's
physical-input corrections to last-copy and kong flags; callers supply the game
context.

### Scores and analysis

`ScoreResult.Winning` contains `fans`, `totalFan`, `nonFlowerFan` and
`meetsMinimum`. The eight-point minimum uses `nonFlowerFan`; `totalFan` includes
flowers. A structurally winning seven-point hand is `Winning` with
`meetsMinimum == false`. `NotWinning` identifies a non-winning shape.

Sum `FanCount.points` when displaying a breakdown. For one exposed and one
concealed kong, the result contains a `TWO_MELDED_KONGS` entry with
`isMixedKongPair == true` and `points == 6`. This combines both kongs into one
award; the base `Fan.points` value of 4 describes two exposed kongs. Use
`Hand.melds` for the physical composition.

`analyze` returns each applicable form's shanten and effective tiles. Its overall
shanten is the minimum, and its effective tiles are the union at that minimum.
`shanten` calculates just the minimum. Zero means structurally ready.
`waitingTiles` returns structural winning tile kinds; use `score` to check the
resulting fan value and minimum qualification.

`EffectiveTile.remainingCopies` accounts for the complete hand, including fixed
melds, and any additional `visibleTiles`. Visible tiles affect remaining counts;
structural shanten stays the same. Zero-copy structural waits are retained.
For `discards`, exclude the drawn tile as well as the hand from `visibleTiles`.

`discards` returns one entry per distinct tile, drawn tile first and then canonical
tile order. Shanten describes the **hand after discarding**. `completesForms`
records forms completed by adding that discard back. The discarded tile remains
known when counting remaining copies.

Invalid tile counts, melds and fifth copies raise `IllegalArgumentException`.
Input and output collections are copied into immutable snapshots. Keep supplied
mutable collections unchanged while a call takes its snapshot.

## Java

```java
import top.skyeyefast.mcr.*;

var hand = new Hand(Tiles.parse("19m19s19pESWNCFP"));
int shanten = McrMahjong.shanten(hand);
var result = McrMahjong.score(hand, Tile.NORTH, new WinContext());
if (result instanceof ScoreResult.Winning win) {
    System.out.println(win.getTotalFan());
}
```

## Documentation

- [Compatibility](COMPATIBILITY.md): scoring profile, upstream baseline, input
  boundaries and version semantics.
- [Rule review](RULES_REVIEW.md): source edition, clause/page references, scoring
  decisions and their implementation/tests.
- [Development](DEVELOPMENT.md): build, verification, ABI maintenance and publishing.
- [Changelog](CHANGELOG.md): version changes.

The `javadoc` JAR contains the generated Dokka HTML API reference at `index.html`
and these guides under `guides/`. KDoc is also included in the sources JAR.
