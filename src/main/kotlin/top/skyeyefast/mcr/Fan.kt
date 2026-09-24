/* Names and ordering derived from fan_calculator.h; public values follow COMPATIBILITY.md.
 * Copyright (c) 2016-2027 Jeff Wang. MIT license; see LICENSE and NOTICE. */
package top.skyeyefast.mcr

/** The 81 fan identifiers of the pinned WMO 2014 Chinese MCR edition; see COMPATIBILITY.md. */
enum class Fan(val points: Int, val chineseName: String) {
    BIG_FOUR_WINDS(88, "大四喜"), BIG_THREE_DRAGONS(88, "大三元"), ALL_GREEN(88, "绿一色"),
    NINE_GATES(88, "九莲宝灯"), FOUR_KONGS(88, "四杠"), SEVEN_SHIFTED_PAIRS(88, "连七对"), THIRTEEN_ORPHANS(88, "十三幺"),
    ALL_TERMINALS(64, "清幺九"), LITTLE_FOUR_WINDS(64, "小四喜"), LITTLE_THREE_DRAGONS(64, "小三元"),
    ALL_HONORS(64, "字一色"), FOUR_CONCEALED_PUNGS(64, "四暗刻"), PURE_TERMINAL_CHOWS(64, "一色双龙会"),
    QUADRUPLE_CHOW(48, "一色四同顺"), FOUR_PURE_SHIFTED_PUNGS(48, "一色四节高"),
    FOUR_PURE_SHIFTED_CHOWS(32, "一色四步高"), THREE_KONGS(32, "三杠"), ALL_TERMINALS_AND_HONORS(32, "混幺九"),
    SEVEN_PAIRS(24, "七对"), GREATER_HONORS_AND_KNITTED_TILES(24, "七星不靠"), ALL_EVEN_PUNGS(24, "全双刻"),
    FULL_FLUSH(24, "清一色"), PURE_TRIPLE_CHOW(24, "一色三同顺"), PURE_SHIFTED_PUNGS(24, "一色三节高"),
    UPPER_TILES(24, "全大"), MIDDLE_TILES(24, "全中"), LOWER_TILES(24, "全小"),
    PURE_STRAIGHT(16, "清龙"), THREE_SUITED_TERMINAL_CHOWS(16, "三色双龙会"), PURE_SHIFTED_CHOWS(16, "一色三步高"),
    ALL_FIVE(16, "全带五"), TRIPLE_PUNG(16, "三同刻"), THREE_CONCEALED_PUNGS(16, "三暗刻"),
    LESSER_HONORS_AND_KNITTED_TILES(12, "全不靠"), KNITTED_STRAIGHT(12, "组合龙"),
    UPPER_FOUR(12, "大于五"), LOWER_FOUR(12, "小于五"), BIG_THREE_WINDS(12, "三风刻"),
    MIXED_STRAIGHT(8, "花龙"), REVERSIBLE_TILES(8, "推不倒"), MIXED_TRIPLE_CHOW(8, "三色三同顺"),
    MIXED_SHIFTED_PUNGS(8, "三色三节高"), CHICKEN_HAND(8, "无番和"), LAST_TILE_DRAW(8, "妙手回春"),
    LAST_TILE_CLAIM(8, "海底捞月"), OUT_WITH_REPLACEMENT_TILE(8, "杠上开花"), ROBBING_THE_KONG(8, "抢杠和"),
    ALL_PUNGS(6, "碰碰和"), HALF_FLUSH(6, "混一色"), MIXED_SHIFTED_CHOWS(6, "三色三步高"),
    ALL_TYPES(6, "五门齐"), MELDED_HAND(6, "全求人"), TWO_CONCEALED_KONGS(8, "双暗杠"), TWO_DRAGONS_PUNGS(6, "双箭刻"),
    OUTSIDE_HAND(4, "全带幺"), FULLY_CONCEALED_HAND(4, "不求人"), TWO_MELDED_KONGS(4, "双明杠"), LAST_TILE(4, "和绝张"),
    DRAGON_PUNG(2, "箭刻"), PREVALENT_WIND(2, "圈风刻"), SEAT_WIND(2, "门风刻"), CONCEALED_HAND(2, "门前清"),
    ALL_CHOWS(2, "平和"), TILE_HOG(2, "四归一"), DOUBLE_PUNG(2, "双同刻"), TWO_CONCEALED_PUNGS(2, "双暗刻"),
    CONCEALED_KONG(2, "暗杠"), ALL_SIMPLES(2, "断幺"),
    PURE_DOUBLE_CHOW(1, "一般高"), MIXED_DOUBLE_CHOW(1, "喜相逢"), SHORT_STRAIGHT(1, "连六"), TWO_TERMINAL_CHOWS(1, "老少副"),
    PUNG_OF_TERMINALS_OR_HONORS(1, "幺九刻"), MELDED_KONG(1, "明杠"), ONE_VOIDED_SUIT(1, "缺一门"), NO_HONORS(1, "无字"),
    EDGE_WAIT(1, "边张"), CLOSED_WAIT(1, "嵌张"), SINGLE_WAIT(1, "单钓"), SELF_DRAWN(1, "自摸"),
    FLOWER_TILES(1, "花牌");

    @get:JvmSynthetic
    internal val index: Int get() = ordinal + 1
}

/**
 * A positive number of occurrences of a standard fan.
 * [isMixedKongPair] identifies the six-point exception within the rulebook's
 * Two Melded Kongs entry: one concealed and one melded kong, not two melded kongs.
 * It is not an extra fan and does not additionally award the individual kongs.
 * [points] is the awarded subtotal; it can differ from `fan.points * count` in that case.
 */
data class FanCount @JvmOverloads constructor(
    val fan: Fan, val count: Int, val isMixedKongPair: Boolean = false,
) {
    init {
        require(count in 1..Int.MAX_VALUE / fan.points) { "Fan count must be positive and its points must fit in Int" }
        require(!isMixedKongPair || (fan == Fan.TWO_MELDED_KONGS && count == 1)) {
            "The mixed-kong exception applies only to one Two Melded Kongs entry"
        }
    }
    val points: Int get() = if (isMixedKongPair) 6 else fan.points * count
}

/** Invalid inputs throw IllegalArgumentException; a valid non-winning shape returns [NotWinning]. */
sealed class ScoreResult {
    /** Valid physical input that does not form a winning shape. */
    data object NotWinning : ScoreResult()

    /** A structural win; use [meetsMinimum] separately to check non-flower qualification. */
    class Winning private constructor(fans: List<FanCount>) : ScoreResult() {
        val fans: List<FanCount> = immutableList(fans)
        val totalFan: Int = this.fans.sumOf { it.points }
        val nonFlowerFan: Int = totalFan - this.fans.filter { it.fan == Fan.FLOWER_TILES }.sumOf { it.points }
        /** Structural wins below eight non-flower points remain available to callers. */
        val meetsMinimum: Boolean get() = nonFlowerFan >= 8
        fun count(fan: Fan): Int = fans.firstOrNull { it.fan == fan }?.count ?: 0
        override fun toString(): String = "Winning(totalFan=$totalFan, fans=$fans)"

        internal companion object {
            @JvmSynthetic
            fun create(fans: List<FanCount>): Winning = Winning(fans)
        }
    }
}
