package de.test.antennapod.storage

import android.content.Context
import android.database.Cursor
import android.test.InstrumentationTestCase
import android.util.Log

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import de.danoeh.antennapod.core.feed.Chapter
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedImage
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.feed.SimpleChapter
import de.danoeh.antennapod.core.storage.DBReader
import de.danoeh.antennapod.core.storage.DBWriter
import de.danoeh.antennapod.core.storage.PodDBAdapter

/**
 * Test class for DBWriter
 */
class DBWriterTest : InstrumentationTestCase() {

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()

        Assert.assertTrue(PodDBAdapter.deleteDatabase())

        val context = instrumentation.targetContext
        val testDir = context.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(testDir)
        for (f in testDir!!.listFiles()) {
            f.delete()
        }
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

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testSetFeedMediaPlaybackInformation() {
        val POSITION = 50
        val LAST_PLAYED_TIME: Long = 1000
        val PLAYED_DURATION = 60
        val DURATION = 100

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val item = FeedItem(0, "Item", "Item", "url", Date(), FeedItem.PLAYED, feed)
        items.add(item)
        val media = FeedMedia(0, item, DURATION, 1, 1, "mime_type", "dummy path", "download_url", true, null, 0, 0)
        item.setMedia(media)

        DBWriter.setFeedItem(item).get(TIMEOUT, TimeUnit.SECONDS)

        media.position = POSITION
        media.lastPlayedTime = LAST_PLAYED_TIME
        media.playedDuration = PLAYED_DURATION

        DBWriter.setFeedMediaPlaybackInformation(item.media).get(TIMEOUT, TimeUnit.SECONDS)

        val itemFromDb = DBReader.getFeedItem(item.id)
        val mediaFromDb = itemFromDb.media

        Assert.assertEquals(POSITION, mediaFromDb!!.position)
        Assert.assertEquals(LAST_PLAYED_TIME, mediaFromDb.lastPlayedTime)
        Assert.assertEquals(PLAYED_DURATION, mediaFromDb.playedDuration)
        Assert.assertEquals(DURATION, mediaFromDb.duration)
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeedMediaOfItemFileExists() {
        val dest = File(instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER), "testFile")

        Assert.assertTrue(dest.createNewFile())

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val item = FeedItem(0, "Item", "Item", "url", Date(), FeedItem.PLAYED, feed)

        var media = FeedMedia(0, item, 1, 1, 1, "mime_type", dest.absolutePath, "download_url", true, null, 0, 0)
        item.setMedia(media)

        items.add(item)

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()
        Assert.assertTrue(media.id != 0)
        Assert.assertTrue(item.id != 0)

        DBWriter.deleteFeedMediaOfItem(instrumentation.targetContext, media.id)
                .get(TIMEOUT, TimeUnit.SECONDS)
        media = DBReader.getFeedMedia(media.id)
        Assert.assertNotNull(media)
        Assert.assertFalse(dest.exists())
        Assert.assertFalse(media.isDownloaded)
        Assert.assertNull(media.file_url)
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeed() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        // create Feed image
        val imgFile = File(destFolder, "image")
        Assert.assertTrue(imgFile.createNewFile())
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        val itemFiles = ArrayList<File>()
        // create items with downloaded media files
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed, true)
            feed.items.add(item)

            val enc = File(destFolder, "file " + i)
            Assert.assertTrue(enc.createNewFile())
            itemFiles.add(enc)

            val media = FeedMedia(0, item, 1, 1, 1, "mime_type", enc.absolutePath, "download_url", true, null, 0, 0)
            item.setMedia(media)

