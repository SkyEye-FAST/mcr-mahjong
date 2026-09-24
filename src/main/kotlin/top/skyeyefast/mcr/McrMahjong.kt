package top.skyeyefast.mcr

import top.skyeyefast.mcr.internal.StandardMcr
import top.skyeyefast.mcr.internal.Shanten
import top.skyeyefast.mcr.internal.TABLE_SIZE

/** Honors-and-knitted includes both lesser (全不靠) and greater (七星不靠) forms. */
enum class HandForm {
    REGULAR, SEVEN_PAIRS, THIRTEEN_ORPHANS, HONORS_AND_KNITTED_TILES, KNITTED_STRAIGHT;

    @JvmSynthetic
    internal fun calculate(tiles: IntArray, useful: BooleanArray?): Int = when (this) {
        REGULAR -> Shanten.regular(tiles, useful)
        SEVEN_PAIRS -> Shanten.sevenPairs(tiles, useful)
        THIRTEEN_ORPHANS -> Shanten.thirteenOrphans(tiles, useful)
        HONORS_AND_KNITTED_TILES -> Shanten.honorsAndKnitted(tiles, useful)
        KNITTED_STRAIGHT -> Shanten.knittedStraight(tiles, useful)
    }
}

/**
 * A structural effective tile and its remaining physical copies. Zero-copy entries
 * are retained to preserve upstream behavior; they cannot actually be drawn.
 */
data class EffectiveTile(val tile: Tile, val remainingCopies: Int) {
    init { require(remainingCopies in 0..4) }
}

/** One applicable structural form and its effective tiles; shanten 0 means ready. */
class FormAnalysis private constructor(val form: HandForm, val shanten: Int, effectiveTiles: List<EffectiveTile>) {
    val effectiveTiles: List<EffectiveTile> = immutableList(effectiveTiles)
    val remainingCount: Int get() = effectiveTiles.sumOf { it.remainingCopies }

    internal companion object {
        @JvmSynthetic
        fun create(form: HandForm, shanten: Int, effectiveTiles: List<EffectiveTile>): FormAnalysis =
            FormAnalysis(form, shanten, effectiveTiles)
    }
}

/** Only applicable forms are present; unavailable special forms are not given fake distances. */
class HandAnalysis private constructor(forms: List<FormAnalysis>) {
    val forms: List<FormAnalysis> = immutableList(forms)
    val shanten: Int = this.forms.minOf { it.shanten }
    val effectiveTiles: List<EffectiveTile> = immutableList(
        this.forms.filter { it.shanten == shanten }.flatMap { it.effectiveTiles }.distinctBy { it.tile }.sortedBy { it.tile.ordinal },
    )
    val remainingCount: Int get() = effectiveTiles.sumOf { it.remainingCopies }

    internal companion object {
        @JvmSynthetic
        fun create(forms: List<FormAnalysis>): HandAnalysis = HandAnalysis(forms)
    }
}

/**
 * One distinct discard, in upstream order (drawn tile first, then canonical tile order).
 * [analysis] describes the remaining 13-tile hand, so a ready hand has shanten 0.
 * [completesForms] identifies upstream's -1 correction: adding the discarded tile
 * back completes those forms. This distinguishes winning now from shanten after discarding.
 */
class DiscardAnalysis private constructor(
    val discard: Tile, val analysis: HandAnalysis, completesForms: Set<HandForm>,
) {
    val completesForms: Set<HandForm> = immutableSet(completesForms)
    val shanten: Int get() = analysis.shanten
    val effectiveTiles: List<EffectiveTile> get() = analysis.effectiveTiles
    val remainingCount: Int get() = analysis.remainingCount

    internal companion object {
        @JvmSynthetic
        fun create(discard: Tile, analysis: HandAnalysis, completesForms: Set<HandForm>): DiscardAnalysis =
            DiscardAnalysis(discard, analysis, completesForms)
    }
}

