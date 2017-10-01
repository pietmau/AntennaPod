package de.test.antennapod.service.playback

import android.content.Context
import android.test.InstrumentationTestCase

import java.util.ArrayList
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import de.danoeh.antennapod.core.feed.EventDistributor
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.event.QueueEvent
import de.danoeh.antennapod.core.service.playback.PlaybackServiceTaskManager
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.core.util.playback.Playable
import de.greenrobot.event.EventBus

/**
 * Test class for PlaybackServiceTaskManager
 */
class PlaybackServiceTaskManagerTest : InstrumentationTestCase() {

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        PodDBAdapter.deleteDatabase()
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

    fun testInit() {
        val pstm = PlaybackServiceTaskManager(instrumentation.targetContext, defaultPSTM)
        pstm.shutdown()
    }

    private fun writeTestQueue(pref: String): List<FeedItem> {
        val c = instrumentation.targetContext
        val NUM_ITEMS = 10
        val f = Feed(0, null, "title", "link", "d", null, null, null, null, "id", null, "null", "url", false)
        f.items = ArrayList<FeedItem>()
        for (i in 0..NUM_ITEMS - 1) {
            f.items.add(FeedItem(0, pref + i, pref + i, "link", Date(), FeedItem.PLAYED, f))
        }
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.setCompleteFeed(f)
        adapter.setQueue(f.items)
        adapter.close()

        for (item in f.items) {
            Assert.assertTrue(item.id != 0)
        }
        return f.items
    }

    @Throws(InterruptedException::class)
    fun testGetQueueWriteBeforeCreation() {
        val c = instrumentation.targetContext
        val queue = writeTestQueue("a")
        Assert.assertNotNull(queue)
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        val testQueue = pstm.queue
        Assert.assertNotNull(testQueue)
        Assert.assertTrue(queue.size == testQueue.size)
        for (i in queue.indices) {
            Assert.assertTrue(queue[i].id == testQueue[i].id)
        }
        pstm.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testGetQueueWriteAfterCreation() {
        val c = instrumentation.targetContext

        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        var testQueue = pstm.queue
        Assert.assertNotNull(testQueue)
        Assert.assertTrue(testQueue.isEmpty())


        val countDownLatch = CountDownLatch(1)
        val queueListener = object : EventDistributor.EventListener() {
            override fun update(eventDistributor: EventDistributor, arg: Int?) {
                countDownLatch.countDown()
            }
        }
        EventDistributor.getInstance().register(queueListener)
        val queue = writeTestQueue("a")
        EventBus.getDefault().post(QueueEvent.setQueue(queue))
        countDownLatch.await(5000, TimeUnit.MILLISECONDS)

        Assert.assertNotNull(queue)
        testQueue = pstm.queue
        Assert.assertNotNull(testQueue)
        Assert.assertTrue(queue.size == testQueue.size)
        for (i in queue.indices) {
            Assert.assertTrue(queue[i].id == testQueue[i].id)
        }
        pstm.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testStartPositionSaver() {
        val c = instrumentation.targetContext
        val NUM_COUNTDOWNS = 2
        val TIMEOUT = 3 * PlaybackServiceTaskManager.POSITION_SAVER_WAITING_INTERVAL
        val countDownLatch = CountDownLatch(NUM_COUNTDOWNS)
        val pstm = PlaybackServiceTaskManager(c, object : PlaybackServiceTaskManager.PSTMCallback {
            override fun positionSaverTick() {
                countDownLatch.countDown()
            }

            override fun onSleepTimerAlmostExpired() {

            }

            override fun onSleepTimerExpired() {

            }

            override fun onSleepTimerReset() {

            }

            override fun onWidgetUpdaterTick() {

            }

            override fun onChapterLoaded(media: Playable) {

            }
        })
        pstm.startPositionSaver()
        countDownLatch.await(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        pstm.shutdown()
    }

    fun testIsPositionSaverActive() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.startPositionSaver()
        Assert.assertTrue(pstm.isPositionSaverActive)
        pstm.shutdown()
    }

    fun testCancelPositionSaver() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.startPositionSaver()
        pstm.cancelPositionSaver()
        Assert.assertFalse(pstm.isPositionSaverActive)
        pstm.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testStartWidgetUpdater() {
        val c = instrumentation.targetContext
        val NUM_COUNTDOWNS = 2
        val TIMEOUT = 3 * PlaybackServiceTaskManager.WIDGET_UPDATER_NOTIFICATION_INTERVAL
        val countDownLatch = CountDownLatch(NUM_COUNTDOWNS)
        val pstm = PlaybackServiceTaskManager(c, object : PlaybackServiceTaskManager.PSTMCallback {
            override fun positionSaverTick() {

            }

            override fun onSleepTimerAlmostExpired() {

            }

            override fun onSleepTimerExpired() {

            }

            override fun onSleepTimerReset() {

            }

            override fun onWidgetUpdaterTick() {
                countDownLatch.countDown()
            }

            override fun onChapterLoaded(media: Playable) {

            }
        })
        pstm.startWidgetUpdater()
        countDownLatch.await(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        pstm.shutdown()
    }

    fun testIsWidgetUpdaterActive() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.startWidgetUpdater()
        Assert.assertTrue(pstm.isWidgetUpdaterActive)
        pstm.shutdown()
    }

    fun testCancelWidgetUpdater() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.startWidgetUpdater()
        pstm.cancelWidgetUpdater()
        Assert.assertFalse(pstm.isWidgetUpdaterActive)
        pstm.shutdown()
    }

    fun testCancelAllTasksNoTasksStarted() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.cancelAllTasks()
        Assert.assertFalse(pstm.isPositionSaverActive)
        Assert.assertFalse(pstm.isWidgetUpdaterActive)
        Assert.assertFalse(pstm.isSleepTimerActive)
        pstm.shutdown()
    }

