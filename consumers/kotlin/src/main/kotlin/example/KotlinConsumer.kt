package example

import top.skyeyefast.mcr.*

fun main() {
    val hand = Hand(Tiles.parse("258m1477s369p"), listOf(Meld.Chow(Tile.M4, ChowPosition.HIGH)))
    check(McrMahjong.shanten(hand) == 0)
    check(Tile.S7 in McrMahjong.waitingTiles(hand))
    check(McrMahjong.analyze(hand).forms.any { it.form == HandForm.KNITTED_STRAIGHT })
    val context = WinContext(seatWind = Wind.SOUTH, flowerCount = 2)
    val result = McrMahjong.score(hand, Tile.S7, context)
    when (result) {
        is ScoreResult.Winning -> {
            check(result.totalFan == 17 && result.nonFlowerFan == 15 && result.meetsMinimum)
            check(result.count(Fan.KNITTED_STRAIGHT) == 1)
        }
        ScoreResult.NotWinning -> error("Expected knitted straight")
    }
    check(HandForm.KNITTED_STRAIGHT in McrMahjong.discards(hand, Tile.S7).first().completesForms)
    // A standards correction must survive packaging and maximum-score selection.
    val pairs = McrMahjong.score(Hand(Tiles.parse("44556m445566s55p")), Tile.M6,
        WinContext(method = WinMethod.SELF_DRAW)) as ScoreResult.Winning
    check(pairs.totalFan == 52 && pairs.count(Fan.SEVEN_PAIRS) == 1)
    val mixed = McrMahjong.score(
        Hand(Tiles.parse("345pEE67s"), listOf(Meld.Kong(Tile.M1), Meld.Kong(Tile.S2, RelativePlayer.LEFT))),
        Tile.S8, WinContext(method = WinMethod.SELF_DRAW),
    ) as ScoreResult.Winning
    check(mixed.totalFan == 8 && mixed.meetsMinimum)
    check(mixed.fans.single { it.isMixedKongPair }.points == 6)
    println("Kotlin / Gradle metadata consumer passed: $result")
}
