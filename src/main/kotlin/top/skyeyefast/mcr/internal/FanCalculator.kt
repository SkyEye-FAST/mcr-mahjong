/* Port of fan_calculator.cpp at the revision in NOTICE.
 * Copyright (c) 2016-2027 Jeff Wang <summer_insects@163.com>.
 * Kotlin port Copyright (c) 2026 SkyEye_FAST. MIT license; see LICENSE.
 */
package top.skyeyefast.mcr.internal

import top.skyeyefast.mcr.internal.UpstreamFan as Fan
import top.skyeyefast.mcr.internal.UpstreamFan.*

internal data class RawScore(val total: Int, val table: IntArray)

internal object FanCalculator {
    private operator fun IntArray.get(fan: Fan): Int = this[fan.index]
    private operator fun IntArray.set(fan: Fan, value: Int) { this[fan.index] = value }
    private fun fanTable(): IntArray = IntArray(Fan.entries.size + 1)

    private fun regularFan(
        packs: IntArray, fixedTable: IntArray, standing: IntArray, unique: IntArray,
        fixed: Int, winTile: Int, uniqueWaiting: Boolean, flags: Int,
        prevalent: Int, seat: Int, table: IntArray,
    ) {
        var pair = 0
        val chows = ArrayList<Int>(4); val pungs = ArrayList<Int>(4)
        var concealedPungs = 0; var meldedKongs = 0; var concealedKongs = 0
        for (p in packs) when (packType(p)) {
            CHOW -> chows.add(p)
            PUNG -> { pungs.add(p); if (!melded(p)) concealedPungs++ }
            KONG -> { pungs.add(p); if (melded(p)) meldedKongs++ else concealedKongs++ }
            PAIR -> pair = p
            else -> return
        }
        if (pair == 0 || chows.size + pungs.size != 4) return
        adjustWinFlags(flags, table)
        if ((flags and 1) == 0 && chows.none {
                val t = packTile(it)
                !melded(it) && (t - 1 == winTile || t == winTile || t + 1 == winTile)
            }) {
            for (p in pungs) if (packTile(p) == winTile && !melded(p)) concealedPungs--
        }
        if (pungs.isNotEmpty()) {
            calculateKongs(concealedPungs, meldedKongs, concealedKongs, table)
            if (pungs.size == 4 && table[FOUR_KONGS] == 0 && table[FOUR_CONCEALED_PUNGS] == 0) table[ALL_PUNGS] = 1
            for (p in pungs) onePungFan(packTile(p))?.let { table[it]++ }
        }
        val chowTiles = chows.map(::packTile).sorted().toIntArray()
        val pungTiles = pungs.map(::packTile).sorted().toIntArray()
        when (chows.size) {
            4 -> when {
                threeSuitedTerminalChows(chows.toIntArray(), pair) -> table[THREE_SUITED_TERMINAL_CHOWS] = 1
                pureTerminalChows(chows.toIntArray(), pair) -> table[PURE_TERMINAL_CHOWS] = 1
                else -> calculateFourChows(chowTiles, table)
            }
            3 -> calculateThreeChows(chowTiles, table)
            2 -> {
                twoChowsFan(chowTiles[0], chowTiles[1])?.let { table[it]++ }
                twoPungsFan(pungTiles[0], pungTiles[1])?.let { table[it]++ }
            }
            1 -> calculateThreePungs(pungTiles, table)
            0 -> calculateFourPungs(pungTiles, table)
        }
        adjustSelfDrawn(packs, fixed, (flags and 1) != 0, table)
        adjustPair(packTile(pair), chows.size, table)
        adjustPacksTraits(packs, table)
        val merged = IntArray(TABLE_SIZE) { standing[it] + fixedTable[it] }
        adjustSuits(unique, table)
        adjustTilesTraits(unique, table)
        adjustRange(unique, table)
        if (table[QUADRUPLE_CHOW] == 0) adjustTileHog(merged, meldedKongs + concealedKongs, table)
        if (uniqueWaiting) adjustWaitingForm(packs.copyOfRange(fixed, 5), winTile, table)
        finalAdjust(table)
        if (table[BIG_FOUR_WINDS] == 0) {
            for (p in pungs) if (wind(packTile(p))) adjustWinds(packTile(p), prevalent, seat, table)
        }
        if (table.all { it == 0 }) table[CHICKEN_HAND] = 1
    }