/**
 * Stateless, thread-safe entry points. Returned collections and retained inputs are immutable snapshots.
 * Callers must not mutate a supplied collection concurrently with the call reading it.
 */
object McrMahjong {
    /** Fixed public scoring contract; see COMPATIBILITY.md for its source and release-review limits. */
    const val SCORING_PROFILE: String = "wmo-2014-zh"

    /**
     * Calculates structural shanten and effective tiles for a hand before drawing.
     * [visibleTiles] are additional known tiles, excluding this hand and its melds.
     * They affect remaining counts only, never the upstream structural shanten.
     */
    @JvmStatic
    @JvmOverloads
    fun analyze(hand: Hand, visibleTiles: List<Tile> = emptyList()): HandAnalysis {
        val counts = knownCounts(hand, visibleTiles)
        return analyzeStanding(hand.standing(), counts)
    }

    /** Minimum shanten across all applicable upstream forms; 0 means structurally ready. */
    @JvmStatic
    fun shanten(hand: Hand): Int {
        val tiles = hand.standing()
        return HandForm.entries.minOf { it.calculate(tiles, null) }
    }

    /** Structural winning tile kinds, including theoretical zero-copy waits as upstream does. */
    @JvmStatic
    fun waitingTiles(hand: Hand): Set<Tile> {
        val waiting = Shanten.waiting(hand.standing())
        return immutableSet(Tile.entries.filter { waiting[it.code] })
    }

    /**
     * Returns all distinct discards after drawing [drawnTile]. The drawn tile and
     * the proposed discard remain known when estimating available copies.
     * [visibleTiles] excludes this hand, its melds and [drawnTile].
     */
    @JvmStatic
    @JvmOverloads
    fun discards(hand: Hand, drawnTile: Tile, visibleTiles: List<Tile> = emptyList()): List<DiscardAnalysis> {
        hand.validateAddition(drawnTile)
        val counts = knownCounts(hand, visibleTiles + drawnTile)
        val choices = listOf(drawnTile) + hand.concealedTiles.distinct().filter { it != drawnTile }.sortedBy { it.ordinal }
        return immutableList(choices.map { discarded ->
            val remaining = ArrayList(hand.concealedTiles)
            if (discarded != drawnTile) { remaining.remove(discarded); remaining.add(drawnTile) }
            val analysis = analyzeStanding(remaining.map { it.code }.toIntArray(), counts)
            val completes = analysis.forms.filter { form ->
                form.shanten == 0 && form.effectiveTiles.any { it.tile == discarded }
            }.mapTo(linkedSetOf()) { it.form }
            DiscardAnalysis.create(discarded, analysis, completes)
        })
    }

    /**
     * Scores using the WMO 2014 Chinese MCR edition pinned in COMPATIBILITY.md,
     * not the upstream's default rule extensions. Invalid inputs throw; a valid
     * non-winning shape returns [ScoreResult.NotWinning].
     */
    @JvmStatic
    @JvmOverloads
    fun score(hand: Hand, winningTile: Tile, context: WinContext = WinContext()): ScoreResult {
        hand.validateAddition(winningTile)
        return StandardMcr.calculate(hand, winningTile, context)
    }

    private fun knownCounts(hand: Hand, extra: List<Tile>): IntArray {
        val counts = hand.physicalCounts()
        for (tile in immutableList(extra)) require(++counts[tile.code] <= 4) { "Known tiles contain more than four copies of $tile" }
        return counts
    }

    private fun analyzeStanding(tiles: IntArray, counts: IntArray): HandAnalysis {
        val forms = ArrayList<FormAnalysis>()
        for (form in HandForm.entries) {
            val useful = BooleanArray(TABLE_SIZE)
            val shanten = form.calculate(tiles, useful)
            if (shanten == Int.MAX_VALUE) continue
            forms.add(FormAnalysis.create(form, shanten, Tile.entries.filter { useful[it.code] }.map {
                EffectiveTile(it, 4 - counts[it.code])
            }))
        }
        return HandAnalysis.create(forms)
    }
}
