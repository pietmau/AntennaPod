package de.test.antennapod.service.playback

import android.content.Context
import android.support.annotation.StringRes
import android.test.InstrumentationTestCase

import junit.framework.AssertionFailedError

import org.apache.commons.io.IOUtils

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.feed.FeedPreferences
import de.danoeh.antennapod.core.feed.MediaType
import de.danoeh.antennapod.core.service.playback.PlaybackServiceMediaPlayer
import de.danoeh.antennapod.core.service.playback.LocalPSMP
import de.danoeh.antennapod.core.service.playback.PlayerStatus
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.core.util.playback.Playable
import de.test.antennapod.util.service.download.HTTPBin

/**
 * Test class for LocalPSMP
 */
class PlaybackServiceMediaPlayerTest : InstrumentationTestCase() {
    private var PLAYABLE_LOCAL_URL: String? = null

    private var httpServer: HTTPBin? = null

    @Volatile private var assertionError: AssertionFailedError? = null

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        PodDBAdapter.deleteDatabase()
        httpServer!!.stop()
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        assertionError = null

        val context = instrumentation.targetContext

        // create new database
        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()

        httpServer = HTTPBin()
        httpServer!!.start()

        var cacheDir = context.getExternalFilesDir("testFiles")
        if (cacheDir == null)
            cacheDir = context.getExternalFilesDir("testFiles")
        val dest = File(cacheDir, PLAYABLE_DEST_URL)