    private fun knittedFan(
        fixedTable: IntArray, standing: IntArray, fixedPacks: IntArray,
        winTile: Int, prevalent: Int, seat: Int, flags: Int, table: IntArray,
    ): Boolean {
        if (ALL_TILES.none { standing[it] > 1 }) return false
        val sequence = KNITTED.firstOrNull { row -> row.all { standing[it] > 0 } } ?: return false
        val rest = standing.copyOf()
        for (t in sequence) rest[t]--
        val work = IntArray(5)
        val fixed = fixedPacks.size
        if (fixed == 1) work[3] = fixedPacks[0]
        val divisions = ArrayList<IntArray>()
        divideRec(rest, fixed + 3, 0, 0, work, divisions)
        if (divisions.size != 1) return false
        val packs = divisions[0]
        val involved = packs[3]
        val pairTile = packTile(packs[4])
        table[KNITTED_STRAIGHT] = 1
        val type = packType(involved)
        if (type == CHOW) {
            if (numbered(pairTile)) table[ALL_CHOWS] = 1
            if (fixedTable[pairTile] + standing[pairTile] == 4) table[TILE_HOG] = 1
        } else {
            val tile = packTile(involved)
            if (honor(tile)) {
                if (wind(tile)) {
                    table[PUNG_OF_TERMINALS_OR_HONORS] = 1
                    adjustWinds(tile, prevalent, seat, table)
                    if (dragon(pairTile)) table[ALL_TYPES] = 1
                } else {
                    table[DRAGON_PUNG] = 1
                    if (wind(pairTile)) table[ALL_TYPES] = 1
                }
            } else {
                if (terminal(tile)) table[PUNG_OF_TERMINALS_OR_HONORS] = 1
                if (!honor(pairTile)) table[NO_HONORS] = 1
                if (type != KONG && fixedTable[tile] + standing[tile] == 4) table[TILE_HOG] = 1
            }
        }
        adjustWinFlags(flags, table)
        if (melded(involved)) {
            if (type == KONG) table[MELDED_KONG] = 1
        } else {
            if (type == KONG) table[CONCEALED_KONG] = 1
            if ((flags and 1) != 0) { table[FULLY_CONCEALED_HAND] = 1; table[SELF_DRAWN] = 0 }
            else table[CONCEALED_HAND] = 1
        }
        val heavenly = seat == 0 && fixed == 0 && (flags and 17) == 17
        if (fixed == 0) {
            if (!heavenly && uniqueWaiting(rest, 4, winTile)) adjustWaitingForm(packs.copyOfRange(3, 5), winTile, table)
        } else {
            if (winTile !in sequence || standing[winTile] == 3) table[SINGLE_WAIT] = 1
        }
        finalAdjust(table)
        return true
    }

    private fun sevenShiftedPairs(table: IntArray, suit: Int): Boolean {
        if (suit == 4) return false
        val t3 = (suit shl 4) or 3
        if ((0..4).all { table[t3 + it] == 2 }) {
            if (table[t3 - 1] == 2) return table[t3 - 2] == 2 || table[t3 + 5] == 2
            return table[t3 + 5] == 2 && table[t3 + 6] == 2
        }
        return false
    }

    private fun honorsAndKnitted(unique: IntArray, table: IntArray): Boolean {
        if (unique.size != 14) return false
        val firstHonor = unique.indexOfFirst(::honor).let { if (it < 0) 14 else it }
        if (firstHonor !in 7..9) return false
        if (KNITTED.none { sequence -> (0 until firstHonor).all { unique[it] in sequence } }) return false
        if (firstHonor == 7 && (0..6).all { ORPHANS[it + 6] == unique[it + 7] }) {
            table[GREATER_HONORS_AND_KNITTED_TILES] = 1
            return true
        }
        if ((firstHonor until 14).all { unique[it] in 0x41..0x47 }) {
            table[LESSER_HONORS_AND_KNITTED_TILES] = 1
            if (firstHonor == 9) table[KNITTED_STRAIGHT] = 1
            return true
        }
        return false
    }

    private fun specialFan(standing: IntArray, winTile: Int, unique: IntArray, flags: Int, table: IntArray): Boolean {
        if (ALL_TILES.all { (standing[it] and 1) == 0 }) {
            val s = suit(winTile)
            if (sevenShiftedPairs(standing, s)) {
                table[SEVEN_SHIFTED_PAIRS] = 1
                if (standing[(s shl 4) or 1] == 0 && standing[(s shl 4) or 9] == 0) table[ALL_SIMPLES] = 1
                adjustWinFlags(flags, table)
            } else {
                table[SEVEN_PAIRS] = 1
                adjustSuits(unique, table)
                adjustTilesTraits(unique, table)
                adjustRange(unique, table)
                adjustTileHog(standing, 0, table)
                adjustWinFlags(flags, table)
                finalAdjust(table)
            }
            return true
        }
        if (honorsAndKnitted(unique, table)) { adjustWinFlags(flags, table); return true }
        if (unique.contentEquals(ORPHANS)) {
            table[THIRTEEN_ORPHANS] = 1
            adjustWinFlags(flags, table)
            return true
        }
        return false
    }

