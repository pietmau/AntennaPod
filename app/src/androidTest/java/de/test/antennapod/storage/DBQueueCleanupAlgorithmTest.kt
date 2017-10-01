package de.test.antennapod.storage

import android.test.FlakyTest

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.storage.DBTasks

/**
 * Tests that the APQueueCleanupAlgorithm is working correctly.
 */
class DBQueueCleanupAlgorithmTest : DBCleanupTests(UserPreferences.EPISODE_CLEANUP_QUEUE) {

    /**
     * For APQueueCleanupAlgorithm we expect even unplayed episodes to be deleted if needed
     * if they aren't in the queue
     */
    @FlakyTest(tolerance = 3)
    @Throws(IOException::class)
    override fun testPerformAutoCleanupHandleUnplayed() {
        val NUM_ITEMS = DBCleanupTests.EPISODE_CACHE_SIZE * 2

        val feed = Feed("url", null, "title")
        val items = ArrayList<FeedItem>()
        feed.items = items
        val files = ArrayList<File>()
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.UNPLAYED, false, false)

        DBTasks.performAutoCleanup(context)
        for (i in files.indices) {
            if (i < DBCleanupTests.EPISODE_CACHE_SIZE) {
                Assert.assertTrue(files[i].exists())
            } else {
                Assert.assertFalse(files[i].exists())
            }
        }
    }

    companion object {

        private val TAG = "DBQueueCleanupAlgorithmTest"
    }
}
