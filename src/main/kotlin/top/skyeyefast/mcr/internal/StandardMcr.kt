package top.skyeyefast.mcr.internal

import top.skyeyefast.mcr.Fan
import top.skyeyefast.mcr.FanCount
import top.skyeyefast.mcr.Hand
import top.skyeyefast.mcr.Meld
import top.skyeyefast.mcr.ScoreResult
import top.skyeyefast.mcr.Tile
import top.skyeyefast.mcr.WinContext
import top.skyeyefast.mcr.WinMethod
import top.skyeyefast.mcr.internal.UpstreamFan.*

/** WMO English revised text (2013 postscript); exact source is pinned in COMPATIBILITY.md. */
internal object StandardMcr {
    private operator fun IntArray.get(fan: UpstreamFan): Int = this[fan.index]
    private operator fun IntArray.set(fan: UpstreamFan, value: Int) { this[fan.index] = value }

    fun calculate(hand: Hand, winningTile: Tile, context: WinContext): ScoreResult {
        val raw = FanCalculator.calculate(
            hand.standing(), hand.fixed(), winningTile.code, context.flags(),
            context.prevalentWind.ordinal, context.seatWind.ordinal, context.flowerCount,
        ) { table, packs ->
            // Fan 57 values one concealed + one melded kong at six points.
            // Keep a private marker for that exception, not an extra public fan.
            if (table[CONCEALED_KONG_AND_MELDED_KONG] != 0) {
                table[TWO_MELDED_KONGS] = 1
            }

            // Restore Half Flush for All Green only when green dragons are present.
            if (table[ALL_GREEN] != 0 && (winningTile == Tile.GREEN || hand.remainingCopies(Tile.GREEN) < 4)) {
                table[HALF_FLUSH] = 1
            }

            // Revised English appendix fan 17 (printed p. 39) permits Concealed
            // Kong / Two Concealed Kongs within Three Kongs. This is independent
            // of the mixed-two-kong clause and never applies to Four Kongs.
            if (table[THREE_KONGS] != 0) {
                when (hand.melds.count { it is Meld.Kong && it.from == null }) {
                    1 -> table[CONCEALED_KONG] = 1
                    2 -> {
                        table[TWO_CONCEALED_KONGS] = 1
                        table[TWO_CONCEALED_PUNGS] = 0
                    }
                }
            }

            // These mandatory-concealed forms explicitly allow Fully Concealed
            // Hand on self-draw in the selected English text, not just Self Drawn.
            if (context.method == WinMethod.SELF_DRAW && (
                    table[NINE_GATES] != 0 || table[SEVEN_SHIFTED_PAIRS] != 0 || table[THIRTEEN_ORPHANS] != 0 ||
                    table[FOUR_CONCEALED_PUNGS] != 0 || table[SEVEN_PAIRS] != 0 ||
                    table[GREATER_HONORS_AND_KNITTED_TILES] != 0 || table[LESSER_HONORS_AND_KNITTED_TILES] != 0)) {
                table[SELF_DRAWN] = 0
                table[FULLY_CONCEALED_HAND] = 1
            }

            // Reuse the enumerated packs; do not invent pungs in a seven-pairs
            // candidate or double-count a combination absorbed by Triple Pung.
            if (table[ALL_TERMINALS] != 0 && table[TRIPLE_PUNG] == 0 && packs != null) {
                val pungs = packs.filter { packType(it) == PUNG || packType(it) == KONG }.map(::packTile)
                table[DOUBLE_PUNG] = pungs.indices.sumOf { i ->
                    (i + 1 until pungs.size).count { j -> sameRank(pungs[i], pungs[j]) }
                }
            }

            // Two Concealed Kongs uses the public eight-point value. The private
            // mixed-kong marker replaces the four-point base award with six.
            Fan.entries.sumOf { it.points * table[it.index] } +
                if (table[CONCEALED_KONG_AND_MELDED_KONG] != 0) 2 else 0
        }
        if (raw.total == -3) return ScoreResult.NotWinning
        check(raw.total >= 0) { "Unexpected internal calculator result: ${raw.total}" }
        val fans = Fan.entries.filter { raw.table[it.index] > 0 }.map {
            FanCount(it, raw.table[it.index], it == Fan.TWO_MELDED_KONGS && raw.table[CONCEALED_KONG_AND_MELDED_KONG] != 0)
        }
        check(fans.sumOf { it.points } == raw.total) { "Scoring breakdown does not match the selected candidate" }
        return ScoreResult.Winning.create(fans)
    }
}