    private fun nineGates(standing: IntArray, winTile: Int, seat: Int, flags: Int, table: IntArray): Boolean {
        val s = suit(winTile)
        // The public API only admits legal tile kinds. No honors can form nine gates.
        if (s == 4) return false
        val base = s shl 4
        var r = rank(winTile)
        if (seat == 0 && (flags and 17) == 17) {
            var secondWin = 0
            for (i in 2..8) {
                val count = standing[base + i]
                if (count == 0) return false
                if (count == 2) secondWin = base + i
            }
            if (secondWin != 0) {
                if (standing[base + 1] != 3 || standing[base + 9] != 3) return false
                r = rank(secondWin)
            } else {
                r = when {
                    standing[base + 1] == 4 && standing[base + 9] == 3 -> 1
                    // Preserve the pinned upstream's r = 2 here (not r = 9).
                    standing[base + 1] == 3 && standing[base + 9] == 4 -> 2
                    else -> return false
                }
            }
        } else when (r) {
            1 -> if (standing[winTile] != 4 || standing[base + 9] != 3 || (2..8).any { standing[base + it] != 1 }) return false
            9 -> if (standing[winTile] != 4 || standing[base + 1] != 3 || (2..8).any { standing[base + it] != 1 }) return false
            else -> if (standing[winTile] != 2 || standing[base + 1] != 3 || standing[base + 9] != 3 ||
                (2 until r).any { standing[base + it] != 1 } || (r + 1..8).any { standing[base + it] != 1 }) return false
        }
        table[NINE_GATES] = 1
        when (r) {
            1, 9 -> { table[PURE_STRAIGHT] = 1; table[TILE_HOG] = 1 }
            2, 8 -> { table[TWO_CONCEALED_PUNGS] = 1; table[SHORT_STRAIGHT] = 1; table[PUNG_OF_TERMINALS_OR_HONORS] = 1 }
            5 -> { table[TWO_CONCEALED_PUNGS] = 1; table[PUNG_OF_TERMINALS_OR_HONORS] = 1 }
            3, 4, 6, 7 -> table[SHORT_STRAIGHT] = 1
        }
        adjustWinFlags(flags, table)
        return true
    }

    private fun total(table: IntArray): Int = Fan.entries.sumOf { it.points * table[it] }

    fun calculate(
        tiles: IntArray, fixedPacks: IntArray, winTile: Int, flags: Int,
        prevalent: Int, seat: Int, flowers: Int = 0,
        evaluate: (IntArray, IntArray?) -> Int = { table, _ -> total(table) },
    ): RawScore {
        val fixed = fixedPacks.size
        val empty = fanTable()
        if (tiles.isEmpty() || fixed !in 0..4 || fixed * 3 + tiles.size != 13) return RawScore(-1, empty)
        val fixedTable = mapPacks(fixedPacks)
        val standing = mapTiles(tiles)
        standing[winTile]++
        val unique = ALL_TILES.filter { fixedTable[it] != 0 || standing[it] != 0 }.toIntArray()
        var corrected = flags
        if (standing[winTile] != 1) corrected = corrected and 2.inv()
        if (fixedTable[winTile] == 3) corrected = corrected or 2
        if ((corrected and 4) != 0) {
            if ((corrected and 1) != 0) {
                if (fixedPacks.none { packType(it) == KONG }) corrected = corrected and 4.inv()
            } else if (fixedTable[winTile] != 0 || standing[winTile] != 1) corrected = corrected and 4.inv()
        }
        var maximum = 0
        val temporary = fanTable()
        if (fixed == 0) {
            if (specialFan(standing, winTile, unique, corrected, temporary) ||
                knittedFan(fixedTable, standing, fixedPacks, winTile, prevalent, seat, corrected, temporary) ||
                nineGates(standing, winTile, seat, corrected, temporary)) maximum = evaluate(temporary, null)
        } else if (fixed == 1) {
            if (knittedFan(fixedTable, standing, fixedPacks, winTile, prevalent, seat, corrected, temporary)) maximum = evaluate(temporary, null)
        }
        if (maximum == 0 || temporary[SEVEN_PAIRS] == 1) {
            val heavenly = seat == 0 && fixed == 0 && (corrected and 17) == 17
            val uniqueWait = !heavenly && uniqueWaiting(standing, tiles.size, winTile)
            var selected: IntArray? = null
            for (packs in divide(standing, fixedPacks)) {
                val current = fanTable()
                regularFan(packs, fixedTable, standing, unique, fixed, winTile, uniqueWait, corrected, prevalent, seat, current)
                // Normalize each candidate before comparison, not only the upstream winner.
                val points = evaluate(current, packs)
                if (points > maximum) { maximum = points; selected = current }
                else if (points == maximum && (current[PURE_TRIPLE_CHOW] == 1 || temporary[SEVEN_PAIRS] == 1 || current[TRIPLE_PUNG] != 0)) selected = current
            }
            selected?.copyInto(temporary)
        }
        if (maximum == 0) return RawScore(-3, empty)
        temporary[FLOWER_TILES] = flowers
        return RawScore(maximum + flowers, temporary)
    }

    private fun adjustSelfDrawn(packs: IntArray, fixed: Int, selfDrawn: Boolean, table: IntArray) {
        when ((0 until fixed).count { melded(packs[it]) }) {
            0 -> table[if (selfDrawn) FULLY_CONCEALED_HAND else CONCEALED_HAND] = 1
            4 -> table[if (selfDrawn) SELF_DRAWN else MELDED_HAND] = 1
            else -> if (selfDrawn) table[SELF_DRAWN] = 1
        }
    }

