/* Kotlin port of mahjong-algorithm, Copyright (c) 2016-2027 Jeff Wang.
 * MIT licensed; see LICENSE and NOTICE. Keep the upstream packed representation. */
package top.skyeyefast.mcr.internal

internal const val TABLE_SIZE = 0x48
internal const val CHOW = 1
internal const val PUNG = 2
internal const val KONG = 3
internal const val PAIR = 4
internal val ALL_TILES = ((0x11..0x19) + (0x21..0x29) + (0x31..0x39) + (0x41..0x47)).toIntArray()
internal val ORPHANS = intArrayOf(0x11, 0x19, 0x21, 0x29, 0x31, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47)
internal val KNITTED = arrayOf(
    intArrayOf(0x11, 0x14, 0x17, 0x22, 0x25, 0x28, 0x33, 0x36, 0x39),
    intArrayOf(0x11, 0x14, 0x17, 0x23, 0x26, 0x29, 0x32, 0x35, 0x38),
    intArrayOf(0x12, 0x15, 0x18, 0x21, 0x24, 0x27, 0x33, 0x36, 0x39),
    intArrayOf(0x12, 0x15, 0x18, 0x23, 0x26, 0x29, 0x31, 0x34, 0x37),
    intArrayOf(0x13, 0x16, 0x19, 0x21, 0x24, 0x27, 0x32, 0x35, 0x38),
    intArrayOf(0x13, 0x16, 0x19, 0x22, 0x25, 0x28, 0x31, 0x34, 0x37),
)

internal fun rank(t: Int): Int = t and 15
internal fun suit(t: Int): Int = (t shr 4) and 15
internal fun numbered(t: Int): Boolean = t in 0x11..0x19 || t in 0x21..0x29 || t in 0x31..0x39
internal fun honor(t: Int): Boolean = t in 0x41..0x47
internal fun wind(t: Int): Boolean = t in 0x41..0x44
internal fun dragon(t: Int): Boolean = t in 0x45..0x47
internal fun terminal(t: Int): Boolean = (t and 0xC7) == 1
internal fun terminalOrHonor(t: Int): Boolean = terminal(t) || honor(t)
internal fun green(t: Int): Boolean = (0x0020000000AE0000L and (1L shl (t - 0x11))) != 0L
internal fun reversible(t: Int): Boolean = (0x0040019F01BA0000L and (1L shl (t - 0x11))) != 0L
internal fun sameSuit(a: Int, b: Int): Boolean = (a and 0xF0) == (b and 0xF0)
internal fun sameRank(a: Int, b: Int): Boolean = (a and 0xCF) == (b and 0xCF)
internal fun pack(offer: Int, type: Int, tile: Int): Int = (offer shl 12) or (type shl 8) or tile
internal fun packType(p: Int): Int = (p shr 8) and 15
internal fun packTile(p: Int): Int = p and 255
internal fun melded(p: Int): Boolean = (p and 0x3000) != 0
internal fun eigen(a: Int, b: Int, c: Int): Int = (a shl 16) or (b shl 8) or c
internal fun mapTiles(tiles: IntArray): IntArray = IntArray(TABLE_SIZE).also { table ->
    for (t in tiles) table[t]++
}
internal fun mapPacks(packs: IntArray): IntArray = IntArray(TABLE_SIZE).also { table ->
    for (p in packs) {
        val t = packTile(p)
        when (packType(p)) {
            CHOW -> { table[t - 1]++; table[t]++; table[t + 1]++ }
            PUNG -> table[t] += 3
            KONG -> table[t] += 4
            PAIR -> table[t] += 2
            else -> error("Invalid internal pack")
        }
    }
}
internal fun tableToTiles(table: IntArray): IntArray {
    val tiles = IntArray(ALL_TILES.sumOf { table[it] })
    var i = 0
    for (t in ALL_TILES) repeat(table[t]) { tiles[i++] = t }
    return tiles
}
internal fun BooleanArray.merge(other: BooleanArray) {
    for (i in indices) this[i] = this[i] || other[i]
}
