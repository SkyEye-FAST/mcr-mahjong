package top.skyeyefast.mcr

/** The 34 non-flower tile types. M = characters, S = bamboo, P = dots. */
enum class Tile(internal val code: Int, val notation: String) {
    M1(0x11, "1m"), M2(0x12, "2m"), M3(0x13, "3m"), M4(0x14, "4m"), M5(0x15, "5m"),
    M6(0x16, "6m"), M7(0x17, "7m"), M8(0x18, "8m"), M9(0x19, "9m"),
    S1(0x21, "1s"), S2(0x22, "2s"), S3(0x23, "3s"), S4(0x24, "4s"), S5(0x25, "5s"),
    S6(0x26, "6s"), S7(0x27, "7s"), S8(0x28, "8s"), S9(0x29, "9s"),
    P1(0x31, "1p"), P2(0x32, "2p"), P3(0x33, "3p"), P4(0x34, "4p"), P5(0x35, "5p"),
    P6(0x36, "6p"), P7(0x37, "7p"), P8(0x38, "8p"), P9(0x39, "9p"),
    EAST(0x41, "E"), SOUTH(0x42, "S"), WEST(0x43, "W"), NORTH(0x44, "N"),
    RED(0x45, "C"), GREEN(0x46, "F"), WHITE(0x47, "P");

    val isHonor: Boolean get() = code >= 0x41
    /** 1..9 for numbered tiles, null for honors. */
    val rank: Int? get() = if (isHonor) null else code and 15

    override fun toString(): String = notation

    companion object {
        private val byCode: Map<Int, Tile> = entries.associateBy { it.code }
        internal fun fromCode(code: Int): Tile = requireNotNull(byCode[code]) { "Invalid tile code: $code" }

        @JvmStatic
        fun parse(notation: String): Tile = Tiles.parse(notation).singleOrNull()
            ?: throw IllegalArgumentException("Expected one tile: $notation")
    }
}

/** Small, strict notation helper: 123m456s789pESWNCFP; whitespace is ignored. */
object Tiles {
    @JvmStatic
    fun parse(notation: String): List<Tile> {
        val result = ArrayList<Tile>()
        val digits = ArrayList<Int>()
        for (c in notation) {
            when {
                c.isWhitespace() -> Unit
                c in '1'..'9' -> digits.add(c - '0')
                c == 'm' || c == 's' || c == 'p' -> {
                    require(digits.isNotEmpty()) { "Suit without ranks in: $notation" }
                    val suit = when (c) { 'm' -> 1; 's' -> 2; else -> 3 }
                    for (r in digits) result.add(Tile.fromCode((suit shl 4) or r))
                    digits.clear()
                }
                c in "ESWNCFP" -> {
                    require(digits.isEmpty()) { "Missing suit in: $notation" }
                    result.add(Tile.fromCode(0x41 + "ESWNCFP".indexOf(c)))
                }
                else -> throw IllegalArgumentException("Invalid tile notation character: $c")
            }
        }
        require(digits.isEmpty()) { "Missing suit in: $notation" }
        return immutableList(result)
    }
}
