package de.test.antennapod.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.test.ActivityInstrumentationTestCase2
import android.test.FlakyTest
import android.view.View
import android.widget.ListView

import com.robotium.solo.Solo
import com.robotium.solo.Timeout

import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.service.playback.PlaybackService
import de.danoeh.antennapod.core.service.playback.PlayerStatus
import de.danoeh.antennapod.core.storage.DBReader
import de.danoeh.antennapod.core.storage.DBWriter
import de.danoeh.antennapod.core.storage.PodDBAdapter

/**
 * test cases for starting and ending playback from the MainActivity and AudioPlayerActivity
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class PlaybackSonicTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {

    private var solo: Solo? = null
    private var uiTestUtils: UITestUtils? = null

    private var context: Context? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()

        context = instrumentation.targetContext

        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
                .clear()
                .putBoolean(UserPreferences.PREF_UNPAUSE_ON_HEADSET_RECONNECT, false)
                .putBoolean(UserPreferences.PREF_PAUSE_ON_HEADSET_DISCONNECT, false)
                .putBoolean(UserPreferences.PREF_SONIC, true)
                .commit()

        solo = Solo(instrumentation, activity)

        uiTestUtils = UITestUtils(context)
        uiTestUtils!!.setup()

        // create database
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        solo!!.finishOpenedActivities()
        uiTestUtils!!.tearDown()

        // shut down playback service
        skipEpisode()
        context!!.sendBroadcast(Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE))

        super.tearDown()
    }

    private fun openNavDrawer() {
        solo!!.clickOnImageButton(0)
        instrumentation.waitForIdleSync()
    }

    private fun setContinuousPlaybackPreference(value: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).commit()
    }

    private fun skipEpisode() {
        val skipIntent = Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE)
        context!!.sendBroadcast(skipIntent)
    }

    private fun startLocalPlayback() {
        openNavDrawer()
        // if we try to just click on plain old text then
        // we might wind up clicking on the fragment title and not
        // the drawer element like we want.
        val drawerView = solo!!.getView(R.id.nav_list) as ListView
        // this should be 'Episodes'
        val targetView = drawerView.getChildAt(EPISODES_DRAWER_LIST_INDEX)
        solo!!.waitForView(targetView)
        solo!!.clickOnView(targetView)
        instrumentation.waitForIdleSync()
        solo!!.waitForText(solo!!.getString(R.string.all_episodes_short_label))
        solo!!.clickOnText(solo!!.getString(R.string.all_episodes_short_label))
        instrumentation.waitForIdleSync()

        val episodes = DBReader.getRecentlyPublishedEpisodes(10)
        Assert.assertTrue(solo!!.waitForView(solo!!.getView(R.id.butSecondaryAction)))

        solo!!.clickOnView(solo!!.getView(R.id.butSecondaryAction))
        val mediaId = episodes[0].media!!.id
        val playing = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == mediaId
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getSmallTimeout())
        Assert.assertTrue(playing)
    }

    private fun startLocalPlaybackFromQueue() {
        openNavDrawer()

        // if we try to just click on plain old text then
        // we might wind up clicking on the fragment title and not
        // the drawer element like we want.
        val drawerView = solo!!.getView(R.id.nav_list) as ListView
        // this should be 'Queue'
        val targetView = drawerView.getChildAt(QUEUE_DRAWER_LIST_INDEX)
        solo!!.waitForView(targetView)
        instrumentation.waitForIdleSync()
        solo!!.clickOnView(targetView)
        Assert.assertTrue(solo!!.waitForView(solo!!.getView(R.id.butSecondaryAction)))

        val queue = DBReader.getQueue()
        solo!!.clickOnImageButton(1)
        Assert.assertTrue(solo!!.waitForView(solo!!.getView(R.id.butPlay)))
        val mediaId = queue[0].media!!.id
        val playing = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == mediaId
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getSmallTimeout())
        Assert.assertTrue(playing)
    }

    @Throws(Exception::class)
    fun testStartLocal() {
        uiTestUtils!!.addLocalFeedData(true)
        DBWriter.clearQueue().get()
        startLocalPlayback()
    }

    @Throws(Exception::class)
    fun testContinousPlaybackOffSingleEpisode() {
        setContinuousPlaybackPreference(false)
        uiTestUtils!!.addLocalFeedData(true)
        DBWriter.clearQueue().get()
        startLocalPlayback()
    }

    @FlakyTest(tolerance = 3)
    @Throws(Exception::class)
    fun testContinousPlaybackOffMultipleEpisodes() {
        setContinuousPlaybackPreference(false)
        uiTestUtils!!.addLocalFeedData(true)
        val queue = DBReader.getQueue()
        val first = queue[0]

        startLocalPlaybackFromQueue()
        val stopped = solo!!.waitForCondition({
            if (uiTestUtils!!.getPlaybackController(activity).status != PlayerStatus.PLAYING) {
                return@solo.waitForCondition true
            } else if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() != first.media!!.id
            } else {
                return@solo.waitForCondition true
            }
        }, Timeout.getSmallTimeout())
        Assert.assertTrue(stopped)
        Thread.sleep(1000)
        val status = uiTestUtils!!.getPlaybackController(activity).status
        Assert.assertFalse(status == PlayerStatus.PLAYING)
    }

    @FlakyTest(tolerance = 3)
    @Throws(Exception::class)
    fun testContinuousPlaybackOnMultipleEpisodes() {
        setContinuousPlaybackPreference(true)
        uiTestUtils!!.addLocalFeedData(true)
        val queue = DBReader.getQueue()
        val first = queue[0]
        val second = queue[1]

        startLocalPlaybackFromQueue()
        val firstPlaying = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == first.media!!.id
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getSmallTimeout())
        Assert.assertTrue(firstPlaying)
        val secondPlaying = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == second.media!!.id
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getLargeTimeout())
        Assert.assertTrue(secondPlaying)
    }

    /**
     * Check if an episode can be played twice without problems.
     */
    @Throws(Exception::class)
    private fun replayEpisodeCheck(followQueue: Boolean) {
        setContinuousPlaybackPreference(followQueue)
        uiTestUtils!!.addLocalFeedData(true)
        DBWriter.clearQueue().get()
        val episodes = DBReader.getRecentlyPublishedEpisodes(10)

        startLocalPlayback()
        val mediaId = episodes[0].media!!.id
        val startedPlaying = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == mediaId
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getSmallTimeout())
        Assert.assertTrue(startedPlaying)

        val stoppedPlaying = solo!!.waitForCondition({ uiTestUtils!!.getCurrentMedia(activity) == null || uiTestUtils!!.getCurrentMedia(activity).id != mediaId }, Timeout.getLargeTimeout())
        Assert.assertTrue(stoppedPlaying)

        startLocalPlayback()
        val startedReplay = solo!!.waitForCondition({
            if (uiTestUtils!!.getCurrentMedia(activity) != null) {
                return@solo.waitForCondition uiTestUtils !!. getCurrentMedia activity.getId() == mediaId
            } else {
                return@solo.waitForCondition false
            }
        }, Timeout.getLargeTimeout())
        Assert.assertTrue(startedReplay)
    }

    @Throws(Exception::class)
    fun testReplayEpisodeContinuousPlaybackOn() {
        replayEpisodeCheck(true)
    }

    @Throws(Exception::class)
    fun testReplayEpisodeContinuousPlaybackOff() {
        replayEpisodeCheck(false)
    }

    companion object {

        private val TAG = PlaybackTest::class.java.simpleName
        val EPISODES_DRAWER_LIST_INDEX = 1
        val QUEUE_DRAWER_LIST_INDEX = 0
    }


}
