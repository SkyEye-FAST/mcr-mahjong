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
    fun sevenPairsSubtractOnlyOneInevitableTileHog() {
        // Chinese non-repetition principle and the All Green seven-pairs example:
        // six available tile kinds force one quad, but not a second or third.
        val cases = listOf(
            Triple("223344668888sFF", Fan.ALL_GREEN, 1),
            Triple("22333344668888s", Fan.ALL_GREEN, 2),
            Triple("22223333666688s", Fan.ALL_GREEN, 3),
            Triple("1199m1199s119999p", Fan.ALL_TERMINALS, 1),
            Triple("11119999m1199s11p", Fan.ALL_TERMINALS, 2),
            Triple("11119999m1111s99p", Fan.ALL_TERMINALS, 3),
        )
        for ((text, compound, quads) in cases) for (selfDrawn in listOf(false, true)) {
            val result = score(text, selfDrawn)
            assertEquals(1, result.count(Fan.SEVEN_PAIRS), text)
            assertEquals(1, result.count(compound), text)
            assertEquals(quads - 1, result.count(Fan.TILE_HOG), text)
            assertEquals(result.totalFan, result.fans.sumOf { it.points }, text)
        }
        // Ordinary Seven Pairs has no inevitable quad: retain zero through three.
        for ((quads, text) in listOf(
            "113355m2277s4499p", "111133m2277s4499p",
            "11113333m77s4499p", "11113333m7777s99p",
        ).withIndex()) {
            val result = score(text)
            assertEquals(1, result.count(Fan.SEVEN_PAIRS), text)
            assertEquals(quads, result.count(Fan.TILE_HOG), text)
        }
        // The raw compatibility vocabulary and full quad count remain unchanged.
        val raw = ReferenceProtocol.compactScore(ReferenceProtocol.calculate("F|223344668888sFF|1|0|0|0"))
        assertTrue(raw.contains("TILE_HOG=1"))
    }

    @Test
    fun regularAllGreenCandidateRetainsItsOwnTileHogExclusions() {
        // This hand is also Seven Pairs with three quads, but four 234 chows score more.
        // The seven-pairs deduction must not leak into the regular candidate.
        val result = score("22223333444466s")
        assertEquals(1, result.count(Fan.ALL_GREEN))
        assertEquals(1, result.count(Fan.QUADRUPLE_CHOW))
        assertEquals(0, result.count(Fan.SEVEN_PAIRS))
        assertEquals(0, result.count(Fan.TILE_HOG))
        assertEquals(166, result.totalFan)
        assertEquals(result.totalFan, result.fans.sumOf { it.points })
        // A regular All Green hand may also score a non-inevitable Tile Hog.
        val pung = score("[222s][234s][666s][FFF]8s8s")
        assertEquals(1, pung.count(Fan.TILE_HOG))
        assertEquals(0, pung.count(Fan.SEVEN_PAIRS))
    }

    @Test
    fun nineGatesRetainsTheRecordedTerminalPungInterpretation() {
        // Characterize the unresolved one-pung vs whole-fan exclusion in RULES_REVIEW.md.
        // These are retained results, not a new rule decision.
        val discardTotals = listOf(106, 92, 89, 89, 91, 89, 89, 92, 106)
        for (rank in 1..9) for (selfDrawn in listOf(false, true)) {
            val result = score("1112345678999m${rank}m", selfDrawn)
            assertEquals(1, result.count(Fan.NINE_GATES))
            assertEquals(if (rank in listOf(2, 5, 8)) 1 else 0, result.count(Fan.PUNG_OF_TERMINALS_OR_HONORS))
            assertEquals(discardTotals[rank - 1] + if (selfDrawn) 4 else 0, result.totalFan)
            assertEquals(if (selfDrawn) 1 else 0, result.count(Fan.FULLY_CONCEALED_HAND))
            for (excluded in listOf(Fan.FULL_FLUSH, Fan.CONCEALED_HAND, Fan.SELF_DRAWN)) {
                assertEquals(0, result.count(excluded))
            }
            assertEquals(result.totalFan, result.fans.sumOf { it.points })
        }
    }

    @Test
    fun sevenShiftedPairsAddsFullyConcealedWithoutRepeatingSevenPairs() {
        // Chinese fans 6 and 19 add Fully Concealed Hand on self-draw.
        // A shifted-pairs hand uses its 88-point category, not an extra ordinary Seven Pairs.
        for (selfDrawn in listOf(false, true)) {
            val result = score("11223344556677m", selfDrawn)
            assertEquals(listOf(FanCount(Fan.SEVEN_SHIFTED_PAIRS, 1)) +
                if (selfDrawn) listOf(FanCount(Fan.FULLY_CONCEALED_HAND, 1)) else emptyList(), result.fans)
            assertEquals(if (selfDrawn) 92 else 88, result.totalFan)
        }
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
    fun knittedDragonWaitFansUseOnlyTheUniqueResidualWait() {
        // WMO Chinese MCR, Combination Dragon example (printed p. 35) combines
        // the knitted body with a residual wait. Wait definitions (pp. 45-46)
        // require that the residual hand has only that winning tile. These cases
        // deliberately use a winning tile also present in the knitted body; the
        // residual can use only its separate physical copy. Each expected total
        // sums the selected fans using the Chinese table values (pp. 12-14).
        val cases = listOf(
            Triple("1233369m147s258p3m", Fan.EDGE_WAIT, 19),
            Triple("2333469m147s258p3m", Fan.CLOSED_WAIT, 19),
            Triple("3369m147s258pEEE3m", Fan.SINGLE_WAIT, 19),
            // 2024 Canadian event example and its suit/rank-equivalent residual 13 + pair.
            Triple("12358s33369p147m2s", Fan.CLOSED_WAIT, 17),
            Triple("147s12358m36999p2m", Fan.CLOSED_WAIT, 17),
        )
        for ((text, waitFan, total) in cases) {
            val result = score(text)
            assertEquals(1, result.count(Fan.KNITTED_STRAIGHT), text)
            assertEquals(1, result.count(waitFan), text)
            for (otherWait in listOf(Fan.EDGE_WAIT, Fan.CLOSED_WAIT, Fan.SINGLE_WAIT) - waitFan) {
                assertEquals(0, result.count(otherWait), text)
            }
            assertEquals(total, result.totalFan, text)
            assertTrue(result.meetsMinimum, text)
            assertEquals(result.totalFan, result.fans.sumOf { it.points }, text)
        }

        // Here the 3m only completes the knitted body; the chow and pair in the
        // residual were already complete, so no wait fan can be added.
        val bodyOnly = score("45669m147s258pEE3m")
        assertEquals(1, bodyOnly.count(Fan.KNITTED_STRAIGHT))
        assertEquals(0, bodyOnly.count(Fan.EDGE_WAIT))
        assertEquals(0, bodyOnly.count(Fan.CLOSED_WAIT))
        assertEquals(0, bodyOnly.count(Fan.SINGLE_WAIT))

        // Residual 2344 before the win has two waits (1 and 4); the winning 4
        // completes a legal hand but no wait fan is unique.
        val ambiguous = score("1234447m258s369p4m")
        assertEquals(1, ambiguous.count(Fan.KNITTED_STRAIGHT))
        assertEquals(0, ambiguous.count(Fan.EDGE_WAIT))
        assertEquals(0, ambiguous.count(Fan.CLOSED_WAIT))
        assertEquals(0, ambiguous.count(Fan.SINGLE_WAIT))
        assertTrue(ambiguous.meetsMinimum)
        assertEquals(ambiguous.totalFan, ambiguous.fans.sumOf { it.points })
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
        assertEquals(122, result.totalFan)
        assertEquals(1, result.count(Fan.ALL_GREEN))
        assertEquals(1, result.count(Fan.SEVEN_PAIRS))
        assertEquals(1, result.count(Fan.HALF_FLUSH))
        assertEquals(0, result.count(Fan.TILE_HOG))
        assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND))
        val pureSuit = score("22334466888866s", selfDrawn = true)
        assertEquals(1, pureSuit.count(Fan.ALL_GREEN))
        assertEquals(1, pureSuit.count(Fan.FULL_FLUSH))
        assertEquals(0, pureSuit.count(Fan.HALF_FLUSH))
        assertEquals(0, score("[234s][234s][234s][234s]6s6s").count(Fan.HALF_FLUSH))
    }

    private fun assertKongCombinations(result: ScoreResult.Winning, vararg expected: Fan) {
        for (fan in listOf(Fan.CONCEALED_KONG, Fan.TWO_CONCEALED_KONGS,
            Fan.TWO_CONCEALED_PUNGS, Fan.THREE_CONCEALED_PUNGS, Fan.FOUR_CONCEALED_PUNGS,
            Fan.MELDED_KONG, Fan.TWO_MELDED_KONGS)) {
            assertEquals(if (fan in expected) 1 else 0, result.count(fan), "$fan in $result")
        }
        assertEquals(result.totalFan, result.fans.sumOf { it.points })
    }

    @Test
    fun threeKongsApplyTheChineseConcealedKongClause() {
        // Chinese fan 17 (p. 28): add concealed kongs; three add Three Concealed Pungs.
        val expected = listOf(32, 34, 40, 50)
        val awards = listOf(emptyArray(), arrayOf(Fan.CONCEALED_KONG),
            arrayOf(Fan.TWO_CONCEALED_KONGS), arrayOf(Fan.THREE_CONCEALED_PUNGS))
        for (concealed in 0..3) {
            val melds = listOf(Tile.S2, Tile.S3, Tile.P5).mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            }
            val result = assertIs<ScoreResult.Winning>(McrMahjong.score(
                Hand(Tiles.parse("67mEE"), melds), Tile.M8,
            ))
            assertEquals(expected[concealed], result.totalFan, "concealed=$concealed")
            assertEquals(1, result.count(Fan.THREE_KONGS))
            assertKongCombinations(result, *awards[concealed])
        }
    }

    @Test
    fun threeKongsCombineConcealedKongsWithTheConcealedPungSeries() {
        // The fourth pung is concealed on self-draw, exposed when completed by discard.
        val selfDrawnTotals = listOf(39, 43, 63, 100)
        val discardTotals = listOf(38, 40, 46, 56)
        for (concealed in 0..3) for (selfDrawn in listOf(false, true)) {
            val melds = listOf(Tile.M2, Tile.S5, Tile.P8).mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            }
            val result = assertIs<ScoreResult.Winning>(McrMahjong.score(
                Hand(listOf(Tile.M4, Tile.M4, Tile.EAST, Tile.EAST), melds), Tile.M4,
                WinContext(method = if (selfDrawn) WinMethod.SELF_DRAW else WinMethod.DISCARD),
            ))
            assertEquals((if (selfDrawn) selfDrawnTotals else discardTotals)[concealed], result.totalFan)
            assertEquals(1, result.count(Fan.THREE_KONGS))
            val awards = when (concealed) {
                0 -> emptyArray()
                1 -> if (selfDrawn) arrayOf(Fan.CONCEALED_KONG, Fan.TWO_CONCEALED_PUNGS) else arrayOf(Fan.CONCEALED_KONG)
                2 -> if (selfDrawn) arrayOf(Fan.TWO_CONCEALED_KONGS, Fan.THREE_CONCEALED_PUNGS) else arrayOf(Fan.TWO_CONCEALED_KONGS)
                else -> arrayOf(if (selfDrawn) Fan.FOUR_CONCEALED_PUNGS else Fan.THREE_CONCEALED_PUNGS)
            }
            assertKongCombinations(result, *awards)
        }
    }

    @Test
    fun threeKongsWithAFixedPungKeepTheSameConcealedKongExclusions() {
        // Fixed fourth pung, winning on the pair: distinct from a pung completed by discard.
        val expected = listOf(44, 41, 47, 55)
        val awards = listOf(emptyArray(), arrayOf(Fan.CONCEALED_KONG),
            arrayOf(Fan.TWO_CONCEALED_KONGS), arrayOf(Fan.THREE_CONCEALED_PUNGS))
        for (concealed in 0..3) {
            val melds = listOf(Tile.M2, Tile.S5, Tile.P8).mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            } + Meld.Pung(Tile.M4)
            val result = assertIs<ScoreResult.Winning>(McrMahjong.score(Hand(listOf(Tile.EAST), melds), Tile.EAST))
            assertEquals(expected[concealed], result.totalFan)
            assertEquals(1, result.count(Fan.THREE_KONGS))
            assertKongCombinations(result, *awards[concealed])
        }
    }

    @Test
    fun fourKongsAddConcealedKongFansAndKeepTheFourConcealedPungCombination() {
        // Chinese fan 5 (p. 24): concealed kongs add; Single Wait is excluded.
        val expected = listOf(94, 90, 96, 104, 156)
        val awards = listOf(emptyArray(), arrayOf(Fan.CONCEALED_KONG), arrayOf(Fan.TWO_CONCEALED_KONGS),
            arrayOf(Fan.THREE_CONCEALED_PUNGS), arrayOf(Fan.FOUR_CONCEALED_PUNGS))
        for (concealed in 0..4) {
            val melds = listOf(Tile.M2, Tile.M8, Tile.S4, Tile.P7).mapIndexed { index, tile ->
                Meld.Kong(tile, if (index < concealed) null else RelativePlayer.LEFT)
            }
            val result = assertIs<ScoreResult.Winning>(McrMahjong.score(
                Hand(listOf(Tile.EAST), melds), Tile.EAST,
                WinContext(method = if (concealed == 4) WinMethod.SELF_DRAW else WinMethod.DISCARD),
            ))
            assertEquals(expected[concealed], result.totalFan, "concealed=$concealed")
            assertEquals(1, result.count(Fan.FOUR_KONGS))
            assertEquals(0, result.count(Fan.SINGLE_WAIT))
            assertKongCombinations(result, *awards[concealed])
            if (concealed == 4) {
                assertEquals(1, result.count(Fan.FULLY_CONCEALED_HAND))
                assertEquals(0, result.count(Fan.SELF_DRAWN))
            }
        }
    }

    @Test
    fun normalizedEvaluationsAreThreadSafeAndDoNotMutateTheRawBaseline() {
        val jobs = listOf(
            "[1111m][2222s1]345pEE67s8s" to 8,
            "44556m445566s55p6m" to 52,
            "223344668888sFF" to 122,
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