    private fun adjustPair(tile: Int, chowCount: Int, table: IntArray) {
        if (chowCount == 4) {
            if (numbered(tile)) table[ALL_CHOWS] = 1
            return
        }
        if (table[TWO_DRAGONS_PUNGS] != 0) {
            if (dragon(tile)) { table[LITTLE_THREE_DRAGONS] = 1; table[TWO_DRAGONS_PUNGS] = 0 }
            return
        }
        if (table[BIG_THREE_WINDS] != 0) {
            if (wind(tile)) { table[LITTLE_FOUR_WINDS] = 1; table[BIG_THREE_WINDS] = 0 }
            return
        }
    }

    private fun adjustSuits(tiles: IntArray, table: IntArray) {
        var flags = 0
        for (tile in tiles) flags = flags or (1 shl suit(tile))
        if ((flags and 0xF1) == 0) table[NO_HONORS] = 1
        if ((flags and 0xE3) == 0) table[ONE_VOIDED_SUIT]++
        if ((flags and 0xE5) == 0) table[ONE_VOIDED_SUIT]++
        if ((flags and 0xE9) == 0) table[ONE_VOIDED_SUIT]++
        if (table[ONE_VOIDED_SUIT] == 2) {
            table[ONE_VOIDED_SUIT] = 0
            if (table[NO_HONORS] == 0) table[HALF_FLUSH] = 1
            else { table[FULL_FLUSH] = 1; table[NO_HONORS] = 0 }
        }
        if (flags == 0x1E && tiles.any(::wind) && tiles.any(::dragon)) table[ALL_TYPES] = 1
    }

    private fun adjustRange(tiles: IntArray, table: IntArray) {
        var flags = 0
        for (tile in tiles) {
            if (!numbered(tile)) return
            flags = flags or (1 shl rank(tile))
        }
        if ((flags and 0xFFE1) == 0) {
            table[if ((flags and 0x10) != 0) LOWER_FOUR else LOWER_TILES] = 1
            return
        }
        if ((flags and 0xFC3F) == 0) {
            table[if ((flags and 0x40) != 0) UPPER_FOUR else UPPER_TILES] = 1
            return
        }
        if ((flags and 0xFF8F) == 0) table[MIDDLE_TILES] = 1
    }

    private fun adjustPacksTraits(packs: IntArray, table: IntArray) {
        var terminals = 0; var honors = 0; var fives = 0; var evens = 0
        for (p in packs) {
            val tile = packTile(p)
            if (numbered(tile)) {
                if (packType(p) == CHOW) {
                    when (rank(tile)) { 2, 8 -> terminals++; 4, 5, 6 -> fives++ }
                } else {
                    when (rank(tile)) { 1, 9 -> terminals++; 5 -> fives++; 2, 4, 6, 8 -> evens++ }
                }
            } else honors++
        }
        if (terminals + honors == 5) { table[OUTSIDE_HAND] = 1; return }
        if (fives == 5) { table[ALL_FIVE] = 1; return }
        if (evens == 5) table[ALL_EVEN_PUNGS] = 1
    }

    private fun adjustTilesTraits(tiles: IntArray, table: IntArray) {
        if (tiles.none(::terminalOrHonor)) table[ALL_SIMPLES] = 1
        if (tiles.all(::reversible)) table[REVERSIBLE_TILES] = 1
        if (tiles.all(::green)) table[ALL_GREEN] = 1
        if (table[ALL_SIMPLES] != 0) return
        if (tiles.all(::honor)) { table[ALL_HONORS] = 1; return }
        if (tiles.all(::terminal)) { table[ALL_TERMINALS] = 1; return }
        if (tiles.all(::terminalOrHonor)) table[ALL_TERMINALS_AND_HONORS] = 1
    }

    private fun adjustTileHog(tiles: IntArray, kongs: Int, table: IntArray) {
        table[TILE_HOG] = (tiles.count { it == 4 } - kongs) and 255
    }

    private fun adjustWaitingForm(concealed: IntArray, winTile: Int, table: IntArray) {
        if (table[MELDED_HAND] != 0 || table[FOUR_KONGS] != 0) return
        var flags = 0
        for (p in concealed) {
            val tile = packTile(p)
            when (packType(p)) {
                CHOW -> {
                    if (tile == winTile) flags = flags or 2
                    else if (tile + 1 == winTile || tile - 1 == winTile) flags = flags or 1
                }
                PAIR -> if (tile == winTile) flags = flags or 4
            }
        }
        when {
            (flags and 1) != 0 -> table[EDGE_WAIT] = 1
            (flags and 2) != 0 -> table[CLOSED_WAIT] = 1
            (flags and 4) != 0 -> table[SINGLE_WAIT] = 1
        }
    }

