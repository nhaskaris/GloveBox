package com.eliteonetube.glovebox.util

import org.junit.Assert.assertEquals
import org.junit.Test

class OdometerValidatorTest {
    private val day = 86_400_000L
    private val history = listOf(10 * day to 10_000, 20 * day to 10_500)

    @Test
    fun sensibleReadingsPass() {
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(10_800, 30 * day, history))
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(10_200, 15 * day, history))
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(500, 1 * day, history)) // before everything
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(500, 1 * day, emptyList()))
    }

    @Test
    fun lowerThanPreviousIsFlagged() {
        assertEquals(OdometerCheck.LowerThanPrevious(10_500), OdometerValidator.check(10_000, 30 * day, history))
    }

    @Test
    fun higherThanALaterEntryIsFlagged() {
        assertEquals(OdometerCheck.HigherThanNext(10_500), OdometerValidator.check(10_900, 15 * day, history))
    }

    @Test
    fun hugeJumpIsFlagged_butLongGapsAreFine() {
        assertEquals(OdometerCheck.ImplausibleJump(10_500, 90_000), OdometerValidator.check(100_500, 21 * day, history))
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(40_500, 20 * day + 400 * day, history))
    }

    @Test
    fun zeroReadingsAreIgnored() {
        assertEquals(OdometerCheck.Ok, OdometerValidator.check(10_100, 30 * day, listOf(20 * day to 0)))
    }
}
