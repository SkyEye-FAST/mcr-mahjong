# mcr-mahjong

A standalone **Kotlin/JVM 17** library for Mahjong Competition Rules (国标麻将).

**Unreleased candidate:** public scoring targets the WMO Chinese *麻将竞赛规则*,
first edition/first printing, December 2014, described with its official source and
SHA-256 in [COMPATIBILITY.md](COMPATIBILITY.md). This is not the upstream's
house-rule default or every rule set called “国标”. Its 81-fan public API and the
82-entry C++ compatibility baseline are separate. This remains a local, unreleased
`0.1.0` candidate. Audited rule interpretations and their Chinese page references
are recorded in [COMPATIBILITY.md](COMPATIBILITY.md), including Combination Dragon
residual waits. The rules review for this profile is complete; the release still
awaits remote CI and a signed version tag.
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
All 81 public fans are represented. The upstream-only five-point mixed-kong entry
is internal: the public scorer applies the rulebook's six-point combination and
eight-point Two Concealed Kongs. Corrections are applied before choosing the best
decomposition; they are not a rescaling of an already-selected upstream result.
For Three/Four Kongs, concealed-kong and concealed-pung combinations follow their
separate Chinese clauses; the exact adjustments and source pages are listed in
[COMPATIBILITY.md](COMPATIBILITY.md).

`McrMahjong.SCORING_PROFILE` identifies this contract as `wmo-2014-zh`, based on
the Chinese edition's printed publication record, not its PDF filename or 2013
postscript.
For the mixed-kong exception, the result has one entry under the rulebook's
`TWO_MELDED_KONGS` category with `FanCount.isMixedKongPair == true` and
`FanCount.points == 6`. It does not also award the individual kongs. Display that
flagged case as one exposed and one concealed kong, and sum `FanCount.points`,
not the base `Fan.points` values. The marker is validated, not an arbitrary score override.

This is an algorithm library, not a game/turn engine. Structural readiness and
winning shape are separate from the eight-point minimum and from whether a
particular call is legal in the current game state.

## Build and local Maven use

Use JDK 17 or later:

```shell
./gradlew build
./gradlew publishToMavenLocal
```

The artifact is compiled with Kotlin 2.3.20. Kotlin consumers need a compiler
compatible with Kotlin 2.3 metadata (the consumer checks use 2.3.20). Java
consumers need no Kotlin build plugin.

On Windows use `gradlew.bat`. The artifact coordinates are:

```kotlin
repositories {
    mavenLocal() // unpublished candidate; local verification only
    mavenCentral()
}
dependencies {
    implementation("top.skyeyefast:mcr-mahjong:0.1.0")
}
```

The single-module build produces the library, sources and documentation JARs,
a Maven POM and Gradle module metadata. The `javadoc` JAR contains the generated
Dokka HTML API reference (`index.html`) and usage/compatibility guides under
`guides/`; KDoc is also available in the sources JAR. Dokka is build-only.
Kotlin standard library is an API dependency so Java consumers can also use
Kotlin-generated public members such as enum entries without adding dependencies.

The public project repository is [SkyEye-FAST/mcr-mahjong](https://github.com/SkyEye-FAST/mcr-mahjong).
The POM includes project and SCM URLs, coordinates, description, MIT license,
developer identity, Java-compatible dependency scopes, the pinned upstream commit,
and the completed `wmo-2014-zh` rule-review status. It does not encode a temporary
publication state.

Maven Central publishing is configured through the [Central Portal publisher
API](https://central.sonatype.org/publish/publish-portal-api/) with the community
[GradleUp NMCP plugin](https://gradleup.com/nmcp/) 1.6.2 and Gradle's OpenPGP
signing plugin. Sonatype currently documents no official Gradle plugin for the
Portal. The Portal is configured for `USER_MANAGED` publishing, so a submitted
bundle requires a separate review and release in the Portal. Supply credentials
outside the repository through Gradle user properties or these environment
variables: `CENTRAL_PORTAL_USERNAME`, `CENTRAL_PORTAL_PASSWORD`,
`MAVEN_CENTRAL_SIGNING_KEY` (ASCII-armored private key), and
`MAVEN_CENTRAL_SIGNING_PASSWORD`. Use a primary key accepted by Central and,
before upload, publish its public part to a [key server supported by
Sonatype](https://central.sonatype.org/publish/requirements/gpg/).
No publishing credentials or key material are stored in this repository. This
`0.1.0` candidate has not been uploaded to Maven Central.

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
to `method`. There is no public initial-hand/blessing switch. The calculator preserves
upstream's automatic correction of contradictory last-copy/kong flags. For one
concealed and one exposed kong, the result identifies fan 57's six-point exception
with `FanCount.isMixedKongPair`. Read its `points` subtotal (6), not `fan.points * count`;
the flag distinguishes it from two physically exposed kongs. The individual kong
fans are not scored again. Use `Hand.melds` for the physical composition.

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
Normal CI runs on JDK 17 and includes the generated public ABI check, publication
content verification, Maven Local publication and both independent consumers.
Public standard-rule tests are separate from the frozen upstream regression tests.

Native differential testing is explicitly opt-in, uses a fixed random seed and
compares complete outputs rather than just totals. See [tools/README.md](tools/README.md).
Neither the upstream C++ checkout nor compiled oracle is shipped in the library.

To verify the unpublished candidate and both independent consumers:

```shell
./gradlew publishToMavenLocal
./gradlew -p consumers clean verify
```

The [consumer build](consumers/README.md) has a plain Java project using only the
Maven POM and a Kotlin project using Gradle module metadata. Neither depends on
the library's project/source sets. The artifact must exist in Maven Local;
consumer resolution never substitutes a remote copy of `mcr-mahjong`.

### Public ABI

The Kotlin Gradle plugin generates the reference dump under `api/`; `checkKotlinAbi`
is part of `build` and fails on unreviewed public signature changes. The entire
`internal` package is excluded. Run `./gradlew updateKotlinAbi` only after a deliberate
API change, review the generated diff, then run `./gradlew checkKotlinAbi` separately
and verify the consumers. Never hand-edit the dump or refresh it in CI.

ABI validation is not rule verification. It does not, for example, detect a changed
fan value behind an unchanged getter; the independent rule tests remain necessary.