    // Preserve upstream's exclusion order; exclusions absent here are handled
    // when building the fan table, not through an independent rules matrix.
    private fun finalAdjust(table: IntArray) {
        if (table[BIG_FOUR_WINDS] != 0) { table[ALL_PUNGS] = 0; table[PUNG_OF_TERMINALS_OR_HONORS] = 0 }
        if (table[BIG_THREE_DRAGONS] != 0) table[DRAGON_PUNG] = 0
        if (table[ALL_GREEN] != 0) { table[HALF_FLUSH] = 0; table[ONE_VOIDED_SUIT] = 0 }
        if (table[FOUR_KONGS] != 0) table[SINGLE_WAIT] = 0
        if (table[ALL_TERMINALS] != 0) {
            table[ALL_PUNGS] = 0; table[OUTSIDE_HAND] = 0
            table[PUNG_OF_TERMINALS_OR_HONORS] = 0; table[NO_HONORS] = 0; table[DOUBLE_PUNG] = 0
        }
        if (table[LITTLE_FOUR_WINDS] != 0) table[PUNG_OF_TERMINALS_OR_HONORS] = 0
        if (table[LITTLE_THREE_DRAGONS] != 0) table[DRAGON_PUNG] = 0
        if (table[ALL_HONORS] != 0) {
            table[ALL_PUNGS] = 0; table[OUTSIDE_HAND] = 0
            table[PUNG_OF_TERMINALS_OR_HONORS] = 0; table[ONE_VOIDED_SUIT] = 0
        }
        if (table[FOUR_CONCEALED_PUNGS] != 0) {
            table[ALL_PUNGS] = 0; table[CONCEALED_HAND] = 0
            if (table[FULLY_CONCEALED_HAND] != 0) { table[FULLY_CONCEALED_HAND] = 0; table[SELF_DRAWN] = 1 }
        }
        if (table[PURE_TERMINAL_CHOWS] != 0) { table[FULL_FLUSH] = 0; table[ALL_CHOWS] = 0; table[NO_HONORS] = 0 }
        if (table[FOUR_PURE_SHIFTED_PUNGS] != 0) table[ALL_PUNGS] = 0
        if (table[ALL_TERMINALS_AND_HONORS] != 0) {
            table[ALL_PUNGS] = 0; table[OUTSIDE_HAND] = 0; table[PUNG_OF_TERMINALS_OR_HONORS] = 0
        }
        if (table[ALL_EVEN_PUNGS] != 0) { table[ALL_PUNGS] = 0; table[ALL_SIMPLES] = 0; table[NO_HONORS] = 0 }
        if (table[UPPER_TILES] != 0) table[NO_HONORS] = 0
        if (table[MIDDLE_TILES] != 0) { table[ALL_SIMPLES] = 0; table[NO_HONORS] = 0 }
        if (table[LOWER_TILES] != 0) table[NO_HONORS] = 0
        if (table[THREE_SUITED_TERMINAL_CHOWS] != 0) { table[ALL_CHOWS] = 0; table[NO_HONORS] = 0 }
        if (table[ALL_FIVE] != 0) { table[ALL_SIMPLES] = 0; table[NO_HONORS] = 0 }
        if (table[UPPER_FOUR] != 0) table[NO_HONORS] = 0
        if (table[LOWER_FOUR] != 0) table[NO_HONORS] = 0
        if (table[BIG_THREE_WINDS] != 0 && table[ALL_HONORS] == 0 && table[ALL_TERMINALS_AND_HONORS] == 0) {
            check(table[PUNG_OF_TERMINALS_OR_HONORS] >= 3)
            table[PUNG_OF_TERMINALS_OR_HONORS] -= 3
        }
        if (table[REVERSIBLE_TILES] != 0) table[ONE_VOIDED_SUIT] = 0
        if (table[LAST_TILE_DRAW] != 0) table[SELF_DRAWN] = 0
        if (table[OUT_WITH_REPLACEMENT_TILE] != 0) table[SELF_DRAWN] = 0
        if (table[MELDED_HAND] != 0) table[SINGLE_WAIT] = 0
        if (table[TWO_DRAGONS_PUNGS] != 0) table[DRAGON_PUNG] = 0
        if (table[FULLY_CONCEALED_HAND] != 0) table[SELF_DRAWN] = 0
        if (table[ALL_CHOWS] != 0) table[NO_HONORS] = 0
        if (table[ALL_SIMPLES] != 0) table[NO_HONORS] = 0
    }

    private fun adjustWinds(tile: Int, prevalent: Int, seat: Int, table: IntArray) {
        val deducted = table[BIG_THREE_WINDS] != 0 || table[ALL_TERMINALS_AND_HONORS] != 0 ||
            table[ALL_HONORS] != 0 || table[LITTLE_FOUR_WINDS] != 0
        if (tile - 0x41 == prevalent) {
            table[PREVALENT_WIND] = 1
            if (!deducted) table[PUNG_OF_TERMINALS_OR_HONORS]--
        }
        if (tile - 0x41 == seat) {
            table[SEAT_WIND] = 1
            if (seat != prevalent && !deducted) table[PUNG_OF_TERMINALS_OR_HONORS]--
        }
    }

