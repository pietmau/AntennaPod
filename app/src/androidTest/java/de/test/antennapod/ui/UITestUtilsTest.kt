package de.test.antennapod.ui

import android.test.InstrumentationTestCase

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem

/**
 * Test for the UITestUtils. Makes sure that all URLs are reachable and that the class does not cause any crashes.
 */
class UITestUtilsTest : InstrumentationTestCase() {

    private var uiTestUtils: UITestUtils? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        uiTestUtils = UITestUtils(instrumentation.targetContext)
        uiTestUtils!!.setup()
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        super.tearDown()
        uiTestUtils!!.tearDown()
    }

    @Throws(Exception::class)
    fun testAddHostedFeeds() {
        uiTestUtils!!.addHostedFeedData()
        val feeds = uiTestUtils!!.hostedFeeds
        Assert.assertNotNull(feeds)
        Assert.assertFalse(feeds.isEmpty())

        for (feed in feeds) {
            testUrlReachable(feed.download_url)
            if (feed.image != null) {
                testUrlReachable(feed.image.download_url)
            }
            for (item in feed.items) {
                if (item.hasMedia()) {
                    testUrlReachable(item.media!!.download_url)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun testUrlReachable(strUtl: String) {
        val url = URL(strUtl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connect()
        val rc = conn.responseCode
        Assert.assertEquals(HttpURLConnection.HTTP_OK, rc)
        conn.disconnect()
    }

    @Throws(Exception::class)
    private fun addLocalFeedDataCheck(downloadEpisodes: Boolean) {
        uiTestUtils!!.addLocalFeedData(downloadEpisodes)
        Assert.assertNotNull(uiTestUtils!!.hostedFeeds)
        Assert.assertFalse(uiTestUtils!!.hostedFeeds.isEmpty())

        for (feed in uiTestUtils!!.hostedFeeds) {
            Assert.assertTrue(feed.id != 0)
            if (feed.image != null) {
                Assert.assertTrue(feed.image.id != 0)
            }
            for (item in feed.items) {
                Assert.assertTrue(item.id != 0)
                if (item.hasMedia()) {
                    Assert.assertTrue(item.media!!.id != 0)
                    if (downloadEpisodes) {
                        Assert.assertTrue(item.media!!.isDownloaded)
                        Assert.assertNotNull(item.media!!.file_url)
                        val file = File(item.media!!.file_url)
                        Assert.assertTrue(file.exists())
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    fun testAddLocalFeedDataNoDownload() {
        addLocalFeedDataCheck(false)
    }

    @Throws(Exception::class)
    fun testAddLocalFeedDataDownload() {
        addLocalFeedDataCheck(true)
    }
}
