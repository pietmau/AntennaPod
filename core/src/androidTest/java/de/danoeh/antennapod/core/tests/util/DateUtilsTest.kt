package de.danoeh.antennapod.core.tests.util


import android.test.AndroidTestCase
import de.danoeh.antennapod.core.util.DateUtils
import junit.framework.Assert
import java.util.*

class DateUtilsTest : AndroidTestCase() {

    @Throws(Exception::class)
    fun testParseDateWithMicroseconds() {
        val exp = GregorianCalendar(2015, 2, 28, 13, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 963)
        val actual = DateUtils.parse("2015-03-28T13:31:04.963870")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithCentiseconds() {
        val exp = GregorianCalendar(2015, 2, 28, 13, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 960)
        val actual = DateUtils.parse("2015-03-28T13:31:04.96")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithDeciseconds() {
        val exp = GregorianCalendar(2015, 2, 28, 13, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 900)
        val actual = DateUtils.parse("2015-03-28T13:31:04.9")
        Assert.assertEquals(expected.time / 1000, actual!!.time / 1000)
        Assert.assertEquals(900, actual.time % 1000)
    }

    @Throws(Exception::class)
    fun testParseDateWithMicrosecondsAndTimezone() {
        val exp = GregorianCalendar(2015, 2, 28, 6, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 963)
        val actual = DateUtils.parse("2015-03-28T13:31:04.963870 +0700")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithCentisecondsAndTimezone() {
        val exp = GregorianCalendar(2015, 2, 28, 6, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 960)
        val actual = DateUtils.parse("2015-03-28T13:31:04.96 +0700")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithDecisecondsAndTimezone() {
        val exp = GregorianCalendar(2015, 2, 28, 6, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 900)
        val actual = DateUtils.parse("2015-03-28T13:31:04.9 +0700")
        Assert.assertEquals(expected.time / 1000, actual!!.time / 1000)
        Assert.assertEquals(900, actual.time % 1000)
    }

    @Throws(Exception::class)
    fun testParseDateWithTimezoneName() {
        val exp = GregorianCalendar(2015, 2, 28, 6, 31, 4)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis)
        val actual = DateUtils.parse("Sat, 28 Mar 2015 01:31:04 EST")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithTimezoneName2() {
        val exp = GregorianCalendar(2015, 2, 28, 6, 31, 0)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis)
        val actual = DateUtils.parse("Sat, 28 Mar 2015 01:31 EST")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithTimeZoneOffset() {
        val exp = GregorianCalendar(2015, 2, 28, 12, 16, 12)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis)
        val actual = DateUtils.parse("Sat, 28 March 2015 08:16:12 -0400")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testAsctime() {
        val exp = GregorianCalendar(2011, 4, 25, 12, 33, 0)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis)
        val actual = DateUtils.parse("Wed, 25 May 2011 12:33:00")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testMultipleConsecutiveSpaces() {
        val exp = GregorianCalendar(2010, 2, 23, 6, 6, 26)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis)
        val actual = DateUtils.parse("Tue,  23 Mar   2010 01:06:26 -0500")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithNoTimezonePadding() {
        val exp = GregorianCalendar(2017, 1, 22, 22, 28, 0)
        exp.timeZone = TimeZone.getTimeZone("UTC")
        val expected = Date(exp.timeInMillis + 2)
        val actual = DateUtils.parse("2017-02-22T14:28:00.002-08:00")
        Assert.assertEquals(expected, actual)
    }

    @Throws(Exception::class)
    fun testParseDateWithForCest() {
        val exp1 = GregorianCalendar(2017, 0, 28, 22, 0, 0)
        exp1.timeZone = TimeZone.getTimeZone("UTC")
        val expected1 = Date(exp1.timeInMillis)
        val actual1 = DateUtils.parse("Sun, 29 Jan 2017 00:00:00 CEST")
        Assert.assertEquals(expected1, actual1)

        val exp2 = GregorianCalendar(2017, 0, 28, 23, 0, 0)
        exp2.timeZone = TimeZone.getTimeZone("UTC")
        val expected2 = Date(exp2.timeInMillis)
        val actual2 = DateUtils.parse("Sun, 29 Jan 2017 00:00:00 CET")
        Assert.assertEquals(expected2, actual2)
    }

    fun testParseDateWithIncorrectWeekday() {
        val exp1 = GregorianCalendar(2014, 9, 8, 9, 0, 0)
        exp1.timeZone = TimeZone.getTimeZone("GMT")
        val expected = Date(exp1.timeInMillis)
        val actual = DateUtils.parse("Thu, 8 Oct 2014 09:00:00 GMT") // actually a Wednesday
        Assert.assertEquals(expected, actual)
    }
}