    private fun adjustWinFlags(flags: Int, table: IntArray) {
        if ((flags and 2) != 0) table[LAST_TILE] = 1
        if ((flags and 1) != 0) {
            table[SELF_DRAWN] = 1
            if ((flags and 8) != 0) { table[LAST_TILE_DRAW] = 1; table[SELF_DRAWN] = 0 }
            if ((flags and 4) != 0) { table[OUT_WITH_REPLACEMENT_TILE] = 1; table[SELF_DRAWN] = 0 }
        } else {
            if ((flags and 8) != 0) table[LAST_TILE_CLAIM] = 1
            if ((flags and 4) != 0) { table[ROBBING_THE_KONG] = 1; table[LAST_TILE] = 0 }
        }
    }

    private fun shifted1(a: Int, b: Int, c: Int): Boolean = a + 1 == b && b + 1 == c
    private fun shifted2(a: Int, b: Int, c: Int): Boolean = a + 2 == b && b + 2 == c
    private fun mixed(a: Int, b: Int, c: Int): Boolean = a != b && a != c && b != c
    private fun shiftedUnordered(a: Int, b: Int, c: Int): Boolean =
        shifted1(b, a, c) || shifted1(c, a, b) || shifted1(a, b, c) ||
            shifted1(c, b, a) || shifted1(a, c, b) || shifted1(b, c, a)

    private fun fourChowsFan(a: Int, b: Int, c: Int, d: Int): Fan? = when {
        a + 2 == b && b + 2 == c && c + 2 == d -> FOUR_PURE_SHIFTED_CHOWS
        a + 1 == b && b + 1 == c && c + 1 == d -> FOUR_PURE_SHIFTED_CHOWS
        a == b && a == c && a == d -> QUADRUPLE_CHOW
        else -> null
    }

    private fun threeChowsFan(a: Int, b: Int, c: Int): Fan? {
        val r0 = rank(a); val r1 = rank(b); val r2 = rank(c)
        if (mixed(suit(a), suit(b), suit(c))) {
            if (shiftedUnordered(r1, r0, r2)) return MIXED_SHIFTED_CHOWS
            if (r0 == r1 && r1 == r2) return MIXED_TRIPLE_CHOW
            if ((r0 == 2 && r1 == 5 && r2 == 8) || (r0 == 2 && r1 == 8 && r2 == 5) ||
                (r0 == 5 && r1 == 2 && r2 == 8) || (r0 == 5 && r1 == 8 && r2 == 2) ||
                (r0 == 8 && r1 == 2 && r2 == 5) || (r0 == 8 && r1 == 5 && r2 == 2)) return MIXED_STRAIGHT
        } else {
            if (a + 3 == b && b + 3 == c) return PURE_STRAIGHT
            if (shifted2(a, b, c) || shifted1(a, b, c)) return PURE_SHIFTED_CHOWS
            if (a == b && a == c) return PURE_TRIPLE_CHOW
        }
        return null
    }

    private fun twoChowsFan(a: Int, b: Int): Fan? {
        if (!sameSuit(a, b)) {
            if (sameRank(a, b)) return MIXED_DOUBLE_CHOW
        } else {
            if (a + 3 == b || b + 3 == a) return SHORT_STRAIGHT
            if ((rank(a) == 2 && rank(b) == 8) || (rank(a) == 8 && rank(b) == 2)) return TWO_TERMINAL_CHOWS
            if (a == b) return PURE_DOUBLE_CHOW
        }
        return null
    }

    private fun fourPungsFan(a: Int, b: Int, c: Int, d: Int): Fan? = when {
        numbered(a) && a + 1 == b && b + 1 == c && c + 1 == d -> FOUR_PURE_SHIFTED_PUNGS
        a == 0x41 && b == 0x42 && c == 0x43 && d == 0x44 -> BIG_FOUR_WINDS
        else -> null
    }

    private fun threePungsFan(a: Int, b: Int, c: Int): Fan? {
        if (numbered(a) && numbered(b) && numbered(c)) {
            val r0 = rank(a); val r1 = rank(b); val r2 = rank(c)
            if (mixed(suit(a), suit(b), suit(c))) {
                if (shiftedUnordered(r1, r0, r2)) return MIXED_SHIFTED_PUNGS
                if (r0 == r1 && r1 == r2) return TRIPLE_PUNG
            } else if (a + 1 == b && b + 1 == c) return PURE_SHIFTED_PUNGS
        } else {
            if ((a == 0x41 && b == 0x42 && c == 0x43) || (a == 0x41 && b == 0x42 && c == 0x44) ||
                (a == 0x41 && b == 0x43 && c == 0x44) || (a == 0x42 && b == 0x43 && c == 0x44)) return BIG_THREE_WINDS
            if (a == 0x45 && b == 0x46 && c == 0x47) return BIG_THREE_DRAGONS
        }
        return null
    }

    private fun twoPungsFan(a: Int, b: Int): Fan? {
        if (numbered(a) && numbered(b)) {
            if (sameRank(a, b)) return DOUBLE_PUNG
        } else if (dragon(a) && dragon(b)) return TWO_DRAGONS_PUNGS
        return null
    }

    private fun onePungFan(tile: Int): Fan? = when {
        dragon(tile) -> DRAGON_PUNG
        terminal(tile) || wind(tile) -> PUNG_OF_TERMINALS_OR_HONORS
        else -> null
    }

