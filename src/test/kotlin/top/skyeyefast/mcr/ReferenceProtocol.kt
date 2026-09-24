package top.skyeyefast.mcr

import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

/** Test-only support for the pinned upstream's bracket notation and oracle protocol. */
internal object ReferenceProtocol {
    data class Parsed(val hand: Hand, val drawn: Tile?)

    fun parse(text: String): Parsed {
        val melds = ArrayList<Meld>()
        val concealed = Regex("\\[([^]]+)]").replace(text) { match ->
            var body = match.groupValues[1]
            val offer = if (body.last().isDigit()) body.last().digitToInt().also { body = body.dropLast(1) } else null
            val tiles = Tiles.parse(body).sortedBy { it.ordinal }
            when {
                tiles.size == 4 && tiles.distinct().size == 1 -> {
                    val source = offer ?: 0
                    melds.add(Meld.Kong(tiles[0], if (source == 0) null else RelativePlayer.entries[(source and 3) - 1], source > 4))
                }
                tiles.size == 3 && tiles.distinct().size == 1 -> melds.add(Meld.Pung(tiles[0], RelativePlayer.entries[(offer ?: 1) - 1]))
                tiles.size == 3 -> melds.add(Meld.Chow(tiles[1], ChowPosition.entries[(offer ?: 1) - 1]))
                else -> error("Unsupported reference meld: ${match.value}")
            }
            ""
        }
        val tiles = Tiles.parse(concealed)
        val drawn = if (tiles.size + 3 * melds.size == 14) tiles.last() else null
        return Parsed(Hand(if (drawn == null) tiles else tiles.dropLast(1), melds), drawn)
    }

    fun context(fields: List<String>): WinContext {
        val flags = fields[2].toInt()
        return WinContext(
            method = if ((flags and 1) != 0) WinMethod.SELF_DRAW else WinMethod.DISCARD,
            prevalentWind = Wind.entries[fields[3].toInt()], seatWind = Wind.entries[fields[4].toInt()],
            flowerCount = fields[5].toInt(), lastTile = (flags and 2) != 0,
            kongInvolved = (flags and 4) != 0, wallLast = (flags and 8) != 0, initial = (flags and 16) != 0,
        )
    }

    private fun bits(tiles: Collection<Tile>): String = Tile.entries.joinToString("") { if (it in tiles) "1" else "0" }

    fun calculate(request: String): String {
        val fields = request.split('|')
        val (hand, drawn) = parse(fields[1])
        return when (fields[0]) {
            "F" -> {
                val result = McrMahjong.score(hand, requireNotNull(drawn), context(fields))
                val counts = Fan.entries.map { if (result is ScoreResult.Winning) result.count(it) else 0 }
                val total = if (result is ScoreResult.Winning) result.totalFan else -3
                (listOf(total) + counts).joinToString(",")
            }
            "S" -> {
                val result = McrMahjong.analyze(hand)
                check(McrMahjong.shanten(hand) == result.shanten) { "Shanten-only path differs: $request" }
                HandForm.entries.joinToString(";") { form ->
                    val analysis = result.forms.firstOrNull { it.form == form }
                    "${analysis?.shanten ?: Int.MAX_VALUE},${bits(analysis?.effectiveTiles?.map { it.tile } ?: emptyList())}"
                } + ";" + bits(McrMahjong.waitingTiles(hand))
            }
            "D" -> McrMahjong.discards(hand, requireNotNull(drawn)).flatMap { discard ->
                discard.analysis.forms.map { form ->
                    val shanten = if (form.form in discard.completesForms) -1 else form.shanten
                    "${discard.discard.code},${1 shl form.form.ordinal},$shanten,${bits(form.effectiveTiles.map { it.tile })}"
                }
            }.joinToString(";")
            else -> error("Unknown request: $request")
        }
    }

    /** Sparse representation still compares the entire table, including absent fans. */
    fun compactScore(response: String): String {
        val numbers = response.split(',').map { it.toInt() }
        require(numbers.size == Fan.entries.size + 1)
        return numbers[0].toString() + ";" + Fan.entries.filter { numbers[it.ordinal + 1] != 0 }.joinToString(",") {
            "${it.name}=${numbers[it.ordinal + 1]}"
        }
    }
}

internal class Oracle(path: String) : Closeable {
    private val process = ProcessBuilder(path).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    private val input = process.outputStream.bufferedWriter()
    private val output = process.inputStream.bufferedReader()
    private val reader = Executors.newSingleThreadExecutor { job ->
        Thread(job, "mcr-oracle-io").apply { isDaemon = true }
    }

    fun query(request: String): String {
        val response = reader.submit<String> {
            input.write(request)
            input.newLine()
            input.flush()
            requireNotNull(output.readLine()) { "Native oracle exited while handling: $request" }
        }
        return try {
            response.get(5, TimeUnit.SECONDS)
        } catch (failure: Exception) {
            process.destroyForcibly()
            response.cancel(true)
            throw IllegalStateException("Native oracle failed or timed out: $request", failure)
        }
    }

    override fun close() {
        try {
            input.close()
            check(process.waitFor(3, TimeUnit.SECONDS)) { "Native oracle did not exit" }
            check(process.exitValue() == 0) { "Native oracle exit code: ${process.exitValue()}" }
        } finally {
            if (process.isAlive) process.destroyForcibly()
            reader.shutdownNow()
            output.close()
        }
    }
}
