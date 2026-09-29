# mcr-mahjong

A standalone **Kotlin/JVM 17** library for Mahjong Competition Rules (国标麻将).

Public scoring follows the World Mahjong Organization's 2014 Chinese *麻将竞赛规则*, exposed as the `wmo-2014-zh` scoring profile.

The implementation is a behavior-first Kotlin port of Jeff Wang's MIT-licensed [mahjong-algorithm](https://github.com/summerinsects/mahjong-algorithm), based on upstream commit `44a178af08bf11f82a8993fddbe2fe8876ddd8f3`. Public scoring applies documented MCR corrections on top of the upstream-compatible calculation.

See [COMPATIBILITY.md](COMPATIBILITY.md) for the exact rule source, compatibility contract and audited differences from upstream.

## Features

* Shanten and effective-tile analysis for:

  * regular hands
  * seven pairs
  * thirteen orphans
  * honors and knitted tiles (全不靠 / 七星不靠)
  * knitted straight (组合龙)
* Structural waiting-tile analysis
* Discard analysis
* Complete public scoring for the 81 standard MCR fans
* Competing decomposition evaluation and fan exclusions
* Kotlin and Java APIs
* Immutable results with no shared mutable evaluation state
* No Minecraft, mod-loader, native-library, JNI or Python dependencies

This is an algorithm library, not a game or turn engine. It does not decide whether a call or win declaration is legal in a particular game state.

## Dependency

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("top.skyeyefast:mcr-mahjong:0.1.0")
}
```

The library targets JVM 17 and is built with Kotlin 2.3.20. Kotlin's standard library is exposed as a normal transitive dependency.

## Kotlin

```kotlin
import top.skyeyefast.mcr.*

val hand = Hand(Tiles.parse("19m19s19pESWNCFP"))

val analysis = McrMahjong.analyze(hand)
println(analysis.shanten)
println(analysis.effectiveTiles)

val result = McrMahjong.score(hand, Tile.NORTH)

when (result) {
    is ScoreResult.Winning -> {
        println("Fan: ${result.totalFan}")
        println("Meets minimum: ${result.meetsMinimum}")
        println(result.fans)
    }

    ScoreResult.NotWinning ->
        println("Not a winning shape")
}
```

Discard analysis is also available:

```kotlin
val discards = McrMahjong.discards(
    hand,
    Tile.NORTH,
    visibleTiles = listOf(Tile.M1),
)

for (discard in discards) {
    println("${discard.discard}: ${discard.shanten}, ${discard.remainingCount} copies")
}
```

## Tiles and hands

Tile notation uses:

* `m` — characters (万)
* `s` — bamboo (索)
* `p` — dots (筒)
* `E S W N` — east, south, west, north
* `C F P` — red, green, white dragons

For example:

```kotlin
Tiles.parse("123m456s789pESWN")
```

`Tiles.parse` accepts grouped ranks such as `123m456s789pEE`. Red fives, bracketed meld notation and `z` honor notation are not supported.

A `Hand` represents the position **before drawing or winning**:

```text
concealedTiles.size + 3 * melds.size == 13
```

Fixed melds are represented with `Meld.Chow`, `Meld.Pung` and `Meld.Kong`. A kong contains four physical tiles but counts as one structural meld.

Flowers are supplied through `WinContext.flowerCount`; they are not a separate tile kind.

## Scoring

Winning context can specify information such as:

```kotlin
val context = WinContext(
    method = WinMethod.DISCARD,
    prevalentWind = Wind.EAST,
    seatWind = Wind.SOUTH,
    flowerCount = 2,
)

val result = McrMahjong.score(hand, winningTile, context)
```

`ScoreResult.Winning` means the hand has a structurally valid winning shape. It contains:

* `fans`
* `totalFan`
* `nonFlowerFan`
* `meetsMinimum`

A winning hand below the eight-point minimum is still returned as `ScoreResult.Winning`, with `meetsMinimum == false`.

`ScoreResult.NotWinning` is reserved for hands that do not form a winning shape.

Flowers do not contribute toward the eight-point minimum.

When consuming the fan breakdown, use `FanCount.points` rather than recalculating points from `Fan.points * count`. Some rulebook-defined combinations, such as one exposed and one concealed kong, have a combined value that differs from the base fan value.

## Analysis semantics

`McrMahjong.analyze` returns the applicable hand forms, shanten and effective tiles.

`McrMahjong.shanten` is a shortcut when effective-tile information is not needed.

`McrMahjong.waitingTiles` returns structural winning tiles. It does not imply that the resulting hand reaches the eight-point minimum.

Effective-tile remaining counts include the tiles already present in the hand and can additionally account for `visibleTiles`.

`McrMahjong.discards` reports the state **after each discard**. A shanten value of zero therefore means the resulting hand is ready.

Invalid tile counts, malformed melds and situations implying more than four copies of a tile are rejected with `IllegalArgumentException`.

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

## Rules and compatibility

`McrMahjong.SCORING_PROFILE` identifies the public scoring contract as:

```text
wmo-2014-zh
```

The public scorer and the raw upstream compatibility baseline are deliberately separate. The public API follows the audited WMO rule profile, while the internal upstream-compatible implementation remains available for regression and differential verification.

See:

* [COMPATIBILITY.md](COMPATIBILITY.md) — rule source, scoring differences and compatibility contract
* [NOTICE](NOTICE) — upstream attribution and pinned source revision
* [CHANGELOG.md](CHANGELOG.md) — release history

## Building

Use JDK 17 or later:

```shell
./gradlew build
```

To publish the current artifact to Maven Local:

```shell
./gradlew publishToMavenLocal
```

On Windows, use `gradlew.bat`.

Optional native differential testing against the pinned C++ implementation is documented in [tools/README.md](tools/README.md).

## License

This project is licensed under the [MIT License](LICENSE).

It contains a Kotlin port derived from Jeff Wang's MIT-licensed `mahjong-algorithm`. See [NOTICE](NOTICE) for attribution and source revision details.
