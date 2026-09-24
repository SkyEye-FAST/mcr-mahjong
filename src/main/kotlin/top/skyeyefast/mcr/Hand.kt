package top.skyeyefast.mcr

import top.skyeyefast.mcr.internal.*
import java.util.Collections

@JvmSynthetic
internal fun <T : Any> immutableList(items: Collection<T>): List<T> = Collections.unmodifiableList(
    items.mapTo(ArrayList(items.size)) { requireNotNull(it) { "Collection elements must not be null" } },
)

@JvmSynthetic
internal fun <T : Any> immutableSet(items: Collection<T>): Set<T> =
    Collections.unmodifiableSet(LinkedHashSet(immutableList(items)))

/** Supplier of an exposed pung/kong, relative to the owner of the hand. */
enum class RelativePlayer(@get:JvmSynthetic internal val offer: Int) { LEFT(1), OPPOSITE(2), RIGHT(3) }
/** Position of the called tile within a chow supplied by the player on the left. */
enum class ChowPosition(@get:JvmSynthetic internal val offer: Int) { LOW(1), MIDDLE(2), HIGH(3) }
/** Round or seat wind. */
enum class Wind { EAST, SOUTH, WEST, NORTH }
/** How the separate winning tile was obtained. */
enum class WinMethod { DISCARD, SELF_DRAW }

/** Fixed melds, including concealed kongs. Concealed chows/pungs stay in [Hand.concealedTiles]. */
sealed class Meld {
    /** [middle] is the middle tile of the chow, not its lowest tile. */
    data class Chow @JvmOverloads constructor(
        val middle: Tile, val calledPosition: ChowPosition = ChowPosition.MIDDLE,
    ) : Meld() {
        init { require(middle.rank in 2..8) { "A chow needs a numbered middle tile of rank 2..8" } }
    }

    /** An exposed triplet; unexposed triplets remain in [Hand.concealedTiles]. */
    data class Pung @JvmOverloads constructor(
        val tile: Tile, val from: RelativePlayer = RelativePlayer.LEFT,
    ) : Meld()

    /** null [from] means concealed; [promoted] marks an added kong and requires a supplier. */
    data class Kong @JvmOverloads constructor(
        val tile: Tile, val from: RelativePlayer? = null, val promoted: Boolean = false,
    ) : Meld() {
        init { require(!promoted || from != null) { "An added kong must be exposed" } }
    }
}

@JvmSynthetic
internal fun Meld.packed(): Int = when (this) {
    is Meld.Chow -> pack(calledPosition.offer, CHOW, middle.code)
    is Meld.Pung -> pack(from.offer, PUNG, tile.code)
    is Meld.Kong -> pack(from?.offer ?: 0, KONG, tile.code) or (if (promoted) 0x4000 else 0)
}

/**
 * Hand before the drawn/winning tile: concealedTiles.size + 3 * melds.size == 13.
 * Kongs contain four physical tiles but count as one fixed meld. Inputs are copied.
 * Equality includes the order of the tiles and melds; it is not shape equivalence.
 * @throws IllegalArgumentException for invalid counts, a fifth copy, or null list elements from Java.
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

    @JvmSynthetic
    internal fun standing(): IntArray = concealedTiles.map { it.code }.toIntArray()
    @JvmSynthetic
    internal fun fixed(): IntArray = melds.map { it.packed() }.toIntArray()
    @JvmSynthetic
    internal fun physicalCounts(): IntArray {
        val result = mapPacks(fixed())
        for (t in concealedTiles) result[t.code]++
        return result
    }
    @JvmSynthetic
    internal fun validateAddition(tile: Tile) {
        require(remainingCopies(tile) > 0) { "Adding $tile would make a fifth physical copy" }
    }

    override fun equals(other: Any?): Boolean = other is Hand && concealedTiles == other.concealedTiles && melds == other.melds
    override fun hashCode(): Int = 31 * concealedTiles.hashCode() + melds.hashCode()
    override fun toString(): String = "Hand(concealedTiles=$concealedTiles, melds=$melds)"
}

/**
 * Flags have the same correction rules as the pinned C++ implementation.
 * @property flowerCount Number of flowers, from 0 through 8; excluded from the eight-point minimum.
 * @property lastTile Last physical copy (和绝张), not the last wall tile.
 * @property kongInvolved Replacement-tile self-draw or robbing a kong, according to [method].
 * @property wallLast Last wall tile: last-tile draw/claim according to [method].
 * @property initial Initial-hand interpretation; does not enable additional blessing fans.
 */
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
    @JvmSynthetic
    internal fun flags(): Int = (if (method == WinMethod.SELF_DRAW) 1 else 0) or
        (if (lastTile) 2 else 0) or (if (kongInvolved) 4 else 0) or
        (if (wallLast) 8 else 0) or (if (initial) 16 else 0)
}
