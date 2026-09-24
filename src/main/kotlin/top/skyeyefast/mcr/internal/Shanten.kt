/* Faithful port of upstream shanten.cpp at the revision in NOTICE.
 * Copyright (c) 2016-2027 Jeff Wang. MIT license; see LICENSE. */
package top.skyeyefast.mcr.internal

import kotlin.math.min

internal object Shanten {
    // Keep eigen ordering, branch order and the upstream taatsu pruning intact.
    private fun regularRec(
        table: IntArray, hasPair: Boolean, packs: Int, partners: Int,
        fixed: Int, packEigen: Int, partnerEigen: Int,
    ): Int {
        if (fixed == 4) return if (ALL_TILES.any { table[it] > 1 }) -1 else 0
        if (packs == 4) return if (hasPair) -1 else 0
        val need = 4 - packs - partners
        val maximum = if (need > 0) partners + need * 2 - (if (hasPair) 1 else 0)
            else (if (hasPair) 3 else 4) - packs
        var result = maximum
        if (packs + partners > 4) return maximum
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (!hasPair && table[t] > 1) {
                table[t] -= 2
                result = min(result, regularRec(table, true, packs, partners, fixed, packEigen, partnerEigen))
                table[t] += 2
            }
            if (table[t] > 2) {
                val e = eigen(t, t, t)
                if (e > packEigen) {
                    table[t] -= 3
                    result = min(result, regularRec(table, hasPair, packs + 1, partners, fixed, e, partnerEigen))
                    table[t] += 3
                }
            }
            val isNumbered = numbered(t)
            if (isNumbered && rank(t) < 8 && table[t + 1] > 0 && table[t + 2] > 0) {
                val e = eigen(t, t + 1, t + 2)
                if (e >= packEigen) {
                    table[t]--; table[t + 1]--; table[t + 2]--
                    result = min(result, regularRec(table, hasPair, packs + 1, partners, fixed, e, partnerEigen))
                    table[t]++; table[t + 1]++; table[t + 2]++
                }
            }
            if (result < maximum) continue
            if (table[t] > 1) {
                val e = eigen(t, t, 0)
                if (e > partnerEigen) {
                    table[t] -= 2
                    result = min(result, regularRec(table, hasPair, packs, partners + 1, fixed, packEigen, e))
                    table[t] += 2
                }
            }
            if (isNumbered) {
                if (rank(t) < 9 && table[t + 1] > 0) {
                    val e = eigen(t, t + 1, 0)
                    if (e >= partnerEigen) {
                        table[t]--; table[t + 1]--
                        result = min(result, regularRec(table, hasPair, packs, partners + 1, fixed, packEigen, e))
                        table[t]++; table[t + 1]++
                    }
                }
                if (rank(t) < 8 && table[t + 2] > 0) {
                    val e = eigen(t, t + 2, 0)
                    if (e >= partnerEigen) {
                        table[t]--; table[t + 2]--
                        result = min(result, regularRec(table, hasPair, packs, partners + 1, fixed, packEigen, e))
                        table[t]++; table[t + 2]++
                    }
                }
            }
        }
        return result
    }

    private fun hasPartner(table: IntArray, t: Int): Boolean {
        val r = rank(t)
        return (r < 9 && table[t + 1] > 0) || (r < 8 && table[t + 2] > 0) ||
            (r > 1 && table[t - 1] > 0) || (r > 2 && table[t - 2] > 0)
    }

    private fun regularFromTable(table: IntArray, fixed: Int, useful: BooleanArray?): Int {
        val result = regularRec(table, false, fixed, 0, fixed, 0, 0)
        if (useful == null) return result
        for (t in ALL_TILES) {
            // Upstream deliberately permits structural fifth-copy waits at shanten 0.
            if (table[t] == 4 && result > 0) continue
            if (table[t] == 0 && (honor(t) || !hasPartner(table, t))) continue
            table[t]++
            if (regularRec(table, false, fixed, 0, fixed, 0, 0) < result) useful[t] = true
            table[t]--
        }
        return result
    }

    fun regular(tiles: IntArray, useful: BooleanArray? = null): Int {
        if (tiles.size !in intArrayOf(1, 4, 7, 10, 13)) return Int.MAX_VALUE
        useful?.fill(false)
        return regularFromTable(mapTiles(tiles), (13 - tiles.size) / 3, useful)
    }

    private fun wait1(table: IntArray, waiting: BooleanArray?): Boolean {
        for (t in ALL_TILES) {
            if (table[t] != 1) continue
            table[t] = 0
            val success = table.all { it == 0 }
            table[t] = 1
            if (success) {
                if (waiting != null) waiting[t] = true
                return true
            }
        }
        return false
    }

    private fun wait2(table: IntArray, waiting: BooleanArray?): Boolean {
        var result = false
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 1) {
                if (waiting == null) return true
                waiting[t] = true; result = true
                continue
            }
            if (numbered(t)) {
                val r = rank(t)
                if (r > 1 && table[t - 1] > 0) {
                    if (waiting == null) return true
                    if (r < 9) waiting[t + 1] = true
                    if (r > 2) waiting[t - 2] = true
                    result = true
                    continue
                }
                if (r > 2 && table[t - 2] > 0) {
                    if (waiting == null) return true
                    waiting[t - 1] = true; result = true
                }
            }
        }
        return result
    }

    private fun wait4(table: IntArray, waiting: BooleanArray?): Boolean {
        var result = false
        for (t in ALL_TILES) {
            if (table[t] < 2) continue
            table[t] -= 2
            if (wait2(table, waiting)) result = true
            table[t] += 2
            if (result && waiting == null) return true
        }
        return result
    }

    private fun waitRec(table: IntArray, left: Int, previous: Int, waiting: BooleanArray?): Boolean {
        if (left == 1) return wait1(table, waiting)
        var result = false
        if (left == 4) {
            result = wait4(table, waiting)
            if (result && waiting == null) return true
        }
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 2) {
                val e = eigen(t, t, t)
                if (e > previous) {
                    table[t] -= 3
                    if (waitRec(table, left - 3, e, waiting)) result = true
                    table[t] += 3
                    if (result && waiting == null) return true
                }
            }
            if (numbered(t) && rank(t) < 8 && table[t + 1] > 0 && table[t + 2] > 0) {
                val e = eigen(t, t + 1, t + 2)
                if (e >= previous) {
                    table[t]--; table[t + 1]--; table[t + 2]--
                    if (waitRec(table, left - 3, e, waiting)) result = true
                    table[t]++; table[t + 1]++; table[t + 2]++
                    if (result && waiting == null) return true
                }
            }
        }
        return result
    }

    fun regularWait(tiles: IntArray, waiting: BooleanArray? = null): Boolean {
        waiting?.fill(false)
        return waitRec(mapTiles(tiles), tiles.size, 0, waiting)
    }

    private fun winRec(table: IntArray, left: Int, previous: Int): Boolean {
        if (left == 2) {
            val first = table.indexOfFirst { it > 0 }
            return first >= 0 && table[first] == 2 && (first + 1 until table.size).none { table[it] > 0 }
        }
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 2) {
                val e = eigen(t, t, t)
                if (e > previous) {
                    table[t] -= 3
                    val result = winRec(table, left - 3, e)
                    table[t] += 3
                    if (result) return true
                }
            }
            if (numbered(t) && rank(t) < 8 && table[t + 1] > 0 && table[t + 2] > 0) {
                val e = eigen(t, t + 1, t + 2)
                if (e >= previous) {
                    table[t]--; table[t + 1]--; table[t + 2]--
                    val result = winRec(table, left - 3, e)
                    table[t]++; table[t + 1]++; table[t + 2]++
                    if (result) return true
                }
            }
        }
        return false
    }

    fun regularWin(tiles: IntArray, tile: Int): Boolean {
        val table = mapTiles(tiles)
        table[tile]++
        return winRec(table, tiles.size + 1, 0)
    }

    fun sevenPairs(tiles: IntArray, useful: BooleanArray? = null): Int {
        if (tiles.size != 13) return Int.MAX_VALUE
        var pairs = 0
        val table = IntArray(TABLE_SIZE)
        for (t in tiles) {
            table[t]++
            if (table[t] == 2) { pairs++; table[t] = 0 }
        }
        if (useful != null) for (i in table.indices) useful[i] = table[i] != 0
        return 6 - pairs
    }

    fun thirteenOrphans(tiles: IntArray, useful: BooleanArray? = null): Int {
        if (tiles.size != 13) return Int.MAX_VALUE
        val table = mapTiles(tiles)
        val hasPair = ORPHANS.any { table[it] > 1 }
        val count = ORPHANS.count { table[it] > 0 }
        if (useful != null) {
            useful.fill(false)
            for (t in ORPHANS) useful[t] = !hasPair || table[t] == 0
        }
        return (if (hasPair) 12 else 13) - count
    }

    private fun specified(table: IntArray, main: IntArray, fixed: Int, useful: BooleanArray?): Int {
        val temp = table.copyOf()
        var existing = 0
        useful?.fill(false)
        for (t in main) {
            if (table[t] > 0) { existing++; temp[t]-- }
            else if (useful != null) useful[t] = true
        }
        return main.size - existing + regularFromTable(temp, fixed + main.size / 3, useful)
    }

    fun knittedStraight(tiles: IntArray, useful: BooleanArray? = null): Int {
        if (tiles.size != 13 && tiles.size != 10) return Int.MAX_VALUE
        val table = mapTiles(tiles)
        var result = Int.MAX_VALUE
        useful?.fill(false)
        val temp = if (useful == null) null else BooleanArray(TABLE_SIZE)
        for (seq in KNITTED) {
            val st = specified(table, seq, (13 - tiles.size) / 3, temp)
            if (st < result) {
                result = st
                if (useful != null) temp!!.copyInto(useful)
            } else if (st == result && useful != null) useful.merge(temp!!)
        }
        return result
    }

    fun knittedWait(tiles: IntArray, waiting: BooleanArray? = null): Boolean {
        if (tiles.size != 13 && tiles.size != 10) return false
        val table = mapTiles(tiles)
        val seq = KNITTED.firstOrNull { row -> row.count { table[it] == 0 } < 2 } ?: return false
        val missing = seq.filter { table[it] == 0 }
        waiting?.fill(false)
        val temp = table.copyOf()
        for (t in seq) if (temp[t] > 0) temp[t]--
        if (missing.size == 1) {
            if (winRec(temp, if (tiles.size == 10) 2 else 5, 0)) {
                if (waiting != null) waiting[missing[0]] = true
                return true
            }
        } else if (missing.isEmpty()) {
            return if (tiles.size == 10) wait1(temp, waiting) else waitRec(temp, 4, 0, waiting)
        }
        return false
    }

    fun honorsAndKnitted(tiles: IntArray, useful: BooleanArray? = null): Int {
        if (tiles.size != 13) return Int.MAX_VALUE
        val table = mapTiles(tiles)
        var result = Int.MAX_VALUE
        useful?.fill(false)
        for (seq in KNITTED) {
            val allowed = seq + ORPHANS.copyOfRange(6, 13)
            val st = 13 - allowed.count { table[it] > 0 }
            if (st < result) { result = st; useful?.fill(false) }
            if (st == result && useful != null) for (t in allowed) if (table[t] == 0) useful[t] = true
        }
        return result
    }

    private fun specialWait(
        tiles: IntArray, waiting: BooleanArray?, calculation: (IntArray, BooleanArray?) -> Int,
    ): Boolean {
        val temp = if (waiting == null) null else BooleanArray(TABLE_SIZE)
        if (calculation(tiles, temp) != 0) return false
        if (waiting != null) temp!!.copyInto(waiting)
        return true
    }

    fun sevenPairsWait(tiles: IntArray, waiting: BooleanArray? = null): Boolean = specialWait(tiles, waiting, ::sevenPairs)
    fun thirteenOrphansWait(tiles: IntArray, waiting: BooleanArray? = null): Boolean = specialWait(tiles, waiting, ::thirteenOrphans)
    fun honorsAndKnittedWait(tiles: IntArray, waiting: BooleanArray? = null): Boolean = specialWait(tiles, waiting, ::honorsAndKnitted)

    fun waiting(tiles: IntArray): BooleanArray {
        val special = BooleanArray(TABLE_SIZE)
        val specialWaiting = when (tiles.size) {
            13 -> thirteenOrphansWait(tiles, special) || honorsAndKnittedWait(tiles, special) ||
                sevenPairsWait(tiles, special) || knittedWait(tiles, special)
            10 -> knittedWait(tiles, special)
            else -> false
        }
        val regular = BooleanArray(TABLE_SIZE)
        val regularWaiting = regularWait(tiles, regular)
        return when {
            specialWaiting && regularWaiting -> special.also { it.merge(regular) }
            specialWaiting -> special
            regularWaiting -> regular
            else -> BooleanArray(TABLE_SIZE)
        }
    }
}
