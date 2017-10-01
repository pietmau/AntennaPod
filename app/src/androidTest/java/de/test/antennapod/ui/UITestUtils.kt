package de.test.antennapod.ui

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log

import junit.framework.Assert

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Date

import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.core.feed.EventDistributor
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedImage
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.event.QueueEvent
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.core.util.playback.PlaybackController
import de.danoeh.antennapod.fragment.ExternalPlayerFragment
import de.greenrobot.event.EventBus
import de.test.antennapod.util.service.download.HTTPBin
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator

/**
 * Utility methods for UI tests.
 * Starts a web server that hosts feeds, episodes and images.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class UITestUtils(private val context: Context) {
    private val server = HTTPBin()
    private var destDir: File? = null
    private var hostedFeedDir: File? = null
    private var hostedMediaDir: File? = null

    var hostedFeeds: MutableList<Feed> = ArrayList()


    @Throws(IOException::class)
    fun setup() {
        destDir = context.getExternalFilesDir(DATA_FOLDER)
        destDir!!.mkdir()
        hostedFeedDir = File(destDir, "hostedFeeds")
        hostedFeedDir!!.mkdir()
        hostedMediaDir = File(destDir, "hostedMediaDir")
        hostedMediaDir!!.mkdir()
        Assert.assertTrue(destDir!!.exists())
        Assert.assertTrue(hostedFeedDir!!.exists())
        Assert.assertTrue(hostedMediaDir!!.exists())
        server.start()
    }

    @Throws(IOException::class)
    fun tearDown() {
        FileUtils.deleteDirectory(destDir!!)
        FileUtils.deleteDirectory(hostedMediaDir!!)
        FileUtils.deleteDirectory(hostedFeedDir!!)
        server.stop()

        if (localFeedDataAdded) {
            PodDBAdapter.deleteDatabase()
        }
    }

    @Throws(IOException::class)
    private fun hostFeed(feed: Feed): String {
        val feedFile = File(hostedFeedDir, feed.title)
        val out = FileOutputStream(feedFile)
        val generator = RSS2Generator()
        generator.writeFeed(feed, out, "UTF-8", 0)
        out.close()
        val id = server.serveFile(feedFile)
        Assert.assertTrue(id != -1)
        return String.format("%s/files/%d", HTTPBin.BASE_URL, id)
    }

    private fun hostFile(file: File): String {
        val id = server.serveFile(file)
        Assert.assertTrue(id != -1)
        return String.format("%s/files/%d", HTTPBin.BASE_URL, id)
    }

    @Throws(IOException::class)
    private fun newBitmapFile(name: String): File {
        val imgFile = File(destDir, name)
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val out = FileOutputStream(imgFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 1, out)
        out.close()
        return imgFile
    }

    @Throws(IOException::class)
    private fun newMediaFile(name: String): File {
        val mediaFile = File(hostedMediaDir, name)
        if (mediaFile.exists()) {
            mediaFile.delete()
        }
        Assert.assertFalse(mediaFile.exists())

        val `in` = context.assets.open(TEST_FILE_NAME)
        Assert.assertNotNull(`in`)

        val out = FileOutputStream(mediaFile)
        IOUtils.copy(`in`, out)
        out.close()

        return mediaFile
    }

    private var feedDataHosted = false

    /**
     * Adds feeds, images and episodes to the webserver for testing purposes.
     */
    @Throws(IOException::class)
    fun addHostedFeedData() {
        if (feedDataHosted) throw IllegalStateException("addHostedFeedData was called twice on the same instance")
        for (i in 0..NUM_FEEDS - 1) {
            val bitmapFile = newBitmapFile("image" + i)
            val image = FeedImage(0, "image " + i, null, hostFile(bitmapFile), false)
            val feed = Feed(0, null, "Title " + i, "http://example.com/" + i, "Description of feed " + i,
                    "http://example.com/pay/feed" + i, "author " + i, "en", Feed.TYPE_RSS2, "feed" + i, image, null,
                    "http://example.com/feed/src/" + i, false)
            image.owner = feed

            // create items
            val items = ArrayList<FeedItem>()
            for (j in 0..NUM_ITEMS_PER_FEED - 1) {
                val item = FeedItem(j.toLong(), "Feed " + (i + 1) + ": Item " + (j + 1), "item" + j,
                        "http://example.com/feed$i/item/$j", Date(), FeedItem.UNPLAYED, feed)
                items.add(item)

                val mediaFile = newMediaFile("feed-$i-episode-$j.mp3")
                item.media = FeedMedia(j.toLong(), item, 0, 0, mediaFile.length(), "audio/mp3", null, hostFile(mediaFile), false, null, 0, 0)

            }
            feed.items = items
            feed.download_url = hostFeed(feed)
            hostedFeeds.add(feed)
        }
        feedDataHosted = true
    }


    private var localFeedDataAdded = false

    /**
     * Adds feeds, images and episodes to the local database. This method will also call addHostedFeedData if it has not
     * been called yet.

     * Adds one item of each feed to the queue and to the playback history.

     * This method should NOT be called if the testing class wants to download the hosted feed data.

     * @param downloadEpisodes true if episodes should also be marked as downloaded.
     */
    @Throws(Exception::class)
    fun addLocalFeedData(downloadEpisodes: Boolean) {
        if (localFeedDataAdded) {
            Log.w(TAG, "addLocalFeedData was called twice on the same instance")
            // might be a flaky test, this is actually not that severe
            return
        }
        if (!feedDataHosted) {
            addHostedFeedData()
        }

        val queue = ArrayList<FeedItem>()
        for (feed in hostedFeeds) {
            feed.isDownloaded = true
            if (feed.image != null) {
                val image = feed.image
                val fileId = Integer.parseInt(StringUtils.substringAfter(image.download_url, "files/"))
                image.file_url = server.accessFile(fileId)!!.absolutePath
                image.isDownloaded = true
            }
            if (downloadEpisodes) {
                for (item in feed.items) {
                    if (item.hasMedia()) {
                        val media = item.media
                        val fileId = Integer.parseInt(StringUtils.substringAfter(media!!.download_url, "files/"))
                        media.file_url = server.accessFile(fileId)!!.absolutePath
                        media.isDownloaded = true
                    }
                }
            }

            queue.add(feed.items[0])
            feed.items[1].media!!.playbackCompletionDate = Date()
        }
        localFeedDataAdded = true

        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(*hostedFeeds.toTypedArray())
        adapter.setQueue(queue)
        adapter.close()
        EventDistributor.getInstance().sendFeedUpdateBroadcast()
        EventBus.getDefault().post(QueueEvent.setQueue(queue))
    }

    fun getPlaybackController(mainActivity: MainActivity): PlaybackController {
        val fragment = mainActivity.supportFragmentManager.findFragmentByTag(ExternalPlayerFragment.TAG) as ExternalPlayerFragment
        return fragment.playbackControllerTestingOnly
    }

    fun getCurrentMedia(mainActivity: MainActivity): FeedMedia {
        return getPlaybackController(mainActivity).media as FeedMedia
    }

    companion object {

        private val TAG = UITestUtils::class.java.simpleName

        private val DATA_FOLDER = "test/UITestUtils"

        val NUM_FEEDS = 5
        val NUM_ITEMS_PER_FEED = 10

        val HOME_VIEW = if (Build.VERSION.SDK_INT >= 11) android.R.id.home else R.id.home
        val TEST_FILE_NAME = "3sec.mp3"
    }
}
