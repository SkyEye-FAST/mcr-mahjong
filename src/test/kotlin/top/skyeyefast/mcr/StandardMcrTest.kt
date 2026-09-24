package top.skyeyefast.mcr

import org.junit.jupiter.api.Test
import top.skyeyefast.mcr.internal.UpstreamFan
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Independent rule assertions, not expectations generated from the C++ compatibility oracle. */
class StandardMcrTest {
    private fun score(text: String, selfDrawn: Boolean = false, flowers: Int = 0): ScoreResult.Winning {
        val (hand, tile) = ReferenceProtocol.parse(text)
        return assertIs(McrMahjong.score(hand, tile!!, WinContext(
            method = if (selfDrawn) WinMethod.SELF_DRAW else WinMethod.DISCARD,
            flowerCount = flowers,
        )))
    }

    @Test
    fun publicVocabularyIsStandardAndOracleVocabularyIsNotChanged() {
        assertEquals("wmo-2014-zh", McrMahjong.SCORING_PROFILE)
        assertEquals(81, Fan.entries.size)
        assertEquals(82, UpstreamFan.entries.size)
        assertEquals(Fan.entries.map { it.name }, UpstreamFan.entries.take(81).map { it.name })
        for (fan in Fan.entries) {
            assertEquals(if (fan == Fan.TWO_CONCEALED_KONGS) 8 else UpstreamFan.valueOf(fan.name).points, fan.points)
        }
        assertEquals(5, UpstreamFan.CONCEALED_KONG_AND_MELDED_KONG.points)
        assertEquals(6, UpstreamFan.TWO_CONCEALED_KONGS.points)
    }

    @Test
    fun mixedKongsUseTheSixPointClauseAndCanReachMinimum() {
        // Chinese MCR Annex I, fan 57 (printed p. 40): the mixed pair is six.
        val result = score("[1111m][2222s1]345pEE67s8s", selfDrawn = true)
        assertEquals(8, result.totalFan)
        assertEquals(1, result.count(Fan.TWO_MELDED_KONGS))
        assertEquals(0, result.count(Fan.CONCEALED_KONG))
        assertEquals(0, result.count(Fan.MELDED_KONG))
        val kongs = result.fans.single { it.fan == Fan.TWO_MELDED_KONGS }
        assertTrue(kongs.isMixedKongPair)
        assertEquals(6, kongs.points)
        assertEquals(result.totalFan, result.fans.sumOf { it.points })
        assertFailsWith<IllegalArgumentException> { kongs.copy(count = 2) }
        assertFailsWith<IllegalArgumentException> { kongs.copy(fan = Fan.CONCEALED_KONG) }
        assertFailsWith<IllegalArgumentException> { FanCount(Fan.CONCEALED_KONG, 1, isMixedKongPair = true) }
        assertTrue(result.meetsMinimum)
        val raw = ReferenceProtocol.compactScore(ReferenceProtocol.calculate("F|[1111m][2222s1]345pEE67s8s|1|0|0|0"))
        assertTrue(raw.startsWith("7;"))
        assertTrue(raw.contains("CONCEALED_KONG_AND_MELDED_KONG=1"))
        assertEquals(result.fans, score("[1111m][2222s5]345pEE67s8s", selfDrawn = true).fans)
    }

    @Test
    fun twoConcealedKongsAreEightAndFlowersStaySeparate() {
        // Chinese MCR fan table (printed p. 14): Two Concealed Kongs is eight.
        val result = score("[1111m][2222s]345pEE67s8s", selfDrawn = true, flowers = 3)
        assertEquals(13, result.nonFlowerFan)
        assertEquals(16, result.totalFan)
        assertEquals(1, result.count(Fan.TWO_CONCEALED_KONGS))
        assertEquals(0, result.count(Fan.TWO_CONCEALED_PUNGS))
        assertEquals(0, result.count(Fan.CONCEALED_KONG))
    }

