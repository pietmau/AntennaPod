package de.test.antennapod.storage

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.test.FlakyTest
import android.test.InstrumentationTestCase

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.storage.DBTasks
import de.danoeh.antennapod.core.storage.PodDBAdapter

import de.test.antennapod.storage.DBTestUtils.saveFeedlist

/**
 * Test class for DBTasks
 */
open class DBCleanupTests : InstrumentationTestCase {
    private val cleanupAlgorithm: Int

    protected var context: Context

    protected var destFolder: File

    constructor() {
        this.cleanupAlgorithm = UserPreferences.EPISODE_CLEANUP_DEFAULT
    }

    constructor(cleanupAlgorithm: Int) {
        this.cleanupAlgorithm = cleanupAlgorithm
    }


    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()

        Assert.assertTrue(PodDBAdapter.deleteDatabase())

        cleanupDestFolder(destFolder)
        Assert.assertTrue(destFolder.delete())
    }

    private fun cleanupDestFolder(destFolder: File) {
        for (f in destFolder.listFiles()) {
            Assert.assertTrue(f.delete())
        }
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        context = instrumentation.targetContext
        destFolder = context.externalCacheDir
        cleanupDestFolder(destFolder)
        Assert.assertNotNull(destFolder)
        Assert.assertTrue(destFolder.exists())
        Assert.assertTrue(destFolder.canWrite())

        // create new database
        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()

        val prefEdit = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE))
        prefEdit.putString(UserPreferences.PREF_EPISODE_CLEANUP, Integer.toString(cleanupAlgorithm))
        prefEdit.commit()

        UserPreferences.init(context)
    }

    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    fun testPerformAutoCleanupShouldDelete() {
        val NUM_ITEMS = EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, false, false)

        DBTasks.performAutoCleanup(context)
        for (i in files.indices) {
            if (i < EPISODE_CACHE_SIZE) {
                Assert.assertTrue(files[i].exists())
            } else {
                Assert.assertFalse(files[i].exists())
            }
        }
    }

    @Throws(IOException::class)
    protected fun populateItems(numItems: Int, feed: Feed, items: MutableList<FeedItem>,
                                files: MutableList<File>, itemState: Int, addToQueue: Boolean,
                                addToFavorites: Boolean) {
        for (i in 0..numItems - 1) {
            val itemDate = Date((numItems - i).toLong())
            var playbackCompletionDate: Date? = null
            if (itemState == FeedItem.PLAYED) {
                playbackCompletionDate = itemDate
            }
            val item = FeedItem(0, "title", "id", "link", itemDate, itemState, feed)

            val f = File(destFolder, "file " + i)
            Assert.assertTrue(f.createNewFile())
            files.add(f)
            item.setMedia(FeedMedia(0, item, 1, 0, 1L, "m", f.absolutePath, "url", true, playbackCompletionDate, 0, 0))
            items.add(item)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        if (addToQueue) {
            adapter.setQueue(items)
        }
        if (addToFavorites) {
            adapter.setFavorites(items)
        }
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        for (item in items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
        }
    }

    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    open fun testPerformAutoCleanupHandleUnplayed() {
        val NUM_ITEMS = EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.UNPLAYED, false, false)

        DBTasks.performAutoCleanup(context)
        for (file in files) {
            Assert.assertTrue(file.exists())
        }
    }

    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    fun testPerformAutoCleanupShouldNotDeleteBecauseInQueue() {
        val NUM_ITEMS = EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, true, false)

        DBTasks.performAutoCleanup(context)
        for (file in files) {
            Assert.assertTrue(file.exists())
        }
    }

    /**
     * Reproduces a bug where DBTasks.performAutoCleanup(android.content.Context) would use the ID of the FeedItem in the
     * call to DBWriter.deleteFeedMediaOfItem instead of the ID of the FeedMedia. This would cause the wrong item to be deleted.
     * @throws IOException
     */
    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    fun testPerformAutoCleanupShouldNotDeleteBecauseInQueue_withFeedsWithNoMedia() {
        // add feed with no enclosures so that item ID != media ID
        saveFeedlist(1, 10, false)

        // add candidate for performAutoCleanup
        val feeds = saveFeedlist(1, 1, true)
        val m = feeds[0].items[0].media
        m!!.isDownloaded = true
        m.file_url = "file"
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setMedia(m)
        adapter.close()

        testPerformAutoCleanupShouldNotDeleteBecauseInQueue()
    }

    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    fun testPerformAutoCleanupShouldNotDeleteBecauseFavorite() {
        val NUM_ITEMS = EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, false, true)

        DBTasks.performAutoCleanup(context)
        for (file in files) {
            Assert.assertTrue(file.exists())
        }
    }

    companion object {

        private val TAG = "DBTasksTest"
        protected val EPISODE_CACHE_SIZE = 5
    }
}
