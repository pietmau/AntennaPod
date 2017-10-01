package de.test.antennapod.handler

import android.content.Context
import android.test.InstrumentationTestCase

import org.xml.sax.SAXException

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList
import java.util.Date

import javax.xml.parsers.ParserConfigurationException

import de.danoeh.antennapod.core.feed.Chapter
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedImage
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.syndication.handler.FeedHandler
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException
import de.test.antennapod.util.syndication.feedgenerator.AtomGenerator
import de.test.antennapod.util.syndication.feedgenerator.FeedGenerator
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator

/**
 * Tests for FeedHandler
 */
class FeedHandlerTest : InstrumentationTestCase() {

    internal var file: File? = null
    internal var outputStream: OutputStream? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val context = instrumentation.context
        val destDir = context.getExternalFilesDir(FEEDS_DIR)
        Assert.assertNotNull(destDir)

        file = File(destDir, "feed.xml")
        file!!.delete()

        Assert.assertNotNull(file)
        Assert.assertFalse(file!!.exists())

        outputStream = FileOutputStream(file!!)
    }


    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        file!!.delete()
        file = null

        outputStream!!.close()
        outputStream = null
    }

    @Throws(IOException::class, UnsupportedFeedtypeException::class, SAXException::class, ParserConfigurationException::class)
    private fun runFeedTest(feed: Feed, g: FeedGenerator, encoding: String, flags: Long): Feed {
        g.writeFeed(feed, outputStream, encoding, flags)
        val handler = FeedHandler()
        val parsedFeed = Feed(feed.download_url, feed.lastUpdate)
        parsedFeed.file_url = file!!.absolutePath
        parsedFeed.isDownloaded = true
        handler.parseFeed(parsedFeed)
        return parsedFeed
    }

    private fun feedValid(feed: Feed, parsedFeed: Feed, feedType: String) {
        Assert.assertEquals(feed.title, parsedFeed.title)
        if (feedType == Feed.TYPE_ATOM1) {
            Assert.assertEquals(feed.feedIdentifier, parsedFeed.feedIdentifier)
        } else {
            Assert.assertEquals(feed.language, parsedFeed.language)
        }

        Assert.assertEquals(feed.link, parsedFeed.link)
        Assert.assertEquals(feed.description, parsedFeed.description)
        Assert.assertEquals(feed.paymentLink, parsedFeed.paymentLink)

        if (feed.image != null) {
            val image = feed.image
            val parsedImage = parsedFeed.image
            Assert.assertNotNull(parsedImage)

            Assert.assertEquals(image.title, parsedImage.title)
            Assert.assertEquals(image.download_url, parsedImage.download_url)
        }

        if (feed.items != null) {
            Assert.assertNotNull(parsedFeed.items)
            Assert.assertEquals(feed.items.size, parsedFeed.items.size)

            for (i in 0..feed.items.size - 1) {
                val item = feed.items[i]
                val parsedItem = parsedFeed.items[i]

                if (item.itemIdentifier != null)
                    Assert.assertEquals(item.itemIdentifier, parsedItem.itemIdentifier)
                Assert.assertEquals(item.title, parsedItem.title)
                Assert.assertEquals(item.description, parsedItem.description)
                Assert.assertEquals(item.contentEncoded, parsedItem.contentEncoded)
                Assert.assertEquals(item.link, parsedItem.link)
                Assert.assertEquals(item.pubDate.time, parsedItem.pubDate.time)
                Assert.assertEquals(item.paymentLink, parsedItem.paymentLink)

                if (item.hasMedia()) {
                    Assert.assertTrue(parsedItem.hasMedia())
                    val media = item.media
                    val parsedMedia = parsedItem.media

                    Assert.assertEquals(media!!.download_url, parsedMedia!!.download_url)
                    Assert.assertEquals(media.size, parsedMedia.size)
                    Assert.assertEquals(media.mime_type, parsedMedia.mime_type)
                }

                if (item.hasItemImage()) {
                    Assert.assertTrue(parsedItem.hasItemImage())
                    val image = item.image
                    val parsedImage = parsedItem.image

                    Assert.assertEquals(image.title, parsedImage.title)
                    Assert.assertEquals(image.download_url, parsedImage.download_url)
                }

                if (item.chapters != null) {
                    Assert.assertNotNull(parsedItem.chapters)
                    Assert.assertEquals(item.chapters.size, parsedItem.chapters.size)
                    val chapters = item.chapters
                    val parsedChapters = parsedItem.chapters
                    for (j in chapters.indices) {
                        val chapter = chapters[j]
                        val parsedChapter = parsedChapters[j]

                        Assert.assertEquals(chapter.title, parsedChapter.title)
                        Assert.assertEquals(chapter.link, parsedChapter.link)
                    }
                }
            }
        }
    }

    @Throws(IOException::class, UnsupportedFeedtypeException::class, SAXException::class, ParserConfigurationException::class)
    fun testRSS2Basic() {
        val f1 = createTestFeed(10, false, true, true)
        val f2 = runFeedTest(f1, RSS2Generator(), "UTF-8", RSS2Generator.FEATURE_WRITE_GUID)
        feedValid(f1, f2, Feed.TYPE_RSS2)
    }

    @Throws(IOException::class, UnsupportedFeedtypeException::class, SAXException::class, ParserConfigurationException::class)
    fun testAtomBasic() {
        val f1 = createTestFeed(10, false, true, true)
        val f2 = runFeedTest(f1, AtomGenerator(), "UTF-8", 0)
        feedValid(f1, f2, Feed.TYPE_ATOM1)
    }

    private fun createTestFeed(numItems: Int, withImage: Boolean, withFeedMedia: Boolean, withChapters: Boolean): Feed {
        var image: FeedImage? = null
        if (withImage) {
            image = FeedImage(0, "image", null, "http://example.com/picture", false)
        }
        val feed = Feed(0, null, "title", "http://example.com", "This is the description",
                "http://example.com/payment", "Daniel", "en", null, "http://example.com/feed", image, file!!.absolutePath,
                "http://example.com/feed", true)
        feed.items = ArrayList<FeedItem>()

        for (i in 0..numItems - 1) {
            val item = FeedItem(0, "item-" + i, "http://example.com/item-" + i,
                    "http://example.com/items/" + i, Date((i * 60000).toLong()), FeedItem.UNPLAYED, feed)
            feed.items.add(item)
            if (withFeedMedia) {
                item.media = FeedMedia(0, item, 4711, 0, (1024 * 1024).toLong(), "audio/mp3", null, "http://example.com/media-" + i,
                        false, null, 0, 0)
            }
        }

        return feed
    }

    companion object {
        private val FEEDS_DIR = "testfeeds"
    }

}