    @Test
    fun mandatoryConcealedFormsCanCombineFullyConcealedOnSelfDraw() {
        // Chinese MCR Annex I: these forms explicitly add Fully Concealed Hand
        // on self-draw; see printed pp. 23-30 and 34-35.
        val cases = listOf(
            "19m19s19pESWNCFPN" to 92,
            "11223344556677m" to 92,
            "1133557799m22s44p" to 29,
            "1112345678999p9p" to 110,
            "EESSWWNNCCFFPP" to 92,
            "69m258s1pESWNCFP3m" to 28,
            "69m258s17pEWNCFP3m" to 16,
        )
        for ((text, total) in cases) {
            val result = score(text, selfDrawn = true)
            assertEquals(total, result.totalFan, text)
            assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND), text)
            assertEquals(0, result.count(Fan.SELF_DRAWN), text)
            assertEquals(0, score(text).count(Fan.FULLY_CONCEALED_HAND), text)
        }
        val pungs = score("111m222s333pEEEFF", selfDrawn = true)
        assertEquals(1, pungs.count(Fan.FOUR_CONCEALED_PUNGS))
        assertEquals(1, pungs.count(Fan.FULLY_CONCEALED_HAND))
    }

    @Test
    fun normalizationPrecedesBestDecompositionSelection() {
        // Upstream prefers the regular 49-point interpretation in a 49/49 tie.
        // The standard seven-pairs interpretation is 24 + 24 + 4 = 52, not 49.
        val result = score("44556m445566s55p6m", selfDrawn = true)
        assertEquals(52, result.totalFan)
        assertEquals(listOf(FanCount(Fan.SEVEN_PAIRS, 1), FanCount(Fan.MIDDLE_TILES, 1),
            FanCount(Fan.FULLY_CONCEALED_HAND, 1)), result.fans)
        val raw = ReferenceProtocol.compactScore(ReferenceProtocol.calculate("F|44556m445566s55p6m|1|0|0|0"))
        assertTrue(raw.startsWith("49;"))
        assertTrue(raw.contains("ALL_FIVE=1"))
    }

    @Test
    fun allTerminalsMayAddDoublePungsButDoesNotDoubleCountATriple() {
        // Chinese MCR Annex I, All Terminals (printed p. 26) explicitly shows
        // two Double Pungs; the triple-pung example must not split those pungs.
        val doubles = score("[111m][111s][999m]99s1p1p9s")
        assertEquals(68, doubles.totalFan)
        assertEquals(2, doubles.count(Fan.DOUBLE_PUNG))
        val triple = score("[111m][111p][111s]99s99p9p")
        assertEquals(1, triple.count(Fan.TRIPLE_PUNG))
        assertEquals(0, triple.count(Fan.DOUBLE_PUNG))
        assertEquals(0, score("1199m1199s11999p9p").count(Fan.DOUBLE_PUNG))
    }

    @Test
    fun greenHandsRetainTheirExplicitHalfFlushCombination() {
        // Chinese MCR Annex I (printed p. 23): Green One explicitly allows
        // Half Flush with green dragons and Full Flush without honors.
        val result = score("223344668888sFF", selfDrawn = true)
        assertEquals(124, result.totalFan)
        assertEquals(1, result.count(Fan.ALL_GREEN))
        assertEquals(1, result.count(Fan.SEVEN_PAIRS))
        assertEquals(1, result.count(Fan.HALF_FLUSH))
        assertEquals(1, result.count(Fan.TILE_HOG))
        assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND))
        val pureSuit = score("22334466888866s", selfDrawn = true)
        assertEquals(1, pureSuit.count(Fan.ALL_GREEN))
        assertEquals(1, pureSuit.count(Fan.FULL_FLUSH))
        assertEquals(0, pureSuit.count(Fan.HALF_FLUSH))
        assertEquals(0, score("[234s][234s][234s][234s]6s6s").count(Fan.HALF_FLUSH))
    }

    @Test
    fun threeKongsApplyTheChineseConcealedKongClause() {
        // Chinese MCR Annex I, fan 17 (printed p. 28): concealed kongs add;
        // three concealed kongs also add Three Concealed Pungs.
        val one = score("[2222s][3333s1][5555p1]67mEE8m")
        assertEquals(34, one.totalFan)
        assertEquals(1, one.count(Fan.THREE_KONGS))
        assertEquals(1, one.count(Fan.CONCEALED_KONG))
        val two = score("[2222s][3333s][5555p1]67mEE8m")
        assertEquals(40, two.totalFan)
        assertEquals(1, two.count(Fan.TWO_CONCEALED_KONGS))
        assertEquals(0, two.count(Fan.TWO_CONCEALED_PUNGS))
        val three = score("[2222s][3333s][5555p]67mEE8m")
        assertEquals(50, three.totalFan)
        assertEquals(1, three.count(Fan.THREE_CONCEALED_PUNGS))
        assertEquals(0, three.count(Fan.TWO_CONCEALED_KONGS))
        assertEquals(0, three.count(Fan.CONCEALED_KONG))
    }

    @Test
    fun threeKongsCombineConcealedKongsWithTheConcealedPungSeries() {
        // Chinese MCR Annex I, fan 17 (p. 28), plus Three Concealed Pungs (p. 33):
        // concealed kongs also qualify as concealed pungs for the concealed-set series.
        val kongTiles = listOf(Tile.M2, Tile.S5, Tile.P8)
        val expectedTotals = listOf(43, 63, 100)
        for (concealed in 1..3) {
            val melds = kongTiles.mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            }
            val result = McrMahjong.score(
                Hand(listOf(Tile.M4, Tile.M4, Tile.EAST, Tile.EAST), melds), Tile.M4,
                WinContext(method = WinMethod.SELF_DRAW),
            ) as ScoreResult.Winning
            assertEquals(expectedTotals[concealed - 1], result.totalFan, "concealed=$concealed $result")
            assertEquals(result.totalFan, result.fans.sumOf { it.points }, "concealed=$concealed")
            assertEquals(1, result.count(Fan.THREE_KONGS))
            when (concealed) {
                1 -> {
                    assertEquals(1, result.count(Fan.CONCEALED_KONG))
                    assertEquals(1, result.count(Fan.TWO_CONCEALED_PUNGS))
                    assertEquals(0, result.count(Fan.TWO_CONCEALED_KONGS))
                }
                2 -> {
                    assertEquals(1, result.count(Fan.TWO_CONCEALED_KONGS))
                    assertEquals(1, result.count(Fan.THREE_CONCEALED_PUNGS))
                    assertEquals(0, result.count(Fan.TWO_CONCEALED_PUNGS))
                }
                3 -> {
                    assertEquals(1, result.count(Fan.FOUR_CONCEALED_PUNGS))
                    assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND))
                    assertEquals(0, result.count(Fan.CONCEALED_KONG))
                }
            }
        }
    }

    @Test
    fun fourKongsAddConcealedKongFansAndKeepTheFourConcealedPungCombination() {
        // Chinese MCR Annex I (printed p. 24): Four Kongs adds concealed kongs
        // and excludes Single Wait. The fan table (printed pp. 12-14) supplies
        // the concealed-kong / concealed-pung values.
        val tiles = listOf(Tile.M2, Tile.M8, Tile.S4, Tile.P7)
        val expected = listOf(94, 90, 96, 104, 156)
        for (concealed in 0..4) {
            val melds = tiles.mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            }
            val result = assertIs<ScoreResult.Winning>(McrMahjong.score(
                Hand(listOf(Tile.EAST), melds), Tile.EAST,
                WinContext(method = if (concealed == 4) WinMethod.SELF_DRAW else WinMethod.DISCARD),
            ))
            assertEquals(expected[concealed], result.totalFan, "concealed=$concealed $result")
            assertEquals(result.totalFan, result.fans.sumOf { it.points }, "concealed=$concealed")
            assertEquals(0, result.count(Fan.SINGLE_WAIT), "concealed=$concealed")
            when (concealed) {
                1 -> assertEquals(1, result.count(Fan.CONCEALED_KONG))
                2 -> {
                    assertEquals(1, result.count(Fan.TWO_CONCEALED_KONGS))
                    assertEquals(0, result.count(Fan.TWO_CONCEALED_PUNGS))
                }
                3 -> assertEquals(1, result.count(Fan.THREE_CONCEALED_PUNGS))
                4 -> {
                    assertEquals(1, result.count(Fan.FOUR_CONCEALED_PUNGS))
                    assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND))
                    assertEquals(0, result.count(Fan.SELF_DRAWN))
                }
            }
        }
    }

    @Test
    fun normalizedEvaluationsAreThreadSafeAndDoNotMutateTheRawBaseline() {
        val jobs = listOf(
            "[1111m][2222s1]345pEE67s8s" to 8,
            "44556m445566s55p6m" to 52,
            "223344668888sFF" to 124,
            "19m19s19pESWNCFPN" to 92,
        )
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val results = (jobs + jobs).map { (text, total) ->
                executor.submit<Boolean> { score(text, selfDrawn = true).totalFan == total }
            }
            assertTrue(results.all { it.get(5, java.util.concurrent.TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
        assertTrue(ReferenceProtocol.compactScore(ReferenceProtocol.calculate(
            "F|44556m445566s55p6m|1|0|0|0")).startsWith("49;"))
    }
}