    private fun extraChowFan(a: Int, b: Int, c: Int, extra: Int): Fan? {
        val f0 = twoChowsFan(a, extra); val f1 = twoChowsFan(b, extra); val f2 = twoChowsFan(c, extra)
        for (fan in arrayOf(PURE_DOUBLE_CHOW, MIXED_DOUBLE_CHOW, SHORT_STRAIGHT, TWO_TERMINAL_CHOWS)) {
            if (fan == f0 || fan == f1 || fan == f2) return fan
        }
        return null
    }

    private fun exclusionaryRule(fans: Array<Fan?>, maximum: Int, table: IntArray) {
        val counts = IntArray(4)
        var count = 0
        for (fan in fans) if (fan != null) { count++; counts[fan.index - PURE_DOUBLE_CHOW.index]++ }
        var limit = 1
        while (count > maximum && limit >= 0) {
            var index = 3
            while (count > maximum && index >= 0) {
                while (counts[index] > limit && count > maximum) { counts[index]--; count-- }
                index--
            }
            limit--
        }
        table[PURE_DOUBLE_CHOW] = counts[0]
        table[MIXED_DOUBLE_CHOW] = counts[1]
        table[SHORT_STRAIGHT] = counts[2]
        table[TWO_TERMINAL_CHOWS] = counts[3]
    }

    private fun threeOfFourChows(a: Int, b: Int, c: Int, extra: Int, table: IntArray): Boolean {
        val fan = threeChowsFan(a, b, c) ?: return false
        table[fan] = 1
        extraChowFan(a, b, c, extra)?.let { table[it] = 1 }
        return true
    }

    private fun calculateFourChows(t: IntArray, table: IntArray) {
        fourChowsFan(t[0], t[1], t[2], t[3])?.let { table[it] = 1; return }
        if (threeOfFourChows(t[0], t[1], t[2], t[3], table) ||
            threeOfFourChows(t[0], t[1], t[3], t[2], table) ||
            threeOfFourChows(t[0], t[2], t[3], t[1], table) ||
            threeOfFourChows(t[1], t[2], t[3], t[0], table)) return
        val fans = arrayOf(
            twoChowsFan(t[0], t[1]), twoChowsFan(t[0], t[2]), twoChowsFan(t[0], t[3]),
            twoChowsFan(t[1], t[2]), twoChowsFan(t[1], t[3]), twoChowsFan(t[2], t[3]),
        )
        var maximum = 3
        if (fans[0] == null && fans[1] == null && fans[2] == null) maximum--
        if (fans[0] == null && fans[3] == null && fans[4] == null) maximum--
        if (fans[1] == null && fans[3] == null && fans[5] == null) maximum--
        if (fans[2] == null && fans[4] == null && fans[5] == null) maximum--
        if (maximum > 0) exclusionaryRule(fans, maximum, table)
    }

    private fun calculateThreeChows(t: IntArray, table: IntArray) {
        threeChowsFan(t[0], t[1], t[2])?.let { table[it] = 1; return }
        exclusionaryRule(arrayOf(twoChowsFan(t[0], t[1]), twoChowsFan(t[0], t[2]), twoChowsFan(t[1], t[2])), 2, table)
    }

