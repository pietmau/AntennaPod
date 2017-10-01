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

/**
 * Tests that the APNullCleanupAlgorithm is working correctly.
 */
class DBNullCleanupAlgorithmTest : InstrumentationTestCase() {

    private var context: Context? = null

    private var destFolder: File? = null

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()

        Assert.assertTrue(PodDBAdapter.deleteDatabase())

        cleanupDestFolder(destFolder)
        Assert.assertTrue(destFolder!!.delete())
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
        destFolder = context!!.externalCacheDir
        cleanupDestFolder(destFolder)
        Assert.assertNotNull(destFolder)
        Assert.assertTrue(destFolder!!.exists())
        Assert.assertTrue(destFolder!!.canWrite())

        // create new database
        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()

        val prefEdit = PreferenceManager.getDefaultSharedPreferences(context!!.applicationContext).edit()
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE))
        prefEdit.putString(UserPreferences.PREF_EPISODE_CLEANUP, Integer.toString(UserPreferences.EPISODE_CLEANUP_NULL))
        prefEdit.commit()

        UserPreferences.init(context!!)
    }

    /**
     * A test with no items in the queue, but multiple items downloaded.
     * The null algorithm should never delete any items, even if they're played and not in the queue.
     * @throws IOException
     */
    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    fun testPerformAutoCleanupShouldNotDelete() {
        val NUM_ITEMS = EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        for (i in 0..NUM_ITEMS - 1) {
            val item = FeedItem(0, "title", "id", "link", Date(), FeedItem.PLAYED, feed)

            val f = File(destFolder, "file " + i)
            Assert.assertTrue(f.createNewFile())
            files.add(f)
            item.setMedia(FeedMedia(0, item, 1, 0, 1L, "m", f.absolutePath, "url", true,
                    Date((NUM_ITEMS - i).toLong()), 0, 0))
            items.add(item)
        }

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(feed)
        adapter.close()

        Assert.assertTrue(feed.id != 0)
        for (item in items) {
            Assert.assertTrue(item.id != 0)
            Assert.assertTrue(item.media!!.id != 0)
        }
        DBTasks.performAutoCleanup(context)
        for (i in files.indices) {
            Assert.assertTrue(files[i].exists())
        }
    }

    companion object {

        private val TAG = "DBNullCleanupAlgorithmTest"
        private val EPISODE_CACHE_SIZE = 5
    }
}
