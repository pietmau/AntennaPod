package de.test.antennapod.storage

import android.content.Context
import android.test.FlakyTest
import android.test.InstrumentationTestCase

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.storage.DBReader
import de.danoeh.antennapod.core.storage.DBTasks
import de.danoeh.antennapod.core.storage.PodDBAdapter

/**
 * Test class for DBTasks
 */
class DBTasksTest : InstrumentationTestCase() {

    private var context: Context? = null

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        Assert.assertTrue(PodDBAdapter.deleteDatabase())
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        context = instrumentation.targetContext

        // create new database
        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()

        UserPreferences.init(context!!)
    }

    @FlakyTest(tolerance = 3)
    fun testUpdateFeedNewFeed() {
        val NUM_ITEMS = 10

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            feed.items.add(FeedItem(0, "item " + i, "id " + i, "link " + i, Date(), FeedItem.UNPLAYED, feed))
        }
        val newFeed = DBTasks.updateFeed(context, feed)[0]

        Assert.assertTrue(newFeed === feed)
        Assert.assertTrue(feed.id != 0)
        for (item in feed.items) {
            Assert.assertFalse(item.isPlayed)
            Assert.assertTrue(item.id != 0)
        }
    }

    /** Two feeds with the same title, but different download URLs should be treated as different feeds.  */
    fun testUpdateFeedSameTitle() {

        val feed1 = Feed("url1", null, "title")
        val feed2 = Feed("url2", null, "title")

        feed1.items = ArrayList<FeedItem>()
        feed2.items = ArrayList<FeedItem>()

        val savedFeed1 = DBTasks.updateFeed(context, feed1)[0]
        val savedFeed2 = DBTasks.updateFeed(context, feed2)[0]

        Assert.assertTrue(savedFeed1.id != savedFeed2.id)
    }

    fun testUpdateFeedUpdatedFeed() {
        val NUM_ITEMS_OLD = 10
        val NUM_ITEMS_NEW = 10

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS_OLD - 1) {
            feed.items.add(FeedItem(0, "item " + i, "id " + i, "link " + i, Date(i.toLong()), FeedItem.PLAYED, feed))
        }
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        // ensure that objects have been saved in db, then reset
        Assert.assertTrue(feed.id != 0)
        val feedID = feed.id
        feed.id = 0
        val itemIDs = ArrayList<Long>()
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            itemIDs.add(item.id)
            item.id = 0
        }

        for (i in NUM_ITEMS_OLD..NUM_ITEMS_NEW + NUM_ITEMS_OLD - 1) {
            feed.items.add(0, FeedItem(0, "item " + i, "id " + i, "link " + i, Date(i.toLong()), FeedItem.UNPLAYED, feed))
        }

        val newFeed = DBTasks.updateFeed(context, feed)[0]
        Assert.assertTrue(feed !== newFeed)

        updatedFeedTest(newFeed, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW)

        val feedFromDB = DBReader.getFeed(newFeed.id)
        Assert.assertNotNull(feedFromDB)
        Assert.assertTrue(feedFromDB.id == newFeed.id)
        updatedFeedTest(feedFromDB, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW)
    }

    fun testUpdateFeedMediaUrlResetState() {
        val feed = Feed("url", null, "title")
        val item = FeedItem(0, "item", "id", "link", Date(), FeedItem.PLAYED, feed)
        feed.items = Arrays.asList(item)

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        // ensure that objects have been saved in db, then reset
        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(item.id != 0)

        val media = FeedMedia(item, "url", 1024, "mime/type")
        item.setMedia(media)
        feed.items = Arrays.asList(item)

        val newFeed = DBTasks.updateFeed(context, feed)[0]
        Assert.assertTrue(feed !== newFeed)

        val feedFromDB = DBReader.getFeed(newFeed.id)
        val feedItemFromDB = feedFromDB.items[0]
        Assert.assertTrue("state: " + feedItemFromDB.state, feedItemFromDB.isNew)
    }

    private fun updatedFeedTest(newFeed: Feed, feedID: Long, itemIDs: List<Long>, NUM_ITEMS_OLD: Int, NUM_ITEMS_NEW: Int) {
        Assert.assertTrue(newFeed.id == feedID)
        Assert.assertTrue(newFeed.items.size == NUM_ITEMS_NEW + NUM_ITEMS_OLD)
        Collections.reverse(newFeed.items)
        var lastDate = Date(0)
        for (i in 0..NUM_ITEMS_OLD - 1) {
            val item = newFeed.items[i]
            Assert.assertTrue(item.feed === newFeed)
            Assert.assertTrue(item.id == itemIDs[i])
            Assert.assertTrue(item.isPlayed)
            Assert.assertTrue(item.pubDate.time >= lastDate.time)
            lastDate = item.pubDate
        }
        for (i in NUM_ITEMS_OLD..NUM_ITEMS_NEW + NUM_ITEMS_OLD - 1) {
            val item = newFeed.items[i]
            Assert.assertTrue(item.feed === newFeed)
            Assert.assertTrue(item.id != 0)
            Assert.assertFalse(item.isPlayed)
            Assert.assertTrue(item.pubDate.time >= lastDate.time)
            lastDate = item.pubDate
        }
    }

    companion object {

        private val TAG = "DBTasksTest"
    }
}
