package top.skyeyefast.mcr

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Random
import kotlin.test.assertEquals

@Tag("differential")
class DifferentialTest {
    private fun verifyUpstream(): String {
        val root = Path.of(".reference/upstream")
        val hashes = Files.readAllLines(Path.of("tools/upstream.sha256"))
            .filter { it.isNotBlank() && !it.startsWith('#') }
        assertEquals(9, hashes.size, "All upstream source files and LICENSE must be pinned")
        for (line in hashes) {
            val (expected, name) = line.split(Regex("\\s+"), limit = 2)
            val text = Files.readString(root.resolve(name)).removePrefix("\uFEFF").replace("\r\n", "\n")
            val actual = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            assertEquals(expected, actual, "Upstream file differs from NOTICE: $name")
        }
        return Files.readString(root.resolve("unit_test.cpp"))
    }

    @Test
    @Timeout(90)
    fun pinnedUpstreamAndDeterministicCorpus() {
        val oraclePath = requireNotNull(System.getProperty("mcr.oracle"))
        val source = verifyUpstream()
        var scores = 0; var analyses = 0; var discards = 0
        Oracle(oraclePath).use { oracle ->
            val manifest = Files.readString(Path.of("tools/upstream.sha256")).replace("\r\n", "\n")
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(manifest.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            assertEquals(fingerprint + "," + Fan.entries.joinToString(",") { it.points.toString() },
                oracle.query("V"), "Oracle revision/profile/fan values")
            val seen = HashSet<String>()
            fun check(request: String) {
                if (!seen.add(request)) return
                assertEquals(oracle.query(request), ReferenceProtocol.calculate(request), request)
                when (request[0]) { 'F' -> scores++; 'S' -> analyses++; 'D' -> discards++ }
            }
            // Read the small frozen regression inputs; no native dependency in ordinary tests.
            for (resource in listOf("/fan-regression.tsv", "/analysis-regression.tsv")) {
                requireNotNull(javaClass.getResourceAsStream(resource)).bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith('#') }.forEach { check(it.substringBefore('\t')) }
                }
            }
            // Every active upstream test_points call is mandatory, not silently optional.
            run {
                val pattern = Regex("(?m)^\\s*test_points\\(\"([^\"]+)\",\\s*([^,]+),\\s*wind_t::(\\w+),\\s*wind_t::(\\w+)\\)")
                val flags = mapOf("WIN_FLAG_DISCARD" to 0, "WIN_FLAG_SELF_DRAWN" to 1, "WIN_FLAG_LAST_TILE" to 2,
                    "WIN_FLAG_KONG_INVOLVED" to 4, "WIN_FLAG_WALL_LAST" to 8, "WIN_FLAG_INITIAL" to 16)
                val matches = pattern.findAll(source).toList()
                assertEquals(214, matches.size, "Pinned upstream test_points inventory changed")
                for (match in matches) {
                    val v = match.groupValues
                    val flag = v[2].split('|').fold(0) { acc, name -> acc or flags.getValue(name.trim()) }
                    check("F|${v[1]}|$flag|${Wind.valueOf(v[3]).ordinal}|${Wind.valueOf(v[4]).ordinal}|0")
                }
            }
            val random = Random(0x4D4352)
            // Construct legal regular wins with 0..4 fixed melds, all three kong kinds,
            // both win methods, winds and flags. Rejection only enforces tile multiplicity.
            var generated = 0
            while (generated < 1000) {
                val fixed = random.nextInt(5)
                val table = IntArray(Tile.entries.size)
                val standing = ArrayList<Tile>()
                val notation = StringBuilder()
                repeat(4) { index ->
                    val tiles: List<Tile>
                    if (random.nextBoolean()) {
                        val base = random.nextInt(3) * 9 + random.nextInt(7)
                        tiles = (0..2).map { Tile.entries[base + it] }
                    } else {
                        val tile = Tile.entries[random.nextInt(Tile.entries.size)]
                        val count = if (index < fixed && random.nextBoolean()) 4 else 3
                        tiles = List(count) { tile }
                    }
                    tiles.forEach { table[it.ordinal]++ }
                    if (index < fixed) {
                        val offer = if (tiles.size == 4) listOf(0, 1, 2, 3, 5, 6, 7)[random.nextInt(7)] else random.nextInt(3) + 1
                        notation.append('[').append(tiles.joinToString("") { it.notation })
                        if (offer != 0) notation.append(offer)
                        notation.append(']')
                    } else standing.addAll(tiles)
                }
                val pair = Tile.entries[random.nextInt(Tile.entries.size)]
                table[pair.ordinal] += 2
                if (table.any { it > 4 }) continue
                standing.add(pair); standing.add(pair)
                val win = standing.removeAt(random.nextInt(standing.size))
                java.util.Collections.shuffle(standing, random)
                val handText = notation.toString() + standing.joinToString("") { it.notation }
                val fullText = handText + win.notation
                check("F|$fullText|${random.nextInt(32)}|${random.nextInt(4)}|${random.nextInt(4)}|${random.nextInt(9)}")
                if (generated < 80) check("S|$handText")
                if (generated < 24) check("D|$fullText")
                generated++
            }
            // Arbitrary non-winning hands are essential for shanten/effective-tile parity.
            repeat(160) {
                val wall = Tile.entries.flatMap { tile -> List(4) { tile } }.toMutableList()
                java.util.Collections.shuffle(wall, random)
                val text = wall.take(13).joinToString("") { it.notation }
                check("S|$text")
                check("F|$text${wall[13].notation}|0|0|0|0")
            }
            // Special-form shanten and their full, possibly multi-form, discard results.
            for (text in listOf("19m19s19pESWNCFPN", "11223344556677m", "11123456789999p",
                    "258m369s147pEECCC", "2229999mSSWWFFF", "69m258s1pESWNCFP3m")) {
                check("S|$text")
                check("D|$text")
                check("F|$text|0|0|0|0")
            }
            // All six knitted permutations, with both concealed and exposed fourth groups.
            val knitted = listOf("147m258s369p", "147m369s258p", "258m147s369p",
                "258m369s147p", "369m147s258p", "369m258s147p")
            var special = 0
            while (special < 240) {
                val body = Tiles.parse(knitted[special % 6]).toMutableList()
                val group = if (random.nextBoolean()) {
                    val base = random.nextInt(3) * 9 + random.nextInt(7)
                    (0..2).map { Tile.entries[base + it] }
                } else {
                    val tile = Tile.entries[random.nextInt(34)]
                    List(3) { tile }
                }
                val pair = Tile.entries[random.nextInt(34)]
                val fixed = random.nextBoolean()
                if ((body + group + listOf(pair, pair)).groupingBy { it }.eachCount().values.any { it > 4 }) continue
                val prefix = if (fixed) "[${group.joinToString("") { it.notation }}1]" else ""
                if (!fixed) body.addAll(group)
                body.add(pair); body.add(pair)
                val win = body.removeAt(random.nextInt(body.size))
                val text = prefix + body.joinToString("") { it.notation }
                check("F|$text${win.notation}|${random.nextInt(32)}|${random.nextInt(4)}|${random.nextInt(4)}|0")
                if (special < 30) check("S|$text")
                special++
            }
            // Special families are rare in ordinary random hands. Exercise them directly.
            repeat(64) { index ->
                val counts = IntArray(34)
                val pairs = ArrayList<Tile>()
                while (pairs.size < 14) {
                    val tile = Tile.entries[random.nextInt(34)]
                    if (counts[tile.ordinal] == 4) continue
                    counts[tile.ordinal] += 2
                    pairs.add(tile); pairs.add(tile)
                }
                java.util.Collections.shuffle(pairs, random)
                val win = pairs.removeLast()
                val text = pairs.joinToString("") { it.notation }
                check("F|$text${win.notation}|${index % 2}|0|1|0")
                if (index < 8) check("S|$text")
            }
            for (sequence in knitted) {
                // Both honors-and-knitted fans, including lesser + knitted-straight.
                for (honorCount in 5..7) {
                    val tiles = Tiles.parse(sequence).take(14 - honorCount) + Tile.entries.filter { it.isHonor }.take(honorCount)
                    val text = tiles.joinToString("") { it.notation }
                    check("F|$text|0|0|1|0")
                    check("S|${tiles.dropLast(1).joinToString("") { it.notation }}")
                }
                // A fixed fourth group can be a concealed, exposed or promoted kong.
                for (offer in listOf("", "1", "5")) {
                    check("F|[EEEE$offer]${sequence}NN|5|0|1|0")
                }
            }
            for (pair in Tiles.parse("19m19s19pESWNCFP")) {
                check("F|19m19s19pESWNCFP${pair.notation}|1|0|1|0")
            }
        }
        println("C++ parity: $scores score tables, $analyses shanten/wait tables, $discards complete discard analyses")
    }
}