            item.chapters = ArrayList<Chapter>()
            item.chapters.add(SimpleChapter(0, "item " + i, item, "example.com"))
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
            Assert.assertTrue(item.chapters[0].id != 0)
        }

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        // check if files still exist
        Assert.assertFalse(imgFile.exists())
        for (f in itemFiles) {
            Assert.assertFalse(f.exists())
        }

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertEquals(0, c.count)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertEquals(0, c.count)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertEquals(0, c.count)
            c.close()
            c = adapter.getSingleFeedMediaCursor(item.media!!.id)
            Assert.assertEquals(0, c.count)
            c.close()
            c = adapter.getSimpleChaptersOfFeedItemCursor(item)
            Assert.assertEquals(0, c.count)
            c.close()
        }
        adapter.close()
    }

    @Throws(ExecutionException::class, InterruptedException::class, IOException::class, TimeoutException::class)
    fun testDeleteFeedNoImage() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        feed.image = null

        val itemFiles = ArrayList<File>()
        // create items with downloaded media files
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)

            val enc = File(destFolder, "file " + i)
            Assert.assertTrue(enc.createNewFile())

            itemFiles.add(enc)
            val media = FeedMedia(0, item, 1, 1, 1, "mime_type", enc.absolutePath, "download_url", true, null, 0, 0)
            item.setMedia(media)
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
        }

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        // check if files still exist
        for (f in itemFiles) {
            Assert.assertFalse(f.exists())
        }

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertTrue(c.count == 0)
            c.close()
            c = adapter.getSingleFeedMediaCursor(item.media!!.id)
            Assert.assertTrue(c.count == 0)
            c.close()
        }
        adapter.close()
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeedNoItems() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = null

        // create Feed image
        val imgFile = File(destFolder, "image")
        Assert.assertTrue(imgFile.createNewFile())
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        // check if files still exist
        Assert.assertFalse(imgFile.exists())

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertTrue(c.count == 0)
        c.close()
        adapter.close()
    }

    @Throws(IOException::class, ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeedNoFeedMedia() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        // create Feed image
        val imgFile = File(destFolder, "image")
        Assert.assertTrue(imgFile.createNewFile())
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        // create items
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)

        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        // check if files still exist
        Assert.assertFalse(imgFile.exists())

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertTrue(c.count == 0)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertTrue(c.count == 0)
            c.close()
        }
        adapter.close()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class, IOException::class)
    fun testDeleteFeedWithItemImages() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        // create Feed image
        val imgFile = File(destFolder, "image")
        Assert.assertTrue(imgFile.createNewFile())
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        // create items with images
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)
            val itemImageFile = File(destFolder, "item-image-" + i)
            val itemImage = FeedImage(0, "item-image" + i, itemImageFile.absolutePath, "url", true)
            item.image = itemImage
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.image.id != 0)
        }

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        // check if files still exist
        Assert.assertFalse(imgFile.exists())

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertTrue(c.count == 0)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertTrue(c.count == 0)
            c.close()
            c = adapter.getImageCursor(item.image.id.toString())
            Assert.assertEquals(0, c.count)
            c.close()
        }
        adapter.close()
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeedWithQueueItems() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        // create Feed image
        val imgFile = File(destFolder, "image")
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        val itemFiles = ArrayList<File>()
        // create items with downloaded media files
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)

            val enc = File(destFolder, "file " + i)
            itemFiles.add(enc)

            val media = FeedMedia(0, item, 1, 1, 1, "mime_type", enc.absolutePath, "download_url", false, null, 0, 0)
            item.setMedia(media)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
        }


        val queue = ArrayList<FeedItem>()
        queue.addAll(feed.items)
        adapter.open()
        adapter.setQueue(queue)

        val queueCursor = adapter.queueIDCursor
        Assert.assertTrue(queueCursor.count == queue.size)
        queueCursor.close()

        adapter.close()
        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)
        adapter.open()

        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertTrue(c.count == 0)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertTrue(c.count == 0)
            c.close()
            c = adapter.getSingleFeedMediaCursor(item.media!!.id)
            Assert.assertTrue(c.count == 0)
            c.close()
        }
        c = adapter.queueCursor
        Assert.assertTrue(c.count == 0)
        c.close()
        adapter.close()
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testDeleteFeedNoDownloadedFiles() {
        val destFolder = instrumentation.targetContext.getExternalFilesDir(TEST_FOLDER)
        Assert.assertNotNull(destFolder)

        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()

        // create Feed image
        val imgFile = File(destFolder, "image")
        val image = FeedImage(0, "image", imgFile.absolutePath, "url", true)
        image.owner = feed
        feed.image = image

        val itemFiles = ArrayList<File>()
        // create items with downloaded media files
        for (i in 0..9) {
            val item = FeedItem(0, "Item " + i, "Item" + i, "url", Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)

            val enc = File(destFolder, "file " + i)
            itemFiles.add(enc)

            val media = FeedMedia(0, item, 1, 1, 1, "mime_type", enc.absolutePath, "download_url", false, null, 0, 0)
            item.setMedia(media)
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        Assert.assertTrue(feed.image.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
        }

        DBWriter.deleteFeed(instrumentation.targetContext, feed.id).get(TIMEOUT, TimeUnit.SECONDS)

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var c = adapter.getFeedCursor(feed.id)
        Assert.assertTrue(c.count == 0)
        c.close()
        c = adapter.getImageCursor(image.id.toString())
        Assert.assertTrue(c.count == 0)
        c.close()
        for (item in feed.items) {
            c = adapter.getFeedItemCursor(item.id.toString())
            Assert.assertTrue(c.count == 0)
            c.close()
            c = adapter.getSingleFeedMediaCursor(item.media!!.id)
            Assert.assertTrue(c.count == 0)
            c.close()
        }
        adapter.close()
    }

    private fun playbackHistorySetup(playbackCompletionDate: Date?): FeedMedia {
        val context = instrumentation.targetContext
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        val item = FeedItem(0, "title", "id", "link", Date(), FeedItem.PLAYED, feed)
        val media = FeedMedia(0, item, 10, 0, 1, "mime", null, "url", false, playbackCompletionDate, 0, 0)
        feed.items.add(item)
        item.setMedia(media)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()
        Assert.assertTrue(media.id != 0)
        return media
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testAddItemToPlaybackHistoryNotPlayedYet() {
        var media = playbackHistorySetup(null)
        DBWriter.addItemToPlaybackHistory(media).get(TIMEOUT, TimeUnit.SECONDS)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        media = DBReader.getFeedMedia(media.id)
        adapter.close()

        Assert.assertNotNull(media)
        Assert.assertNotNull(media.playbackCompletionDate)
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testAddItemToPlaybackHistoryAlreadyPlayed() {
        val OLD_DATE: Long = 0

        var media = playbackHistorySetup(Date(OLD_DATE))
        DBWriter.addItemToPlaybackHistory(media).get(TIMEOUT, TimeUnit.SECONDS)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        media = DBReader.getFeedMedia(media.id)
        adapter.close()

        Assert.assertNotNull(media)
        Assert.assertNotNull(media.playbackCompletionDate)
        Assert.assertFalse(OLD_DATE == media.playbackCompletionDate.time)
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    private fun queueTestSetupMultipleItems(NUM_ITEMS: Int): Feed {
        val context = instrumentation.targetContext
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title " + i, "id " + i, "link " + i, Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }
        val futures = ArrayList<Future<*>>()
        for (item in feed.items) {
            futures.add(DBWriter.addQueueItem(context, item))
        }
        for (f in futures) {
            f.get(TIMEOUT, TimeUnit.SECONDS)
        }
        return feed
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testAddQueueItemSingleItem() {
        val context = instrumentation.targetContext
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        val item = FeedItem(0, "title", "id", "link", Date(), FeedItem.PLAYED, feed)
        feed.items.add(item)

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(item.id != 0)
        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS)

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        val cursor = adapter.queueIDCursor
        Assert.assertTrue(cursor.moveToFirst())
        Assert.assertTrue(cursor.getLong(0) == item.id)
        cursor.close()
        adapter.close()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testAddQueueItemSingleItemAlreadyInQueue() {
        val context = instrumentation.targetContext
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        val item = FeedItem(0, "title", "id", "link", Date(), FeedItem.PLAYED, feed)
        feed.items.add(item)

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(item.id != 0)
        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS)

        adapter = PodDBAdapter.getInstance()
        adapter.open()
        var cursor = adapter.queueIDCursor
        Assert.assertTrue(cursor.moveToFirst())
        Assert.assertTrue(cursor.getLong(0) == item.id)
        cursor.close()
        adapter.close()

        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS)
        adapter = PodDBAdapter.getInstance()
        adapter.open()
        cursor = adapter.queueIDCursor
        Assert.assertTrue(cursor.moveToFirst())
        Assert.assertTrue(cursor.getLong(0) == item.id)
        Assert.assertTrue(cursor.count == 1)
        cursor.close()
        adapter.close()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testAddQueueItemMultipleItems() {
        val context = instrumentation.targetContext
        val NUM_ITEMS = 10

        val feed = queueTestSetupMultipleItems(NUM_ITEMS)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        val cursor = adapter.queueIDCursor
        Assert.assertTrue(cursor.moveToFirst())
        Assert.assertTrue(cursor.count == NUM_ITEMS)
        for (i in 0..NUM_ITEMS - 1) {
            Assert.assertTrue(cursor.moveToPosition(i))
            Assert.assertTrue(cursor.getLong(0) == feed.items[i].id)
        }
        cursor.close()
        adapter.close()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testClearQueue() {
        val NUM_ITEMS = 10

        val feed = queueTestSetupMultipleItems(NUM_ITEMS)
        DBWriter.clearQueue().get(TIMEOUT, TimeUnit.SECONDS)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        val cursor = adapter.queueIDCursor
        Assert.assertFalse(cursor.moveToFirst())
        cursor.close()
        adapter.close()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testRemoveQueueItem() {
        val NUM_ITEMS = 10
        val context = instrumentation.targetContext
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title " + i, "id " + i, "link " + i, Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }
        for (removeIndex in 0..NUM_ITEMS - 1) {
            val item = feed.items[removeIndex]
            adapter = PodDBAdapter.getInstance()
            adapter.open()
            adapter.setQueue(feed.items)
            adapter.close()

            DBWriter.removeQueueItem(context, item, false).get(TIMEOUT, TimeUnit.SECONDS)
            adapter = PodDBAdapter.getInstance()
            adapter.open()
            val queue = adapter.queueIDCursor
            Assert.assertTrue(queue.count == NUM_ITEMS - 1)
            for (i in 0..queue.count - 1) {
                Assert.assertTrue(queue.moveToPosition(i))
                val queueID = queue.getLong(0)
                Assert.assertTrue(queueID != item.id)  // removed item is no longer in queue
                var idFound = false
                for (other in feed.items) { // items that were not removed are still in the queue
                    idFound = idFound or (other.id == queueID)
                }
                Assert.assertTrue(idFound)
            }
            queue.close()
            adapter.close()
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testMoveQueueItem() {
        val NUM_ITEMS = 10
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title " + i, "id " + i, "link " + i, Date(), FeedItem.PLAYED, feed)
            feed.items.add(item)
        }

        var adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }
        for (from in 0..NUM_ITEMS - 1) {
            for (to in 0..NUM_ITEMS - 1) {
                if (from == to) {
                    continue
                }
                Log.d(TAG, String.format("testMoveQueueItem: From=%d, To=%d", from, to))
                val fromID = feed.items[from].id

                adapter = PodDBAdapter.getInstance()
                adapter.open()
                adapter.setQueue(feed.items)
                adapter.close()

                DBWriter.moveQueueItem(from, to, false).get(TIMEOUT, TimeUnit.SECONDS)
                adapter = PodDBAdapter.getInstance()
                adapter.open()
                val queue = adapter.queueIDCursor
                Assert.assertTrue(queue.count == NUM_ITEMS)
                Assert.assertTrue(queue.moveToPosition(from))
                Assert.assertFalse(queue.getLong(0) == fromID)
                Assert.assertTrue(queue.moveToPosition(to))
                Assert.assertTrue(queue.getLong(0) == fromID)

                queue.close()
                adapter.close()
            }
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testMarkFeedRead() {
        val NUM_ITEMS = 10
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title " + i, "id " + i, "link " + i, Date(), FeedItem.UNPLAYED, feed)
            feed.items.add(item)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }

        DBWriter.markFeedRead(feed.id).get(TIMEOUT, TimeUnit.SECONDS)
        val loadedItems = DBReader.getFeedItemList(feed)
        for (item in loadedItems) {
            Assert.assertTrue(item.isPlayed)
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun testMarkAllItemsReadSameFeed() {
        val NUM_ITEMS = 10
        val feed = Feed("url", null, "title")
        feed.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title " + i, "id " + i, "link " + i, Date(), FeedItem.UNPLAYED, feed)
            feed.items.add(item)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        for (item in feed.items) {
            Assert.assertTrue(item.id != 0)
        }

        DBWriter.markAllItemsRead().get(TIMEOUT, TimeUnit.SECONDS)
        val loadedItems = DBReader.getFeedItemList(feed)
        for (item in loadedItems) {
            Assert.assertTrue(item.isPlayed)
        }
    }

    companion object {

        private val TAG = "DBWriterTest"
        private val TEST_FOLDER = "testDBWriter"
        private val TIMEOUT = 5L
    }
}
