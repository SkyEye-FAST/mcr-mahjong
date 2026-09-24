package top.skyeyefast.mcr

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegressionTest {
    private fun fixtures(name: String): List<Pair<String, String>> =
        requireNotNull(javaClass.getResourceAsStream("/$name")).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val columns = line.split('\t', limit = 2)
                require(columns.size == 2) { "Malformed fixture: $line" }
                columns[0] to columns[1]
            }.toList()
        }

    @TestFactory
    fun frozenCppFanTables(): List<DynamicTest> = fixtures("fan-regression.tsv").map { (request, expected) ->
        DynamicTest.dynamicTest(request) {
            assertEquals(expected, ReferenceProtocol.compactScore(ReferenceProtocol.calculate(request)))
        }
    }

    @TestFactory
    fun frozenCppShantenAndDiscards(): List<DynamicTest> = fixtures("analysis-regression.tsv").map { (request, expected) ->
        DynamicTest.dynamicTest(request) {
            val result = ReferenceProtocol.calculate(request)
            val actual = if (request.startsWith("D|")) MessageDigest.getInstance("SHA-256")
                .digest(result.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) } else result
            assertEquals(expected, actual)
        }
    }

    @Test
    fun regressionCorpusIncludesEveryEnabledFan() {
        for (name in listOf("fan-regression.tsv", "analysis-regression.tsv")) {
            val inputs = fixtures(name).map { it.first }
            assertEquals(inputs.size, inputs.distinct().size, "Duplicate fixtures in $name")
        }
        val covered = fixtures("fan-regression.tsv").flatMap { (_, expected) ->
            expected.substringAfter(';').split(',').filter { it.isNotEmpty() }.map { it.substringBefore('=') }
        }.toSet()
        assertEquals(Fan.entries.map { it.name }.toSet(), covered)
    }

    @Test
    fun validationAndImmutableBoundaries() {
        assertFailsWith<IllegalArgumentException> { Hand(Tiles.parse("123m")) }
        assertFailsWith<IllegalArgumentException> { Hand(Tiles.parse("11111m123s123pEE")) }
        assertFailsWith<IllegalArgumentException> { Meld.Chow(Tile.EAST) }
        assertFailsWith<IllegalArgumentException> { Meld.Chow(Tile.M1) }
        assertFailsWith<IllegalArgumentException> { Meld.Kong(Tile.M1, promoted = true) }
        assertFailsWith<IllegalArgumentException> { WinContext(flowerCount = 9) }
        assertFailsWith<IllegalArgumentException> { Tiles.parse("123") }
        assertFailsWith<IllegalArgumentException> { Tiles.parse("0m") }
        assertFailsWith<IllegalArgumentException> { Tile.parse("12m") }
        val input = Tiles.parse("19m19s19pESWNCFP").toMutableList()
        val hand = Hand(input)
        input.clear()
        assertEquals(13, hand.concealedTiles.size)
        assertFailsWith<UnsupportedOperationException> { (hand.concealedTiles as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (McrMahjong.analyze(hand).forms as MutableList).clear() }
    }

    @Test
    fun fifthCopyIsStructuralOnlyAndVisibilityIsNotDoubleCounted() {
        val hand = Hand(listOf(Tile.M1), listOf(Meld.Pung(Tile.M1), Meld.Chow(Tile.S2), Meld.Chow(Tile.P5), Meld.Pung(Tile.EAST)))
        val analysis = McrMahjong.analyze(hand)
        assertEquals(0, analysis.shanten)
        assertEquals(listOf(EffectiveTile(Tile.M1, 0)), analysis.effectiveTiles)
        assertEquals(setOf(Tile.M1), McrMahjong.waitingTiles(hand))
        assertFailsWith<IllegalArgumentException> { McrMahjong.score(hand, Tile.M1) }
        assertFailsWith<IllegalArgumentException> { McrMahjong.analyze(hand, listOf(Tile.M1)) }
        val orphan = Hand(Tiles.parse("19m19s19pESWNCFP"))
        assertEquals(39, McrMahjong.analyze(orphan).remainingCount)
        assertEquals(38, McrMahjong.analyze(orphan, listOf(Tile.M1)).remainingCount)
        val discardDrawn = McrMahjong.discards(orphan, Tile.M1).first()
        assertEquals(Tile.M1, discardDrawn.discard)
        assertEquals(0, discardDrawn.shanten)
        assertTrue(HandForm.THIRTEEN_ORPHANS in discardDrawn.completesForms)
        assertEquals(38, discardDrawn.remainingCount)
    }

    @Test
    fun flowersDoNotQualifyASevenPointHand() {
        val (hand, tile) = ReferenceProtocol.parse("[1111m][2222s1]345pEE67s8s")
        val score = assertIs<ScoreResult.Winning>(McrMahjong.score(hand, tile!!, WinContext(method = WinMethod.SELF_DRAW, flowerCount = 8)))
        assertEquals(15, score.totalFan)
        assertEquals(7, score.nonFlowerFan)
        assertEquals(8, score.count(Fan.FLOWER_TILES))
        assertFalse(score.meetsMinimum)
        val (losing, drawn) = ReferenceProtocol.parse("123m456s789pEE23m5m")
        assertEquals(ScoreResult.NotWinning, McrMahjong.score(losing, drawn!!))
    }

    @Test
    fun noSharedMutableEvaluationState() {
        val requests = fixtures("fan-regression.tsv").take(8)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val jobs = (1..3).flatMap { requests }.map { (request, expected) ->
                executor.submit<Boolean> { expected == ReferenceProtocol.compactScore(ReferenceProtocol.calculate(request)) }
            }
            assertTrue(jobs.all { it.get(5, java.util.concurrent.TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
    }
}
