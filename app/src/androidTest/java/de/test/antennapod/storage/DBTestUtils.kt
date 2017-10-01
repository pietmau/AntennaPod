package de.test.antennapod.storage

import junit.framework.Assert

import java.util.ArrayList
import java.util.Collections
import java.util.Date

import de.danoeh.antennapod.core.feed.Chapter
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.feed.SimpleChapter
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator
import de.danoeh.antennapod.core.util.flattr.FlattrStatus

/**
 * Utility methods for DB* tests.
 */
object DBTestUtils {

    /**
     * Use this method when tests involve chapters.
     */
    @JvmOverloads fun saveFeedlist(numFeeds: Int, numItems: Int, withMedia: Boolean,
                                   withChapters: Boolean = false, numChapters: Int = 0): List<Feed> {
        if (numFeeds <= 0) {
            throw IllegalArgumentException("numFeeds<=0")
        }
        if (numItems < 0) {
            throw IllegalArgumentException("numItems<0")
        }

        val feeds = ArrayList<Feed>()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        for (i in 0..numFeeds - 1) {
            val f = Feed(0, null, "feed " + i, null, "link" + i, "descr", null, null, null, null, "id" + i, null, null, "url" + i, false, FlattrStatus(), false, null, null, false)
            f.items = ArrayList<FeedItem>()
            for (j in 0..numItems - 1) {
                val item = FeedItem(0, "item " + j, "id" + j, "link" + j, Date(),
                        FeedItem.PLAYED, f, withChapters)
                if (withMedia) {
                    val media = FeedMedia(item, "url" + j, 1, "audio/mp3")
                    item.setMedia(media)
                }
                if (withChapters) {
                    val chapters = ArrayList<Chapter>()
                    item.chapters = chapters
                    for (k in 0..numChapters - 1) {
                        chapters.add(SimpleChapter(k.toLong(), "item $j chapter $k", item, "http://example.com"))
                    }
                }
                f.items.add(item)
            }
            Collections.sort(f.items, FeedItemPubdateComparator())
            adapter.setCompleteFeed(f)
            Assert.assertTrue(f.id != 0)
            for (item in f.items) {
                Assert.assertTrue(item.id != 0)
            }
            feeds.add(f)
        }
        adapter.close()

        return feeds
    }
}
/**
 * Use this method when tests don't involve chapters.
 */
