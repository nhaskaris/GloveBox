package com.eliteonetube.glovebox.util

import java.util.concurrent.TimeUnit

sealed interface OdometerCheck {
    data object Ok : OdometerCheck
    /** The reading is lower than an earlier entry's. */
    data class LowerThanPrevious(val previous: Int) : OdometerCheck
    /** The reading is higher than a later entry's. */
    data class HigherThanNext(val next: Int) : OdometerCheck
    /** The reading implies an unrealistic distance since the previous entry. */
    data class ImplausibleJump(val previous: Int, val distance: Int) : OdometerCheck
}

object OdometerValidator {
    private const val MAX_PER_DAY = 1500       // km or mi
    private const val MIN_FLAGGED_JUMP = 5000  // ignore small jumps even after a long gap

    /**
     * @param existing (timestamp, odometer) readings of the same vehicle, excluding the entry
     *                 being edited. Readings of 0 or below are ignored.
     */
    fun check(reading: Int, date: Long, existing: List<Pair<Long, Int>>): OdometerCheck {
        val readings = existing.filter { it.second > 0 }
        val previous = readings.filter { it.first <= date }.maxByOrNull { it.first }
        val next = readings.filter { it.first > date }.minByOrNull { it.first }

        if (previous != null && reading < previous.second) return OdometerCheck.LowerThanPrevious(previous.second)
        if (next != null && reading > next.second) return OdometerCheck.HigherThanNext(next.second)

        if (previous != null) {
            val distance = reading - previous.second
            val days = maxOf(1L, TimeUnit.MILLISECONDS.toDays(date - previous.first))
            if (distance >= MIN_FLAGGED_JUMP && distance > days * MAX_PER_DAY) {
                return OdometerCheck.ImplausibleJump(previous.second, distance)
            }
        }
        return OdometerCheck.Ok
    }
}
