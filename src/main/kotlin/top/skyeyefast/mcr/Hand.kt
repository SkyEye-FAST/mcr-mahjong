package top.skyeyefast.mcr

import top.skyeyefast.mcr.internal.*
import java.util.Collections

internal fun <T> immutableList(items: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(items))
internal fun <T> immutableSet(items: Collection<T>): Set<T> = Collections.unmodifiableSet(LinkedHashSet(items))

enum class RelativePlayer(internal val offer: Int) { LEFT(1), OPPOSITE(2), RIGHT(3) }
enum class ChowPosition(internal val offer: Int) { LOW(1), MIDDLE(2), HIGH(3) }
enum class Wind { EAST, SOUTH, WEST, NORTH }
enum class WinMethod { DISCARD, SELF_DRAW }

/** Fixed melds, including concealed kongs. Concealed chows/pungs stay in [Hand.concealedTiles]. */
sealed class Meld {
    internal abstract fun packed(): Int

    /** [middle] is the middle tile of the chow, not its lowest tile. */
    data class Chow @JvmOverloads constructor(
        val middle: Tile, val calledPosition: ChowPosition = ChowPosition.MIDDLE,
    ) : Meld() {
        init { require(middle.rank in 2..8) { "A chow needs a numbered middle tile of rank 2..8" } }
        override fun packed(): Int = pack(calledPosition.offer, CHOW, middle.code)
    }

    data class Pung @JvmOverloads constructor(
        val tile: Tile, val from: RelativePlayer = RelativePlayer.LEFT,
    ) : Meld() {
        override fun packed(): Int = pack(from.offer, PUNG, tile.code)
    }

    /** null [from] means concealed; [promoted] marks an added kong and requires a supplier. */
    data class Kong @JvmOverloads constructor(
        val tile: Tile, val from: RelativePlayer? = null, val promoted: Boolean = false,
    ) : Meld() {
        init { require(!promoted || from != null) { "An added kong must be exposed" } }
        override fun packed(): Int = pack(from?.offer ?: 0, KONG, tile.code) or (if (promoted) 0x4000 else 0)
    }
}

/**
 * Hand before the drawn/winning tile: concealedTiles.size + 3 * melds.size == 13.
 * Kongs contain four physical tiles but count as one fixed meld. Inputs are copied.
 */
class Hand @JvmOverloads constructor(concealedTiles: List<Tile>, melds: List<Meld> = emptyList()) {
    val concealedTiles: List<Tile> = immutableList(concealedTiles)
    val melds: List<Meld> = immutableList(melds)

    init {
        require(this.melds.size <= 4 && this.concealedTiles.size + 3 * this.melds.size == 13) {
            "Expected concealed tile count + 3 * fixed meld count == 13"
        }
        require(physicalCounts().all { it <= 4 }) { "A tile occurs more than four times, including fixed melds" }
    }

    /** Remaining copies after counting this hand only; this is not a wall-visibility estimate. */
    fun remainingCopies(tile: Tile): Int = 4 - physicalCounts()[tile.code]

    internal fun standing(): IntArray = concealedTiles.map { it.code }.toIntArray()
    internal fun fixed(): IntArray = melds.map { it.packed() }.toIntArray()
    internal fun physicalCounts(): IntArray {
        val result = mapPacks(fixed())
        for (t in concealedTiles) result[t.code]++
        return result
    }
    internal fun validateAddition(tile: Tile) {
        require(remainingCopies(tile) > 0) { "Adding $tile would make a fifth physical copy" }
    }

    override fun equals(other: Any?): Boolean = other is Hand && concealedTiles == other.concealedTiles && melds == other.melds
    override fun hashCode(): Int = 31 * concealedTiles.hashCode() + melds.hashCode()
    override fun toString(): String = "Hand(concealedTiles=$concealedTiles, melds=$melds)"
}

/** Flags have the same correction rules as the pinned C++ implementation. */
data class WinContext @JvmOverloads constructor(
    val method: WinMethod = WinMethod.DISCARD,
    val prevalentWind: Wind = Wind.EAST,
    val seatWind: Wind = Wind.EAST,
    val flowerCount: Int = 0,
    val lastTile: Boolean = false,
    val kongInvolved: Boolean = false,
    val wallLast: Boolean = false,
    val initial: Boolean = false,
) {
    init { require(flowerCount in 0..8) { "Flower count must be in 0..8" } }
    internal fun flags(): Int = (if (method == WinMethod.SELF_DRAW) 1 else 0) or
        (if (lastTile) 2 else 0) or (if (kongInvolved) 4 else 0) or
        (if (wallLast) 8 else 0) or (if (initial) 16 else 0)
}
