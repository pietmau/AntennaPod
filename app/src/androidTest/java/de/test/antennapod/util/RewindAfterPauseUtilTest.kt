package de.test.antennapod.util

import junit.framework.*

import de.danoeh.antennapod.core.util.*

/**
 * Tests for [RewindAfterPauseUtils].
 */
class RewindAfterPauseUtilTest : TestCase() {

    fun testCalculatePositionWithRewindNoRewind() {
        val ORIGINAL_POSITION = 10000
        val lastPlayed = System.currentTimeMillis()
        val position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed)

        Assert.assertEquals(ORIGINAL_POSITION, position)
    }

    fun testCalculatePositionWithRewindSmallRewind() {
        val ORIGINAL_POSITION = 10000
        val lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_SHORT_REWIND - 1000
        val position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed)

        Assert.assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.SHORT_REWIND, position.toLong())
    }

    fun testCalculatePositionWithRewindMediumRewind() {
        val ORIGINAL_POSITION = 10000
        val lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_MEDIUM_REWIND - 1000
        val position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed)

        Assert.assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.MEDIUM_REWIND, position.toLong())
    }

    fun testCalculatePositionWithRewindLongRewind() {
        val ORIGINAL_POSITION = 30000
        val lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000
        val position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed)

        Assert.assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.LONG_REWIND, position.toLong())
    }

    fun testCalculatePositionWithRewindNegativeNumber() {
        val ORIGINAL_POSITION = 100
        val lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000
        val position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed)

        Assert.assertEquals(0, position)
    }
}
