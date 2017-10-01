package de.test.antennapod.storage

import android.content.Context
import android.test.InstrumentationTestCase

import java.util.ArrayList
import java.util.Date
import java.util.Random

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.storage.DBReader
import de.danoeh.antennapod.core.storage.FeedItemStatistics
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.core.util.LongList

import de.test.antennapod.storage.DBTestUtils.saveFeedlist

/**
 * Test class for DBReader
 */
class DBReaderTest : InstrumentationTestCase() {

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        Assert.assertTrue(PodDBAdapter.deleteDatabase())
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        // create new database
        PodDBAdapter.init(instrumentation.targetContext)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()
    }

    fun testGetFeedList() {
        val feeds = saveFeedlist(10, 0, false)
        val savedFeeds = DBReader.getFeedList()
        Assert.assertNotNull(savedFeeds)
        Assert.assertEquals(feeds.size, savedFeeds.size)
        for (i in feeds.indices) {
            Assert.assertTrue(savedFeeds[i].id == feeds[i].id)
        }
    }

    fun testGetFeedListSortOrder() {
        val adapter = PodDBAdapter.getInstance()
        adapter.open()

        val feed1 = Feed(0, null, "A", "link", "d", null, null, null, "rss", "A", null, "", "", true)
        val feed2 = Feed(0, null, "b", "link", "d", null, null, null, "rss", "b", null, "", "", true)
        val feed3 = Feed(0, null, "C", "link", "d", null, null, null, "rss", "C", null, "", "", true)
        val feed4 = Feed(0, null, "d", "link", "d", null, null, null, "rss", "d", null, "", "", true)
        adapter.setCompleteFeed(feed1)
        adapter.setCompleteFeed(feed2)
        adapter.setCompleteFeed(feed3)
        adapter.setCompleteFeed(feed4)
        Assert.assertTrue(feed1.id != 0)
        Assert.assertTrue(feed2.id != 0)
        Assert.assertTrue(feed3.id != 0)
        Assert.assertTrue(feed4.id != 0)

        adapter.close()

        val saved = DBReader.getFeedList()
        Assert.assertNotNull(saved)
        Assert.assertEquals("Wrong size: ", 4, saved.size)

        Assert.assertEquals("Wrong id of feed 1: ", feed1.id, saved[0].id)
        Assert.assertEquals("Wrong id of feed 2: ", feed2.id, saved[1].id)
        Assert.assertEquals("Wrong id of feed 3: ", feed3.id, saved[2].id)
        Assert.assertEquals("Wrong id of feed 4: ", feed4.id, saved[3].id)
    }

    fun testFeedListDownloadUrls() {
        val feeds = saveFeedlist(10, 0, false)
        val urls = DBReader.getFeedListDownloadUrls()
        Assert.assertNotNull(urls)
        Assert.assertTrue(urls.size == feeds.size)
        for (i in urls.indices) {
            Assert.assertEquals(urls[i], feeds[i].download_url)
        }
    }

    fun testLoadFeedDataOfFeedItemlist() {
        val context = instrumentation.targetContext
        val numFeeds = 10
        val numItems = 1
        val feeds = saveFeedlist(numFeeds, numItems, false)
        val items = ArrayList<FeedItem>()
        for (f in feeds) {
            for (item in f.items) {
                item.feed = null
                item.feedId = f.id
                items.add(item)
            }
        }
        DBReader.loadAdditionalFeedItemListData(items)
        for (i in 0..numFeeds - 1) {
            for (j in 0..numItems - 1) {
                val item = feeds[i].items[j]
                Assert.assertNotNull(item.feed)
                Assert.assertTrue(item.feed.id == feeds[i].id)
                Assert.assertTrue(item.feedId == item.feed.id)
            }
        }
    }

    fun testGetFeedItemList() {
        val numFeeds = 1
        val numItems = 10
        val feed = saveFeedlist(numFeeds, numItems, false)[0]
        val items = feed.items
        feed.items = null
        val savedItems = DBReader.getFeedItemList(feed)
        Assert.assertNotNull(savedItems)
        Assert.assertTrue(savedItems.size == items.size)
        for (i in savedItems.indices) {
            Assert.assertTrue(items[i].id == savedItems[i].id)
        }
    }

    private fun saveQueue(numItems: Int): List<FeedItem> {
        if (numItems <= 0) {
            throw IllegalArgumentException("numItems<=0")
        }
        val feeds = saveFeedlist(numItems, numItems, false)
        val allItems = ArrayList<FeedItem>()
        for (f in feeds) {
            allItems.addAll(f.items)
        }
        // take random items from every feed
        val random = Random()
        val queue = ArrayList<FeedItem>()
        while (queue.size < numItems) {
            val index = random.nextInt(numItems)
            if (!queue.contains(allItems[index])) {
                queue.add(allItems[index])
            }
        }
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setQueue(queue)
        adapter.close()
        return queue
    }

    fun testGetQueueIDList() {
        val numItems = 10
        val queue = saveQueue(numItems)
        val ids = DBReader.getQueueIDList()
        Assert.assertNotNull(ids)
        Assert.assertTrue(queue.size == ids.size())
        for (i in queue.indices) {
            Assert.assertTrue(ids.get(i) != 0)
            Assert.assertTrue(queue[i].id == ids.get(i))
        }
    }

    fun testGetQueue() {
        val numItems = 10
        val queue = saveQueue(numItems)
        val savedQueue = DBReader.getQueue()
        Assert.assertNotNull(savedQueue)
        Assert.assertTrue(queue.size == savedQueue.size)
        for (i in queue.indices) {
            Assert.assertTrue(savedQueue[i].id != 0)
            Assert.assertTrue(queue[i].id == savedQueue[i].id)
        }
    }

    private fun saveDownloadedItems(numItems: Int): List<FeedItem> {
        if (numItems <= 0) {
            throw IllegalArgumentException("numItems<=0")
        }
        val feeds = saveFeedlist(numItems, numItems, true)
        val items = ArrayList<FeedItem>()
        for (f in feeds) {
            items.addAll(f.items)
        }
        val downloaded = ArrayList<FeedItem>()
        val random = Random()

        while (downloaded.size < numItems) {
            val i = random.nextInt(numItems)
            if (!downloaded.contains(items[i])) {
                val item = items[i]
                item.media!!.isDownloaded = true
                item.media!!.file_url = "file" + i
                downloaded.add(item)
            }
        }
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setFeedItemlist(downloaded)
        adapter.close()
        return downloaded
    }

    fun testGetDownloadedItems() {
        val numItems = 10
        val downloaded = saveDownloadedItems(numItems)
        val downloaded_saved = DBReader.getDownloadedItems()
        Assert.assertNotNull(downloaded_saved)
        Assert.assertTrue(downloaded_saved.size == downloaded.size)
        for (item in downloaded_saved) {
            Assert.assertNotNull(item.media)
            Assert.assertTrue(item.media!!.isDownloaded)
            Assert.assertNotNull(item.media!!.download_url)
        }
    }

    private fun saveNewItems(numItems: Int): List<FeedItem> {
        val feeds = saveFeedlist(numItems, numItems, true)
        val items = ArrayList<FeedItem>()
        for (f in feeds) {
            items.addAll(f.items)
        }
        val newItems = ArrayList<FeedItem>()
        val random = Random()

        while (newItems.size < numItems) {
            val i = random.nextInt(numItems)
            if (!newItems.contains(items[i])) {
                val item = items[i]
                item.setNew()
                newItems.add(item)
            }
        }
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setFeedItemlist(newItems)
        adapter.close()
        return newItems
    }

    fun testGetNewItemIds() {
        val numItems = 10

        val newItems = saveNewItems(numItems)
        val unreadIds = LongArray(newItems.size)
        for (i in newItems.indices) {
            unreadIds[i] = newItems[i].id
        }
        val newItemsSaved = DBReader.getNewItemsList()
        Assert.assertNotNull(newItemsSaved)
        Assert.assertTrue(newItems.size == newItemsSaved.size)
        for (i in newItemsSaved.indices) {
            val savedId = newItemsSaved[i].id
            var found = false
            for (id in unreadIds) {
                if (id == savedId) {
                    found = true
                    break
                }
            }
            Assert.assertTrue(found)
        }
    }

    fun testGetPlaybackHistory() {
        val numItems = (DBReader.PLAYBACK_HISTORY_SIZE + 1) * 2
        val playedItems = DBReader.PLAYBACK_HISTORY_SIZE + 1
        val numReturnedItems = Math.min(playedItems, DBReader.PLAYBACK_HISTORY_SIZE)
        val numFeeds = 1

        val feed = DBTestUtils.saveFeedlist(numFeeds, numItems, true)[0]
        val ids = LongArray(playedItems)

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        for (i in 0..playedItems - 1) {
            val m = feed.items[i].media
            m!!.playbackCompletionDate = Date((i + 1).toLong())
            adapter.setFeedMediaPlaybackCompletionDate(m)
            ids[ids.size - 1 - i] = m.item!!.id
        }
        adapter.close()

        val saved = DBReader.getPlaybackHistory()
        Assert.assertNotNull(saved)
        Assert.assertEquals("Wrong size: ", numReturnedItems, saved.size)
        for (i in 0..numReturnedItems - 1) {
            val item = saved[i]
            Assert.assertNotNull(item.media!!.playbackCompletionDate)
            Assert.assertEquals("Wrong sort order: ", item.id, ids[i])
        }
    }

    fun testGetFeedStatisticsCheckOrder() {
        val NUM_FEEDS = 10
        val NUM_ITEMS = 10
        val feeds = DBTestUtils.saveFeedlist(NUM_FEEDS, NUM_ITEMS, false)
        val statistics = DBReader.getFeedStatisticsList()
        Assert.assertNotNull(statistics)
        Assert.assertEquals(feeds.size, statistics.size)
        for (i in 0..NUM_FEEDS - 1) {
            Assert.assertEquals("Wrong entry at index " + i, feeds[i].id, statistics[i].feedID)
        }
    }

    fun testGetNavDrawerDataQueueEmptyNoUnreadItems() {
        val NUM_FEEDS = 10
        val NUM_ITEMS = 10
        val feeds = DBTestUtils.saveFeedlist(NUM_FEEDS, NUM_ITEMS, true)
        val navDrawerData = DBReader.getNavDrawerData()
        Assert.assertEquals(NUM_FEEDS, navDrawerData.feeds.size)
        Assert.assertEquals(0, navDrawerData.numNewItems)
        Assert.assertEquals(0, navDrawerData.queueSize)
    }

    fun testGetNavDrawerDataQueueNotEmptyWithUnreadItems() {
        val NUM_FEEDS = 10
        val NUM_ITEMS = 10
        val NUM_QUEUE = 1
        val NUM_NEW = 2
        val feeds = DBTestUtils.saveFeedlist(NUM_FEEDS, NUM_ITEMS, true)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        for (i in 0..NUM_NEW - 1) {
            val item = feeds[0].items[i]
            item.setNew()
            adapter.setSingleFeedItem(item)
        }
        val queue = ArrayList<FeedItem>()
        for (i in 0..NUM_QUEUE - 1) {
            val item = feeds[1].items[i]
            queue.add(item)
        }
        adapter.setQueue(queue)

        adapter.close()

        val navDrawerData = DBReader.getNavDrawerData()
        Assert.assertEquals(NUM_FEEDS, navDrawerData.feeds.size)
        Assert.assertEquals(NUM_NEW, navDrawerData.numNewItems)
        Assert.assertEquals(NUM_QUEUE, navDrawerData.queueSize)
    }

    @Throws(Exception::class)
    fun testGetFeedItemlistCheckChaptersFalse() {
        val context = instrumentation.targetContext
        val feeds = DBTestUtils.saveFeedlist(10, 10, false, false, 0)
        for (feed in feeds) {
            for (item in feed.items) {
                Assert.assertFalse(item.hasChapters())
            }
        }
    }

    @Throws(Exception::class)
    fun testGetFeedItemlistCheckChaptersTrue() {
        val feeds = saveFeedlist(10, 10, false, true, 10)
        for (feed in feeds) {
            for (item in feed.items) {
                Assert.assertTrue(item.hasChapters())
            }
        }
    }

    @Throws(Exception::class)
    fun testLoadChaptersOfFeedItemNoChapters() {
        val feeds = saveFeedlist(1, 3, false, false, 0)
        saveFeedlist(1, 3, false, true, 3)
        for (feed in feeds) {
            for (item in feed.items) {
                Assert.assertFalse(item.hasChapters())
                DBReader.loadChaptersOfFeedItem(item)
                Assert.assertFalse(item.hasChapters())
                Assert.assertNull(item.chapters)
            }
        }
    }

    @Throws(Exception::class)
    fun testLoadChaptersOfFeedItemWithChapters() {
        val NUM_CHAPTERS = 3
        DBTestUtils.saveFeedlist(1, 3, false, false, 0)
        val feeds = saveFeedlist(1, 3, false, true, NUM_CHAPTERS)
        for (feed in feeds) {
            for (item in feed.items) {
                Assert.assertTrue(item.hasChapters())
                DBReader.loadChaptersOfFeedItem(item)
                Assert.assertTrue(item.hasChapters())
                Assert.assertNotNull(item.chapters)
                Assert.assertEquals(NUM_CHAPTERS, item.chapters.size)
            }
        }
    }

    @Throws(Exception::class)
    fun testGetItemWithChapters() {
        val NUM_CHAPTERS = 3
        val feeds = saveFeedlist(1, 1, false, true, NUM_CHAPTERS)
        val item1 = feeds[0].items[0]
        val item2 = DBReader.getFeedItem(item1.id)
        Assert.assertTrue(item2.hasChapters())
        Assert.assertEquals(item1.chapters, item2.chapters)
    }
}
