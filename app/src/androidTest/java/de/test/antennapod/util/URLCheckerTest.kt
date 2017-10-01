package de.test.antennapod.util

import android.test.AndroidTestCase
import de.danoeh.antennapod.core.util.URLChecker

/**
 * Test class for URLChecker
 */
class URLCheckerTest : AndroidTestCase() {

    fun testCorrectURLHttp() {
        val `in` = "http://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals(`in`, out)
    }

    fun testCorrectURLHttps() {
        val `in` = "https://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals(`in`, out)
    }

    fun testMissingProtocol() {
        val `in` = "example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testFeedProtocol() {
        val `in` = "feed://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testPcastProtocolNoScheme() {
        val `in` = "pcast://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testItpcProtocol() {
        val `in` = "itpc://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testWhiteSpaceUrlShouldNotAppend() {
        val `in` = "\n http://example.com \t"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testWhiteSpaceShouldAppend() {
        val `in` = "\n example.com \t"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    @Throws(Exception::class)
    fun testAntennaPodSubscribeProtocolNoScheme() {
        val `in` = "antennapod-subscribe://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("http://example.com", out)
    }

    fun testPcastProtocolWithScheme() {
        val `in` = "pcast://https://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("https://example.com", out)
    }

    @Throws(Exception::class)
    fun testAntennaPodSubscribeProtocolWithScheme() {
        val `in` = "antennapod-subscribe://https://example.com"
        val out = URLChecker.prepareURL(`in`)
        Assert.assertEquals("https://example.com", out)
    }

    @Throws(Exception::class)
    fun testProtocolRelativeUrlIsAbsolute() {
        val `in` = "https://example.com"
        val inBase = "http://examplebase.com"
        val out = URLChecker.prepareURL(`in`, inBase)
        Assert.assertEquals(`in`, out)
    }

    @Throws(Exception::class)
    fun testProtocolRelativeUrlIsRelativeHttps() {
        val `in` = "//example.com"
        val inBase = "https://examplebase.com"
        val out = URLChecker.prepareURL(`in`, inBase)
        Assert.assertEquals("https://example.com", out)

    }

    @Throws(Exception::class)
    fun testProtocolRelativeUrlIsHttpsWithAPSubscribeProtocol() {
        val `in` = "//example.com"
        val inBase = "antennapod-subscribe://https://examplebase.com"
        val out = URLChecker.prepareURL(`in`, inBase)
        Assert.assertEquals("https://example.com", out)
    }

    @Throws(Exception::class)
    fun testProtocolRelativeUrlBaseUrlNull() {
        val `in` = "example.com"
        val out = URLChecker.prepareURL(`in`, null)
        Assert.assertEquals("http://example.com", out)
    }
}
