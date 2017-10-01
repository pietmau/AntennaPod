package de.test.antennapod.util.syndication

import android.test.InstrumentationTestCase
import de.danoeh.antennapod.core.util.syndication.FeedDiscoverer
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.io.File
import java.io.FileOutputStream

/**
 * Test class for FeedDiscoverer
 */
class FeedDiscovererTest : InstrumentationTestCase() {

    private var fd: FeedDiscoverer? = null

    private var testDir: File? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        fd = FeedDiscoverer()
        testDir = instrumentation.targetContext.getExternalFilesDir("FeedDiscovererTest")
        testDir!!.mkdir()
        Assert.assertTrue(testDir!!.exists())
    }

    @Throws(Exception::class)
    override fun tearDown() {
        FileUtils.deleteDirectory(testDir!!)
        super.tearDown()
    }

    private fun createTestHtmlString(rel: String, type: String, href: String, title: String): String {
        return String.format("<html><head><title>Test</title><link rel=\"%s\" type=\"%s\" href=\"%s\" title=\"%s\"></head><body></body></html>",
                rel, type, href, title)
    }

    private fun createTestHtmlString(rel: String, type: String, href: String): String {
        return String.format("<html><head><title>Test</title><link rel=\"%s\" type=\"%s\" href=\"%s\"></head><body></body></html>",
                rel, type, href)
    }

    @Throws(Exception::class)
    private fun checkFindUrls(isAlternate: Boolean, isRss: Boolean, withTitle: Boolean, isAbsolute: Boolean, fromString: Boolean) {
        val title = "Test title"
        val hrefAbs = "http://example.com/feed"
        val hrefRel = "/feed"
        val base = "http://example.com"

        val rel = if (isAlternate) "alternate" else "feed"
        val type = if (isRss) "application/rss+xml" else "application/atom+xml"
        val href = if (isAbsolute) hrefAbs else hrefRel

        val res: Map<String, String>
        val html = if (withTitle)
            createTestHtmlString(rel, type, href, title)
        else
            createTestHtmlString(rel, type, href)
        if (fromString) {
            res = fd!!.findLinks(html, base)
        } else {
            val testFile = File(testDir, "feed")
            val out = FileOutputStream(testFile)
            IOUtils.write(html, out)
            out.close()
            res = fd!!.findLinks(testFile, base)
        }

        Assert.assertNotNull(res)
        Assert.assertEquals(1, res.size)
        for (key in res.keys) {
            Assert.assertEquals(hrefAbs, key)
        }
        Assert.assertTrue(res.containsKey(hrefAbs))
        if (withTitle) {
            Assert.assertEquals(title, res[hrefAbs])
        } else {
            Assert.assertEquals(href, res[hrefAbs])
        }
    }

    @Throws(Exception::class)
    fun testAlternateRSSWithTitleAbsolute() {
        checkFindUrls(true, true, true, true, true)
    }

    @Throws(Exception::class)
    fun testAlternateRSSWithTitleRelative() {
        checkFindUrls(true, true, true, false, true)
    }

    @Throws(Exception::class)
    fun testAlternateRSSNoTitleAbsolute() {
        checkFindUrls(true, true, false, true, true)
    }

    @Throws(Exception::class)
    fun testAlternateRSSNoTitleRelative() {
        checkFindUrls(true, true, false, false, true)
    }

    @Throws(Exception::class)
    fun testAlternateAtomWithTitleAbsolute() {
        checkFindUrls(true, false, true, true, true)
    }

    @Throws(Exception::class)
    fun testFeedAtomWithTitleAbsolute() {
        checkFindUrls(false, false, true, true, true)
    }

    @Throws(Exception::class)
    fun testAlternateRSSWithTitleAbsoluteFromFile() {
        checkFindUrls(true, true, true, true, false)
    }
}
