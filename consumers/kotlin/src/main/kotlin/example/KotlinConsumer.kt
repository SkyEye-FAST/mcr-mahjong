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
    println("Kotlin / Gradle metadata consumer passed: $result")
}
