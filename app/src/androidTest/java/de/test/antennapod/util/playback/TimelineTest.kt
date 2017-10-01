package de.test.antennapod.util.playback

import android.content.Context
import android.test.InstrumentationTestCase

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.util.Date

import de.danoeh.antennapod.core.feed.Chapter
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.util.playback.Playable
import de.danoeh.antennapod.core.util.playback.Timeline

/**
 * Test class for timeline
 */
class TimelineTest : InstrumentationTestCase() {

    private var context: Context? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        context = instrumentation.targetContext
    }

    private fun newTestPlayable(chapters: List<Chapter>?, shownotes: String): Playable {
        val item = FeedItem(0, "Item", "item-id", "http://example.com/item", Date(), FeedItem.PLAYED, null)
        item.chapters = chapters
        item.contentEncoded = shownotes
        val media = FeedMedia(item, "http://example.com/episode", 100, "audio/mp3")
        media.duration = Integer.MAX_VALUE
        item.media = media
        return media
    }

    @Throws(Exception::class)
    fun testProcessShownotesAddTimecodeHHMMSSNoChapters() {
        val timeStr = "10:11:12"
        val time = (3600 * 1000 * 10 + 60 * 1000 * 11 + 12 * 1000).toLong()

        val p = newTestPlayable(null, "<p> Some test text with a timecode $timeStr here.</p>")
        val t = Timeline(context, p)
        val res = t.processShownotes(true)
        checkLinkCorrect(res, longArrayOf(time), arrayOf(timeStr))
    }

    @Throws(Exception::class)
    fun testProcessShownotesAddTimecodeHHMMNoChapters() {
        val timeStr = "10:11"
        val time = (3600 * 1000 * 10 + 60 * 1000 * 11).toLong()

        val p = newTestPlayable(null, "<p> Some test text with a timecode $timeStr here.</p>")
        val t = Timeline(context, p)
        val res = t.processShownotes(true)
        checkLinkCorrect(res, longArrayOf(time), arrayOf(timeStr))
    }

    @Throws(Exception::class)
    fun testProcessShownotesAddTimecodeParentheses() {
        val timeStr = "10:11"
        val time = (3600 * 1000 * 10 + 60 * 1000 * 11).toLong()

        val p = newTestPlayable(null, "<p> Some test text with a timecode ($timeStr) here.</p>")
        val t = Timeline(context, p)
        val res = t.processShownotes(true)
        checkLinkCorrect(res, longArrayOf(time), arrayOf(timeStr))
    }

    @Throws(Exception::class)
    fun testProcessShownotesAddTimecodeBrackets() {
        val timeStr = "10:11"
        val time = (3600 * 1000 * 10 + 60 * 1000 * 11).toLong()

        val p = newTestPlayable(null, "<p> Some test text with a timecode [$timeStr] here.</p>")
        val t = Timeline(context, p)
        val res = t.processShownotes(true)
        checkLinkCorrect(res, longArrayOf(time), arrayOf(timeStr))
    }

    @Throws(Exception::class)
    fun testProcessShownotesAddTimecodeAngleBrackets() {
        val timeStr = "10:11"
        val time = (3600 * 1000 * 10 + 60 * 1000 * 11).toLong()

        val p = newTestPlayable(null, "<p> Some test text with a timecode <$timeStr> here.</p>")
        val t = Timeline(context, p)
        val res = t.processShownotes(true)
        checkLinkCorrect(res, longArrayOf(time), arrayOf(timeStr))
    }

    private fun checkLinkCorrect(res: String, timecodes: LongArray, timecodeStr: Array<String>) {
        Assert.assertNotNull(res)
        val d = Jsoup.parse(res)
        val links = d.body().getElementsByTag("a")
        var countedLinks = 0
        for (link in links) {
            val href = link.attributes().get("href")
            val text = link.text()
            if (href.startsWith("antennapod://")) {
                Assert.assertTrue(href.endsWith(timecodes[countedLinks].toString()))
                Assert.assertEquals(timecodeStr[countedLinks], text)
                countedLinks++
                Assert.assertTrue("Contains too many links: " + countedLinks + " > " + timecodes.size, countedLinks <= timecodes.size)
            }
        }
        Assert.assertEquals(timecodes.size, countedLinks)
    }

    @Throws(Exception::class)
    fun testIsTimecodeLink() {
        Assert.assertFalse(Timeline.isTimecodeLink(null))
        Assert.assertFalse(Timeline.isTimecodeLink("http://antennapod/timecode/123123"))
        Assert.assertFalse(Timeline.isTimecodeLink("antennapod://timecode/"))
        Assert.assertFalse(Timeline.isTimecodeLink("antennapod://123123"))
        Assert.assertFalse(Timeline.isTimecodeLink("antennapod://timecode/123123a"))
        Assert.assertTrue(Timeline.isTimecodeLink("antennapod://timecode/123"))
        Assert.assertTrue(Timeline.isTimecodeLink("antennapod://timecode/1"))
    }

    @Throws(Exception::class)
    fun testGetTimecodeLinkTime() {
        Assert.assertEquals(-1, Timeline.getTimecodeLinkTime(null))
        Assert.assertEquals(-1, Timeline.getTimecodeLinkTime("http://timecode/123"))
        Assert.assertEquals(123, Timeline.getTimecodeLinkTime("antennapod://timecode/123"))

    }
}
