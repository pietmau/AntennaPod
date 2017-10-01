package de.test.antennapod.util

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.util.Converter
import junit.framework.Assert

/**
 * Test class for converter
 */
class ConverterTest : AndroidTestCase() {

    @Throws(Exception::class)
    fun testGetDurationStringLong() {
        val expected = "13:05:10"
        val input = 47110000
        Assert.assertEquals(expected, Converter.getDurationStringLong(input))
    }

    @Throws(Exception::class)
    fun testGetDurationStringShort() {
        val expected = "13:05"
        val input = 47110000
        Assert.assertEquals(expected, Converter.getDurationStringShort(input))
    }

    @Throws(Exception::class)
    fun testDurationStringLongToMs() {
        val input = "01:20:30"
        val expected: Long = 4830000
        Assert.assertEquals(expected, Converter.durationStringLongToMs(input).toLong())
    }

    @Throws(Exception::class)
    fun testDurationStringShortToMs() {
        val input = "8:30"
        val expected: Long = 30600000
        Assert.assertEquals(expected, Converter.durationStringShortToMs(input).toLong())
    }
}