    private fun calculateFourPungs(t: IntArray, table: IntArray) {
        fourPungsFan(t[0], t[1], t[2], t[3])?.let { table[it] = 1; return }
        var free = -1
        for (indices in arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(0, 1, 3, 2), intArrayOf(0, 2, 3, 1), intArrayOf(1, 2, 3, 0))) {
            val fan = threePungsFan(t[indices[0]], t[indices[1]], t[indices[2]])
            if (fan != null) { table[fan] = 1; free = indices[3]; break }
        }
        if (free >= 0) {
            for (i in 0..3) {
                if (i == free) continue
                val fan = twoPungsFan(t[i], t[free])
                if (fan != null) { table[fan]++; break }
            }
            return
        }
        for (i in 0..2) for (j in i + 1..3) twoPungsFan(t[i], t[j])?.let { table[it]++ }
    }

    private fun calculateThreePungs(t: IntArray, table: IntArray) {
        threePungsFan(t[0], t[1], t[2])?.let { table[it] = 1; return }
        for (i in 0..1) for (j in i + 1..2) twoPungsFan(t[i], t[j])?.let { table[it]++ }
    }

    private fun calculateKongs(concealedPungs: Int, meldedKongs: Int, concealedKongs: Int, table: IntArray) {
        fun concealed(count: Int) {
            when (count) {
                2 -> table[TWO_CONCEALED_PUNGS] = 1
                3 -> table[THREE_CONCEALED_PUNGS] = 1
                4 -> table[FOUR_CONCEALED_PUNGS] = 1
            }
        }
        when (meldedKongs + concealedKongs) {
            0 -> concealed(concealedPungs)
            1 -> {
                if (meldedKongs == 1) { table[MELDED_KONG] = 1; concealed(concealedPungs) }
                else { table[CONCEALED_KONG] = 1; concealed(concealedPungs + 1) }
            }
            2 -> when (concealedKongs) {
                0 -> { table[TWO_MELDED_KONGS] = 1; if (concealedPungs == 2) table[TWO_CONCEALED_PUNGS] = 1 }
                1 -> { table[CONCEALED_KONG_AND_MELDED_KONG] = 1; concealed(concealedPungs + 1) }
                2 -> { table[TWO_CONCEALED_KONGS] = 1; if (concealedPungs > 0) concealed(concealedPungs + 2) }
            }
            3 -> {
                table[THREE_KONGS] = 1
                when (concealedKongs) {
                    1 -> if (concealedPungs > 0) table[TWO_CONCEALED_PUNGS] = 1
                    2 -> table[if (concealedPungs == 0) TWO_CONCEALED_PUNGS else THREE_CONCEALED_PUNGS] = 1
                    3 -> table[if (concealedPungs == 0) THREE_CONCEALED_PUNGS else FOUR_CONCEALED_PUNGS] = 1
                }
            }
            4 -> { table[FOUR_KONGS] = 1; concealed(concealedKongs) }
        }
    }

    private fun pureTerminalChows(chows: IntArray, pair: Int): Boolean {
        val pairTile = packTile(pair)
        if (rank(pairTile) != 5) return false
        var low = 0; var high = 0
        for (p in chows) {
            val t = packTile(p)
            if (suit(t) != suit(pairTile)) return false
            when (rank(t)) { 2 -> low++; 8 -> high++; else -> return false }
        }
        return low == 2 && high == 2
    }

    private fun threeSuitedTerminalChows(chows: IntArray, pair: Int): Boolean {
        val pairTile = packTile(pair)
        if (rank(pairTile) != 5) return false
        val low = IntArray(4); val high = IntArray(4)
        val pairSuit = suit(pairTile)
        for (p in chows) {
            val t = packTile(p)
            if (suit(t) == pairSuit) return false
            when (rank(t)) { 2 -> low[suit(t)]++; 8 -> high[suit(t)]++; else -> return false }
        }
        return when (pairSuit) {
            1 -> low[2] != 0 && low[3] != 0 && high[2] != 0 && high[3] != 0
            2 -> low[1] != 0 && low[3] != 0 && high[1] != 0 && high[3] != 0
            3 -> low[1] != 0 && low[2] != 0 && high[1] != 0 && high[2] != 0
            else -> false
        }
    }

    private fun checkSevenPairsWaiting(standing: IntArray, waiting: IntArray) {
        var pairs = 0
        for (t in ALL_TILES) when (standing[t]) { 2, 3 -> pairs++; 4 -> pairs += 2 }
        if (pairs == 6) for (t in ALL_TILES) {
            if (standing[t] == 1 || standing[t] == 3) { waiting[t] = 1; break }
        }
    }

    private fun regularPackWaiting(table: IntArray, waiting: IntArray) {
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 1) { waiting[t] = 1; return }
            if (numbered(t)) {
                val r = rank(t)
                if (r > 1 && table[t - 1] != 0) {
                    if (r < 9) waiting[t + 1] = 1
                    if (r > 2) waiting[t - 2] = 1
                    return
                }
                if (r > 2 && table[t - 2] != 0) { waiting[t - 1] = 1; return }
            }
        }
    }

    private fun checkRegularWaiting(table: IntArray, count: Int, previous: Int, waiting: IntArray) {
        if (count == 1) {
            for (t in ALL_TILES) if (table[t] == 1) { waiting[t] = 1; break }
            return
        }
        if (count == 4) for (t in ALL_TILES) {
            if (table[t] < 2) continue
            table[t] -= 2
            regularPackWaiting(table, waiting)
            table[t] += 2
        }
        for (t in ALL_TILES) {
            if (table[t] < 1) continue
            if (table[t] > 2) {
                val e = eigen(t, t, t)
                if (e > previous) {
                    table[t] -= 3
                    checkRegularWaiting(table, count - 3, e, waiting)
                    table[t] += 3
                }
            }
            if (numbered(t) && rank(t) < 8 && table[t + 1] != 0 && table[t + 2] != 0) {
                val e = eigen(t, t + 1, t + 2)
                if (e >= previous) {
                    table[t]--; table[t + 1]--; table[t + 2]--
                    checkRegularWaiting(table, count - 3, e, waiting)
                    table[t]++; table[t + 1]++; table[t + 2]++
                }
            }
        }
    }

    private fun uniqueWaiting(standing: IntArray, count: Int, winTile: Int): Boolean {
        val waiting = IntArray(TABLE_SIZE)
        // The knitted body may already contain this tile; preserve uint16_t wraparound.
        standing[winTile] = (standing[winTile] - 1) and 0xFFFF
        if (count == 13) checkSevenPairsWaiting(standing, waiting)
        checkRegularWaiting(standing, count, 0, waiting)
        standing[winTile] = (standing[winTile] + 1) and 0xFFFF
        return ALL_TILES.count { waiting[it] != 0 } == 1
    }

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