    fun testCancelAllTasksAllTasksStarted() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.startWidgetUpdater()
        pstm.startPositionSaver()
        pstm.setSleepTimer(100000, false, false)
        pstm.cancelAllTasks()
        Assert.assertFalse(pstm.isPositionSaverActive)
        Assert.assertFalse(pstm.isWidgetUpdaterActive)
        Assert.assertFalse(pstm.isSleepTimerActive)
        pstm.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testSetSleepTimer() {
        val c = instrumentation.targetContext
        val TIME: Long = 2000
        val TIMEOUT = 2 * TIME
        val countDownLatch = CountDownLatch(1)
        val pstm = PlaybackServiceTaskManager(c, object : PlaybackServiceTaskManager.PSTMCallback {
            override fun positionSaverTick() {

            }

            override fun onSleepTimerAlmostExpired() {

            }

            override fun onSleepTimerExpired() {
                if (countDownLatch.count == 0) {
                    Assert.fail()
                }
                countDownLatch.countDown()
            }

            override fun onSleepTimerReset() {

            }

            override fun onWidgetUpdaterTick() {

            }

            override fun onChapterLoaded(media: Playable) {

            }
        })
        pstm.setSleepTimer(TIME, false, false)
        countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        pstm.shutdown()
    }

    @Throws(InterruptedException::class)
    fun testDisableSleepTimer() {
        val c = instrumentation.targetContext
        val TIME: Long = 1000
        val TIMEOUT = 2 * TIME
        val countDownLatch = CountDownLatch(1)
        val pstm = PlaybackServiceTaskManager(c, object : PlaybackServiceTaskManager.PSTMCallback {
            override fun positionSaverTick() {

            }

            override fun onSleepTimerAlmostExpired() {

            }

            override fun onSleepTimerExpired() {
                Assert.fail("Sleeptimer expired")
            }

            override fun onSleepTimerReset() {

            }

            override fun onWidgetUpdaterTick() {

            }

            override fun onChapterLoaded(media: Playable) {

            }
        })
        pstm.setSleepTimer(TIME, false, false)
        pstm.disableSleepTimer()
        Assert.assertFalse(countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        pstm.shutdown()
    }

    fun testIsSleepTimerActivePositive() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.setSleepTimer(10000, false, false)
        Assert.assertTrue(pstm.isSleepTimerActive)
        pstm.shutdown()
    }

    fun testIsSleepTimerActiveNegative() {
        val c = instrumentation.targetContext
        val pstm = PlaybackServiceTaskManager(c, defaultPSTM)
        pstm.setSleepTimer(10000, false, false)
        pstm.disableSleepTimer()
        Assert.assertFalse(pstm.isSleepTimerActive)
        pstm.shutdown()
    }

    private val defaultPSTM = object : PlaybackServiceTaskManager.PSTMCallback {
        override fun positionSaverTick() {

        }

        override fun onSleepTimerAlmostExpired() {

        }

        override fun onSleepTimerExpired() {

        }

        override fun onSleepTimerReset() {

        }

        override fun onWidgetUpdaterTick() {

        }

        override fun onChapterLoaded(media: Playable) {

        }
    }
}