        Assert.assertNotNull(cacheDir)
        Assert.assertTrue(cacheDir!!.canWrite())
        Assert.assertTrue(cacheDir.canRead())
        if (!dest.exists()) {
            val i = instrumentation.context.assets.open("testfile.mp3")
            val o = FileOutputStream(File(cacheDir, PLAYABLE_DEST_URL))
            IOUtils.copy(i, o)
            o.flush()
            o.close()
            i.close()
        }
        PLAYABLE_LOCAL_URL = "file://" + dest.absolutePath
        Assert.assertEquals(0, httpServer!!.serveFile(dest))
    }

    private fun checkPSMPInfo(info: LocalPSMP.PSMPInfo) {
        try {
            when (info.playerStatus) {
                PlayerStatus.PLAYING, PlayerStatus.PAUSED, PlayerStatus.PREPARED, PlayerStatus.PREPARING, PlayerStatus.INITIALIZED, PlayerStatus.INITIALIZING, PlayerStatus.SEEKING -> Assert.assertNotNull(info.playable)
                PlayerStatus.STOPPED -> Assert.assertNull(info.playable)
                PlayerStatus.ERROR -> Assert.assertNull(info.playable)
            }
        } catch (e: AssertionFailedError) {
            if (assertionError == null)
                assertionError = e
        }

    }

    fun testInit() {
        val c = instrumentation.targetContext
        val psmp = LocalPSMP(c, DefaultPSMPCallback())
        psmp.shutdown()
    }

    private fun writeTestPlayable(downloadUrl: String, fileUrl: String?): Playable {
        val c = instrumentation.targetContext
        val f = Feed(0, null, "f", "l", "d", null, null, null, null, "i", null, null, "l", false)
        val prefs = FeedPreferences(f.id, false, FeedPreferences.AutoDeleteAction.NO, null, null)
        f.preferences = prefs
        f.items = ArrayList<FeedItem>()
        val i = FeedItem(0, "t", "i", "l", Date(), FeedItem.UNPLAYED, f)
        f.items.add(i)
        val media = FeedMedia(0, i, 0, 0, 0, "audio/wav", fileUrl, downloadUrl, fileUrl != null, null, 0, 0)
        i.setMedia(media)
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(f)
        Assert.assertTrue(media.id != 0)
        adapter.close()
        return media
    }


    @Throws(InterruptedException::class)
    fun testPlayMediaObjectStreamNoStartNoPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(2)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                        countDownLatch.countDown()
                    } else {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                        countDownLatch.countDown()
                    }
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, null)
        psmp.playMediaObject(p, true, false, false)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)

        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.INITIALIZED)
        Assert.assertFalse(psmp.isStartWhenPrepared)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectStreamStartNoPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(2)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                        countDownLatch.countDown()
                    } else {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                        countDownLatch.countDown()
                    }
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, null)
        psmp.playMediaObject(p, true, true, false)

        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)

        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.INITIALIZED)
        Assert.assertTrue(psmp.isStartWhenPrepared)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectStreamNoStartPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(4)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 4) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 3) {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 1) {
                        Assert.assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus)
                    }
                    countDownLatch.countDown()
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, null)
        psmp.playMediaObject(p, true, false, true)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.PREPARED)

        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectStreamStartPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(5)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()

                    } else if (countDownLatch.count == 5) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 4) {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 3) {
                        Assert.assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 1) {
                        Assert.assertEquals(PlayerStatus.PLAYING, newInfo.playerStatus)
                    }
                    countDownLatch.countDown()
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, null)
        psmp.playMediaObject(p, true, true, true)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.PLAYING)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectLocalNoStartNoPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(2)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                        countDownLatch.countDown()
                    } else {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                        countDownLatch.countDown()
                    }
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        psmp.playMediaObject(p, false, false, false)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.INITIALIZED)
        Assert.assertFalse(psmp.isStartWhenPrepared)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectLocalStartNoPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(2)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                        countDownLatch.countDown()
                    } else {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                        countDownLatch.countDown()
                    }
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        psmp.playMediaObject(p, false, true, false)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.INITIALIZED)
        Assert.assertTrue(psmp.isStartWhenPrepared)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectLocalNoStartPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(4)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 4) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 3) {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 1) {
                        Assert.assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus)
                    }
                    countDownLatch.countDown()
                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                }

            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        psmp.playMediaObject(p, false, false, true)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.PREPARED)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPlayMediaObjectLocalStartPrepare() {
        val c = instrumentation.targetContext
        val countDownLatch = CountDownLatch(5)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                try {
                    checkPSMPInfo(newInfo)
                    if (newInfo.playerStatus == PlayerStatus.ERROR)
                        throw IllegalStateException("MediaPlayer error")
                    if (countDownLatch.count == 0) {
                        Assert.fail()
                    } else if (countDownLatch.count == 5) {
                        Assert.assertEquals(PlayerStatus.INITIALIZING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 4) {
                        Assert.assertEquals(PlayerStatus.INITIALIZED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 3) {
                        Assert.assertEquals(PlayerStatus.PREPARING, newInfo.playerStatus)
                    } else if (countDownLatch.count == 2) {
                        Assert.assertEquals(PlayerStatus.PREPARED, newInfo.playerStatus)
                    } else if (countDownLatch.count == 1) {
                        Assert.assertEquals(PlayerStatus.PLAYING, newInfo.playerStatus)
                    }

                } catch (e: AssertionFailedError) {
                    if (assertionError == null)
                        assertionError = e
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        psmp.playMediaObject(p, false, true, true)
        val res = countDownLatch.await(LATCH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        Assert.assertTrue(psmp.psmpInfo.playerStatus == PlayerStatus.PLAYING)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    private fun pauseTestSkeleton(initialState: PlayerStatus, stream: Boolean, abandonAudioFocus: Boolean, reinit: Boolean, timeoutSeconds: Long) {
        val c = instrumentation.targetContext
        val latchCount = if (stream && reinit) 2 else 1
        val countDownLatch = CountDownLatch(latchCount)

        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                checkPSMPInfo(newInfo)
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = UnexpectedStateChange(newInfo.playerStatus)
                } else if (initialState != PlayerStatus.PLAYING) {
                    if (assertionError == null)
                        assertionError = UnexpectedStateChange(newInfo.playerStatus)
                } else {
                    when (newInfo.playerStatus) {
                        PlayerStatus.PAUSED -> if (latchCount.toLong() == countDownLatch.count)
                            countDownLatch.countDown()
                        else {
                            if (assertionError == null)
                                assertionError = UnexpectedStateChange(newInfo.playerStatus)
                        }
                        PlayerStatus.INITIALIZED -> if (stream && reinit && countDownLatch.count < latchCount) {
                            countDownLatch.countDown()
                        } else if (countDownLatch.count < latchCount) {
                            if (assertionError == null)
                                assertionError = UnexpectedStateChange(newInfo.playerStatus)
                        }
                    }
                }

            }

            override fun shouldStop() {
                if (assertionError == null)
                    assertionError = AssertionFailedError("Unexpected call to shouldStop")
            }

            override fun onMediaPlayerError(inObj: Any, what: Int, extra: Int): Boolean {
                if (assertionError == null)
                    assertionError = AssertionFailedError("Unexpected call to onMediaPlayerError")
                return false
            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        if (initialState == PlayerStatus.PLAYING) {
            psmp.playMediaObject(p, stream, true, true)
        }
        psmp.pause(abandonAudioFocus, reinit)
        val res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res || initialState != PlayerStatus.PLAYING)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPauseDefaultState() {
        pauseTestSkeleton(PlayerStatus.STOPPED, false, false, false, 1)
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateNoAbandonNoReinitNoStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, false, false, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateNoAbandonNoReinitStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, false, false, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateAbandonNoReinitNoStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, true, false, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateAbandonNoReinitStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, true, false, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateNoAbandonReinitNoStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, false, true, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateNoAbandonReinitStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, false, true, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateAbandonReinitNoStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, false, true, true, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPausePlayingStateAbandonReinitStream() {
        pauseTestSkeleton(PlayerStatus.PLAYING, true, true, true, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    private fun resumeTestSkeleton(initialState: PlayerStatus, timeoutSeconds: Long) {
        val c = instrumentation.targetContext
        val latchCount = if (initialState == PlayerStatus.PAUSED || initialState == PlayerStatus.PLAYING)
            2
        else if (initialState == PlayerStatus.PREPARED) 1 else 0
        val countDownLatch = CountDownLatch(latchCount)

        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                checkPSMPInfo(newInfo)
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = UnexpectedStateChange(newInfo.playerStatus)
                } else if (newInfo.playerStatus == PlayerStatus.PLAYING) {
                    if (countDownLatch.count == 0) {
                        if (assertionError == null)
                            assertionError = UnexpectedStateChange(newInfo.playerStatus)
                    } else {
                        countDownLatch.countDown()
                    }
                }

            }

            override fun onMediaPlayerError(inObj: Any, what: Int, extra: Int): Boolean {
                if (assertionError == null) {
                    assertionError = AssertionFailedError("Unexpected call of onMediaPlayerError")
                }
                return false
            }
        }
        val psmp = LocalPSMP(c, callback)
        if (initialState == PlayerStatus.PREPARED || initialState == PlayerStatus.PLAYING || initialState == PlayerStatus.PAUSED) {
            val startWhenPrepared = initialState != PlayerStatus.PREPARED
            psmp.playMediaObject(writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL), false, startWhenPrepared, true)
        }
        if (initialState == PlayerStatus.PAUSED) {
            psmp.pause(false, false)
        }
        psmp.resume()
        val res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res || initialState != PlayerStatus.PAUSED && initialState != PlayerStatus.PREPARED)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testResumePausedState() {
        resumeTestSkeleton(PlayerStatus.PAUSED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testResumePreparedState() {
        resumeTestSkeleton(PlayerStatus.PREPARED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testResumePlayingState() {
        resumeTestSkeleton(PlayerStatus.PLAYING, 1)
    }

    @Throws(InterruptedException::class)
    private fun prepareTestSkeleton(initialState: PlayerStatus, timeoutSeconds: Long) {
        val c = instrumentation.targetContext
        val latchCount = 1
        val countDownLatch = CountDownLatch(latchCount)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                checkPSMPInfo(newInfo)
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = UnexpectedStateChange(newInfo.playerStatus)
                } else {
                    if (initialState == PlayerStatus.INITIALIZED && newInfo.playerStatus == PlayerStatus.PREPARED) {
                        countDownLatch.countDown()
                    } else if (initialState != PlayerStatus.INITIALIZED && initialState == newInfo.playerStatus) {
                        countDownLatch.countDown()
                    }
                }
            }

            override fun onMediaPlayerError(inObj: Any, what: Int, extra: Int): Boolean {
                if (assertionError == null)
                    assertionError = AssertionFailedError("Unexpected call to onMediaPlayerError")
                return false
            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        if (initialState == PlayerStatus.INITIALIZED
                || initialState == PlayerStatus.PLAYING
                || initialState == PlayerStatus.PREPARED
                || initialState == PlayerStatus.PAUSED) {
            val prepareImmediately = initialState != PlayerStatus.INITIALIZED
            val startWhenPrepared = initialState != PlayerStatus.PREPARED
            psmp.playMediaObject(p, false, startWhenPrepared, prepareImmediately)
            if (initialState == PlayerStatus.PAUSED) {
                psmp.pause(false, false)
            }
            psmp.prepare()
        }

        val res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS)
        if (initialState != PlayerStatus.INITIALIZED) {
            Assert.assertEquals(initialState, psmp.psmpInfo.playerStatus)
        }

        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testPrepareInitializedState() {
        prepareTestSkeleton(PlayerStatus.INITIALIZED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPreparePlayingState() {
        prepareTestSkeleton(PlayerStatus.PLAYING, 1)
    }

    @Throws(InterruptedException::class)
    fun testPreparePausedState() {
        prepareTestSkeleton(PlayerStatus.PAUSED, 1)
    }

    @Throws(InterruptedException::class)
    fun testPreparePreparedState() {
        prepareTestSkeleton(PlayerStatus.PREPARED, 1)
    }

    @Throws(InterruptedException::class)
    private fun reinitTestSkeleton(initialState: PlayerStatus, timeoutSeconds: Long) {
        val c = instrumentation.targetContext
        val latchCount = 2
        val countDownLatch = CountDownLatch(latchCount)
        val callback = object : DefaultPSMPCallback() {
            override fun statusChanged(newInfo: LocalPSMP.PSMPInfo) {
                checkPSMPInfo(newInfo)
                if (newInfo.playerStatus == PlayerStatus.ERROR) {
                    if (assertionError == null)
                        assertionError = UnexpectedStateChange(newInfo.playerStatus)
                } else {
                    if (newInfo.playerStatus == initialState) {
                        countDownLatch.countDown()
                    } else if (countDownLatch.count < latchCount && newInfo.playerStatus == PlayerStatus.INITIALIZED) {
                        countDownLatch.countDown()
                    }
                }
            }

            override fun onMediaPlayerError(inObj: Any, what: Int, extra: Int): Boolean {
                if (assertionError == null)
                    assertionError = AssertionFailedError("Unexpected call to onMediaPlayerError")
                return false
            }
        }
        val psmp = LocalPSMP(c, callback)
        val p = writeTestPlayable(PLAYABLE_FILE_URL, PLAYABLE_LOCAL_URL)
        val prepareImmediately = initialState != PlayerStatus.INITIALIZED
        val startImmediately = initialState != PlayerStatus.PREPARED
        psmp.playMediaObject(p, false, startImmediately, prepareImmediately)
        if (initialState == PlayerStatus.PAUSED) {
            psmp.pause(false, false)
        }
        psmp.reinit()
        val res = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS)
        if (assertionError != null)
            throw assertionError
        Assert.assertTrue(res)
        psmp.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testReinitPlayingState() {
        reinitTestSkeleton(PlayerStatus.PLAYING, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testReinitPausedState() {
        reinitTestSkeleton(PlayerStatus.PAUSED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testPreparedPlayingState() {
        reinitTestSkeleton(PlayerStatus.PREPARED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    @Throws(InterruptedException::class)
    fun testReinitInitializedState() {
        reinitTestSkeleton(PlayerStatus.INITIALIZED, LATCH_TIMEOUT_SECONDS.toLong())
    }

    private class UnexpectedStateChange(status: PlayerStatus) : AssertionFailedError("Unexpected state change: " + status)

    private open inner class DefaultPSMPCallback : PlaybackServiceMediaPlayer.PSMPCallback {
        override fun statusChanged(newInfo: PlaybackServiceMediaPlayer.PSMPInfo) {

        }

        override fun shouldStop() {

        }

        override fun playbackSpeedChanged(s: Float) {

        }

        override fun setSpeedAbilityChanged() {

        }

        override fun onBufferingUpdate(percent: Int) {

        }

        override fun onMediaChanged(reloadUI: Boolean) {

        }

        override fun onMediaPlayerInfo(code: Int, @StringRes resourceId: Int): Boolean {
            return false
        }

        override fun onMediaPlayerError(inObj: Any, what: Int, extra: Int): Boolean {
            return false
        }

        override fun onPostPlayback(media: Playable, ended: Boolean, skipped: Boolean, playingNext: Boolean) {

        }

        override fun onPlaybackStart(playable: Playable, position: Int) {

        }

        override fun onPlaybackPause(playable: Playable, position: Int) {

        }

        override fun getNextInQueue(currentMedia: Playable): Playable? {
            return null
        }

        override fun onPlaybackEnded(mediaType: MediaType, stopPlaying: Boolean) {

        }
    }

    companion object {
        private val TAG = "PlaybackServiceMediaPlayerTest"

        private val PLAYABLE_FILE_URL = "http://127.0.0.1:" + HTTPBin.PORT + "/files/0"
        private val PLAYABLE_DEST_URL = "psmptestfile.mp3"
        private val LATCH_TIMEOUT_SECONDS = 10
    }
}
