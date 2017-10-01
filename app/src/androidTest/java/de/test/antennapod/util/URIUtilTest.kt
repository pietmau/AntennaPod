package de.test.antennapod.util

import android.test.AndroidTestCase
import de.danoeh.antennapod.core.util.URIUtil

/**
 * Test class for URIUtil
 */
class URIUtilTest : AndroidTestCase() {

    fun testGetURIFromRequestUrlShouldNotEncode() {
        val testUrl = "http://example.com/this%20is%20encoded"
        Assert.assertEquals(testUrl, URIUtil.getURIFromRequestUrl(testUrl).toString())
    }

    fun testGetURIFromRequestUrlShouldEncode() {
        val testUrl = "http://example.com/this is not encoded"
        val expected = "http://example.com/this%20is%20not%20encoded"
        Assert.assertEquals(expected, URIUtil.getURIFromRequestUrl(testUrl).toString())
    }
}
