/* Port of fan_calculator.cpp at the revision in NOTICE.
 * Copyright (c) 2016-2027 Jeff Wang <summer_insects@163.com>.
 * Kotlin port Copyright (c) 2026 SkyEye_FAST. MIT license; see LICENSE.
 */
package top.skyeyefast.mcr.internal

import top.skyeyefast.mcr.Fan
import top.skyeyefast.mcr.Fan.*

internal data class RawScore(val total: Int, val table: IntArray)

internal object FanCalculator {
    private operator fun IntArray.get(fan: Fan): Int = this[fan.index]
    private operator fun IntArray.set(fan: Fan, value: Int) { this[fan.index] = value }
    private fun fanTable(): IntArray = IntArray(Fan.entries.size + 1)

    private fun divideTail(table: IntArray, fixed: Int, work: IntArray, result: MutableList<IntArray>): Boolean {
        for (t in ALL_TILES) {
            if (table[t] < 2) continue
            table[t] -= 2
            val used = table.all { it == 0 }
            table[t] += 2
            if (used) {
                work[4] = pack(0, PAIR, t)
                val copy = work.copyOf()
                if (fixed < 4) copy.sort(fixed, 4)
                if (result.none { old -> (fixed until 4).all { old[it] == copy[it] } }) result.add(copy)
                return true
            }
        }
        return false
    }

    private fun divideRec(table: IntArray, fixed: Int, step: Int, previous: Int, work: IntArray, result: MutableList<IntArray>): Boolean {
        val index = fixed + step
        if (index == 4) return divideTail(table, fixed, work, result)
        var found = false
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 2) {
                val e = eigen(t, t, t)
                if (e > previous) {
                    work[index] = pack(0, PUNG, t)
                    table[t] -= 3
                    if (divideRec(table, fixed, step + 1, e, work, result)) found = true
                    table[t] += 3
                }
            }
            if (numbered(t) && rank(t) < 8 && table[t + 1] != 0 && table[t + 2] != 0) {
                val e = eigen(t, t + 1, t + 2)
                if (e >= previous) {
                    work[index] = pack(0, CHOW, t + 1)
                    table[t]--; table[t + 1]--; table[t + 2]--
                    if (divideRec(table, fixed, step + 1, e, work, result)) found = true
                    table[t]++; table[t + 1]++; table[t + 2]++
                }
            }
        }
        return found
    }

    private fun divide(table: IntArray, fixedPacks: IntArray): List<IntArray> {
        val result = ArrayList<IntArray>()
        if (ALL_TILES.none { table[it] > 1 }) return result
        val work = IntArray(5)
        fixedPacks.copyInto(work)
        divideRec(table, fixedPacks.size, 0, 0, work, result)
        return result
    }
}
