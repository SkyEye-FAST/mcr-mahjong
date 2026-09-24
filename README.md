# mcr-mahjong

A standalone **Kotlin/JVM 17** library for Mahjong Competition Rules (国标麻将).
The only direct library dependency is Kotlin's standard library, which brings
JetBrains annotations transitively. There are no Minecraft, mod-loader,
native-library, JNI or Python dependencies.

This is a behavior-first port of Jeff Wang's MIT-licensed
[mahjong-algorithm](https://github.com/summerinsects/mahjong-algorithm), pinned to
`44a178af08bf11f82a8993fddbe2fe8876ddd8f3`. See [NOTICE](NOTICE),
[LICENSE](LICENSE) and the [compatibility contract](COMPATIBILITY.md).

## Scope

The library provides regular-hand, seven-pairs, thirteen-orphans,
honors-and-knitted (全不靠 / 七星不靠) and knitted-straight (组合龙) shanten and
effective tiles; structural waits and discard analysis; and the complete upstream
fan calculator, including competing decompositions, fan exclusions and tie-breaking.
All 81 standard fans are represented, plus the upstream's enabled five-point
concealed-and-melded-kong compatibility entry.

This is an algorithm library, not a game/turn engine. Structural readiness and
winning shape are separate from the eight-point minimum and from whether a
particular call is legal in the current game state.

## Build and local Maven use

Use JDK 17 or later:

```shell
./gradlew build
./gradlew publishToMavenLocal
```

On Windows use `gradlew.bat`. The artifact coordinates are:

```kotlin
repositories {
    mavenLocal() // until the artifact is published to a shared repository
    mavenCentral()
}
dependencies {
    implementation("top.skyeyefast:mcr-mahjong:0.1.0-SNAPSHOT")
}
```

The single-module build produces the library, sources and documentation JARs,
a Maven POM and Gradle module metadata. The documentation JAR contains the usage
and compatibility guides; KDoc is in the sources JAR. No remote Maven repository
or credentials are configured, and this version is not advertised as published
to Maven Central. Git commit signing is independent of Maven artifact signing.

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

// Extra visible tiles exclude this hand, its fixed melds and the drawn tile.
val discards = McrMahjong.discards(hand, Tile.NORTH, visibleTiles = listOf(Tile.M1))
for (discard in discards) {
    println("${discard.discard}: ${discard.shanten}, ${discard.remainingCount} copies")
}
```

### Tiles, melds and winning context

`m` means characters (万), `s` bamboo (索), and `p` dots (筒).
Honor notation is `E S W N C F P`: east, south, west, north, red, green, white.
`Tiles.parse` accepts grouped ranks such as `123m456s789pEE`; it does not accept
bracketed melds, red fives or `z` honor notation. `Tile.parse` accepts exactly one tile.
Flowers are supplied as a count in the winning context, not as a 35th tile kind.

`Hand` always represents the position **before drawing/winning**:
`concealedTiles.size + 3 * melds.size == 13`. A kong has four physical tiles but
counts as one structural meld. Use `Meld.Chow` with its middle tile,
`Meld.Pung`, or `Meld.Kong`. For a kong, `from = null` means concealed; specify a
`RelativePlayer` for an exposed kong, and `promoted = true` for an added kong.
Non-fixed concealed chows/pungs remain individual tiles in `concealedTiles`.

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

`lastTile` means the last available copy (和绝张); `wallLast` means the last wall
tile. `kongInvolved` denotes replacement-tile self-draw or robbing a kong according
to `method`. `initial` affects upstream initial-hand interpretation; blessing
fans themselves are disabled. The calculator preserves upstream's automatic
correction of contradictory last-copy/kong flags.

### Results and validation

`analyze` returns all applicable forms, their shanten and effective tiles, and the
minimum shanten with the union of effective tiles at that minimum. `shanten` is a
shortcut that does not calculate effective tiles. `waitingTiles` returns structural
winning tile kinds, not a guarantee of eight-point qualification.

`EffectiveTile.remainingCopies` accounts for your complete hand and optionally
additional `visibleTiles`. Zero-copy structural waits are retained intentionally.
Visible tiles change counts, not structural shanten. Passing duplicate knowledge
that implies more than four copies throws `IllegalArgumentException`.

`discards` returns one entry per distinct tile, drawn tile first and then canonical
tile order. Shanten describes the **hand after discarding**, with zero meaning
ready. `completesForms` separately records forms already completed by adding the
discard back, preserving the information encoded by upstream's `-1` result.
The discarded tile remains known when counting remaining copies.

`ScoreResult.Winning` contains fan counts, `totalFan`, `nonFlowerFan` and
`meetsMinimum`. Flowers never help satisfy the eight-point minimum. A structurally
winning seven-point hand is still `Winning` with `meetsMinimum == false`;
`NotWinning` is reserved for a non-winning shape. Invalid counts, melds and fifth
copies are rejected before entering the algorithms. Input/output collections are
defensively copied and unmodifiable, and calculations have no shared mutable state.

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

## Verification

`build` runs the small native-free regression suite, including frozen C++ fan
tables covering every enabled fan, special-form shanten/useful/wait sets, complete
discard-output digests, validation, immutable boundaries and Java interoperability.
The normal CI only runs this suite on JDK 17.

Native differential testing is explicitly opt-in, uses a fixed random seed and
compares complete outputs rather than just totals. See [tools/README.md](tools/README.md).
Neither the upstream C++ checkout nor compiled oracle is shipped in the library.
