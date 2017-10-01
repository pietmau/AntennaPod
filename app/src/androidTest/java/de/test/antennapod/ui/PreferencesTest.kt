package de.test.antennapod.ui

import android.content.Context
import android.content.res.Resources
import android.test.ActivityInstrumentationTestCase2
import android.util.Log

import com.robotium.solo.Solo
import com.robotium.solo.Timeout

import java.util.Arrays
import java.util.concurrent.TimeUnit

import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.PreferenceActivity
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm
import de.danoeh.antennapod.core.storage.APNullCleanupAlgorithm
import de.danoeh.antennapod.core.storage.APQueueCleanupAlgorithm
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm

class PreferencesTest : ActivityInstrumentationTestCase2<PreferenceActivity>(PreferenceActivity::class.java) {

    private var solo: Solo? = null
    private var context: Context? = null
    private var res: Resources? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        solo = Solo(instrumentation, activity)
        Timeout.setSmallTimeout(500)
        Timeout.setLargeTimeout(1000)
        context = instrumentation.targetContext
        res = activity.resources
        UserPreferences.init(context!!)
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        solo!!.finishOpenedActivities()
        super.tearDown()
    }

    fun testSwitchTheme() {
        val theme = UserPreferences.getTheme()
        val otherTheme: Int
        if (theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark
        } else {
            otherTheme = R.string.pref_theme_title_light
        }
        solo!!.clickOnText(solo!!.getString(R.string.pref_set_theme_title))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(solo!!.getString(otherTheme))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getTheme() != theme }, Timeout.getLargeTimeout()))
    }

    fun testSwitchThemeBack() {
        val theme = UserPreferences.getTheme()
        val otherTheme: Int
        if (theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark
        } else {
            otherTheme = R.string.pref_theme_title_light
        }
        solo!!.clickOnText(solo!!.getString(R.string.pref_set_theme_title))
        solo!!.waitForDialogToOpen(1000)
        solo!!.clickOnText(solo!!.getString(otherTheme))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getTheme() != theme }, Timeout.getLargeTimeout()))
    }

    fun testExpandNotification() {
        val priority = UserPreferences.getNotifyPriority()
        solo!!.clickOnText(solo!!.getString(R.string.pref_expandNotify_title))
        Assert.assertTrue(solo!!.waitForCondition({ priority != UserPreferences.getNotifyPriority() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_expandNotify_title))
        Assert.assertTrue(solo!!.waitForCondition({ priority == UserPreferences.getNotifyPriority() }, Timeout.getLargeTimeout()))
    }

    fun testEnablePersistentPlaybackControls() {
        val persistNotify = UserPreferences.isPersistNotify()
        solo!!.clickOnText(solo!!.getString(R.string.pref_persistNotify_title))
        Assert.assertTrue(solo!!.waitForCondition({ persistNotify != UserPreferences.isPersistNotify() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_persistNotify_title))
        Assert.assertTrue(solo!!.waitForCondition({ persistNotify == UserPreferences.isPersistNotify() }, Timeout.getLargeTimeout()))
    }

    fun testSetLockscreenButtons() {
        val buttons = res!!.getStringArray(R.array.compact_notification_buttons_options)
        solo!!.clickOnText(solo!!.getString(R.string.pref_compact_notification_buttons_title))
        solo!!.waitForDialogToOpen(1000)
        // First uncheck every checkbox
        for (i in buttons.indices) {
            Assert.assertTrue(solo!!.searchText(buttons[i]))
            if (solo!!.isTextChecked(buttons[i])) {
                solo!!.clickOnText(buttons[i])
            }
        }
        // Now try to check all checkboxes
        solo!!.clickOnText(buttons[0])
        solo!!.clickOnText(buttons[1])
        solo!!.clickOnText(buttons[2])
        // Make sure that the third checkbox is unchecked
        Assert.assertTrue(!solo!!.isTextChecked(buttons[2]))
        solo!!.clickOnText(solo!!.getString(R.string.confirm_label))
        solo!!.waitForDialogToClose(1000)
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.showRewindOnCompactNotification() }, Timeout.getLargeTimeout()))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.showFastForwardOnCompactNotification() }, Timeout.getLargeTimeout()))
        Assert.assertTrue(solo!!.waitForCondition({ !UserPreferences.showSkipOnCompactNotification() }, Timeout.getLargeTimeout()))
    }

    fun testEnqueueAtFront() {
        val enqueueAtFront = UserPreferences.enqueueAtFront()
        solo!!.clickOnText(solo!!.getString(R.string.pref_queueAddToFront_title))
        Assert.assertTrue(solo!!.waitForCondition({ enqueueAtFront != UserPreferences.enqueueAtFront() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_queueAddToFront_title))
        Assert.assertTrue(solo!!.waitForCondition({ enqueueAtFront == UserPreferences.enqueueAtFront() }, Timeout.getLargeTimeout()))
    }

    fun testHeadPhonesDisconnect() {
        val pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect()
        solo!!.clickOnText(solo!!.getString(R.string.pref_pauseOnHeadsetDisconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_pauseOnHeadsetDisconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect() }, Timeout.getLargeTimeout()))
    }

    fun testHeadPhonesReconnect() {
        if (UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            solo!!.clickOnText(solo!!.getString(R.string.pref_pauseOnHeadsetDisconnect_title))
            Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.isPauseOnHeadsetDisconnect() }, Timeout.getLargeTimeout()))
        }
        val unpauseOnHeadsetReconnect = UserPreferences.isUnpauseOnHeadsetReconnect()
        solo!!.clickOnText(solo!!.getString(R.string.pref_unpauseOnHeadsetReconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ unpauseOnHeadsetReconnect != UserPreferences.isUnpauseOnHeadsetReconnect() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_unpauseOnHeadsetReconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ unpauseOnHeadsetReconnect == UserPreferences.isUnpauseOnHeadsetReconnect() }, Timeout.getLargeTimeout()))
    }

    fun testBluetoothReconnect() {
        if (UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            solo!!.clickOnText(solo!!.getString(R.string.pref_pauseOnHeadsetDisconnect_title))
            Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.isPauseOnHeadsetDisconnect() }, Timeout.getLargeTimeout()))
        }
        val unpauseOnBluetoothReconnect = UserPreferences.isUnpauseOnBluetoothReconnect()
        solo!!.clickOnText(solo!!.getString(R.string.pref_unpauseOnBluetoothReconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ unpauseOnBluetoothReconnect != UserPreferences.isUnpauseOnBluetoothReconnect() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_unpauseOnBluetoothReconnect_title))
        Assert.assertTrue(solo!!.waitForCondition({ unpauseOnBluetoothReconnect == UserPreferences.isUnpauseOnBluetoothReconnect() }, Timeout.getLargeTimeout()))
    }

    fun testContinuousPlayback() {
        val continuousPlayback = UserPreferences.isFollowQueue()
        solo!!.clickOnText(solo!!.getString(R.string.pref_followQueue_title))
        Assert.assertTrue(solo!!.waitForCondition({ continuousPlayback != UserPreferences.isFollowQueue() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_followQueue_title))
        Assert.assertTrue(solo!!.waitForCondition({ continuousPlayback == UserPreferences.isFollowQueue() }, Timeout.getLargeTimeout()))
    }

    fun testAutoDelete() {
        val autoDelete = UserPreferences.isAutoDelete()
        solo!!.clickOnText(solo!!.getString(R.string.pref_auto_delete_title))
        Assert.assertTrue(solo!!.waitForCondition({ autoDelete != UserPreferences.isAutoDelete() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_auto_delete_title))
        Assert.assertTrue(solo!!.waitForCondition({ autoDelete == UserPreferences.isAutoDelete() }, Timeout.getLargeTimeout()))
    }

    fun testPlaybackSpeeds() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_playback_speed_title))
        solo!!.waitForDialogToOpen(1000)
        Assert.assertTrue(solo!!.searchText(res!!.getStringArray(R.array.playback_speed_values)[0]))
        solo!!.clickOnText(solo!!.getString(R.string.cancel_label))
        solo!!.waitForDialogToClose(1000)
    }

    fun testPauseForInterruptions() {
        val pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss()
        solo!!.clickOnText(solo!!.getString(R.string.pref_pausePlaybackForFocusLoss_title))
        Assert.assertTrue(solo!!.waitForCondition({ pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_pausePlaybackForFocusLoss_title))
        Assert.assertTrue(solo!!.waitForCondition({ pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss() }, Timeout.getLargeTimeout()))
    }

    fun testDisableUpdateInterval() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_autoUpdateIntervallOrTime_sum))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(solo!!.getString(R.string.pref_autoUpdateIntervallOrTime_Disable))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getUpdateInterval() == 0 }, 1000))
    }

    fun testSetUpdateInterval() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_autoUpdateIntervallOrTime_title))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(solo!!.getString(R.string.pref_autoUpdateIntervallOrTime_Interval))
        solo!!.waitForDialogToOpen()
        val search = "12 " + solo!!.getString(R.string.pref_update_interval_hours_plural)
        solo!!.clickOnText(search)
        solo!!.waitForDialogToClose()
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getUpdateInterval() == TimeUnit.HOURS.toMillis(12) }, Timeout.getLargeTimeout()))
    }

    fun testMobileUpdates() {
        val mobileUpdates = UserPreferences.isAllowMobileUpdate()
        solo!!.clickOnText(solo!!.getString(R.string.pref_mobileUpdate_title))
        Assert.assertTrue(solo!!.waitForCondition({ mobileUpdates != UserPreferences.isAllowMobileUpdate() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_mobileUpdate_title))
        Assert.assertTrue(solo!!.waitForCondition({ mobileUpdates == UserPreferences.isAllowMobileUpdate() }, Timeout.getLargeTimeout()))
    }

    fun testSetSequentialDownload() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_parallel_downloads_title))
        solo!!.waitForDialogToOpen()
        solo!!.clearEditText(0)
        solo!!.enterText(0, "1")
        solo!!.clickOnText(solo!!.getString(android.R.string.ok))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getParallelDownloads() == 1 }, Timeout.getLargeTimeout()))
    }

    fun testSetParallelDownloads() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_parallel_downloads_title))
        solo!!.waitForDialogToOpen()
        solo!!.clearEditText(0)
        solo!!.enterText(0, "10")
        solo!!.clickOnText(solo!!.getString(android.R.string.ok))
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getParallelDownloads() == 10 }, Timeout.getLargeTimeout()))
    }

    fun testSetParallelDownloadsInvalidInput() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_parallel_downloads_title))
        solo!!.waitForDialogToOpen()
        solo!!.clearEditText(0)
        solo!!.enterText(0, "0")
        assertEquals("1", solo!!.getEditText(0).getText().toString())
        solo!!.clearEditText(0)
        solo!!.enterText(0, "100")
        assertEquals("50", solo!!.getEditText(0).getText().toString())
    }

    fun testSetEpisodeCache() {
        val entries = res!!.getStringArray(R.array.episode_cache_size_entries)
        val values = res!!.getStringArray(R.array.episode_cache_size_values)
        val entry = entries[entries.size / 2]
        val value = Integer.valueOf(values[values.size / 2])!!
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.waitForText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cache_title))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(entry)
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getEpisodeCacheSize() == value }, Timeout.getLargeTimeout()))
    }

    fun testSetEpisodeCacheMin() {
        val entries = res!!.getStringArray(R.array.episode_cache_size_entries)
        val values = res!!.getStringArray(R.array.episode_cache_size_values)
        val minEntry = entries[0]
        val minValue = Integer.valueOf(values[0])!!
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.waitForText(solo!!.getString(R.string.pref_automatic_download_title))
        if (!UserPreferences.isEnableAutodownload()) {
            solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        }
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cache_title))
        solo!!.waitForDialogToOpen(1000)
        solo!!.scrollUp()
        solo!!.clickOnText(minEntry)
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getEpisodeCacheSize() == minValue }, Timeout.getLargeTimeout()))
    }

    fun testSetEpisodeCacheMax() {
        val entries = res!!.getStringArray(R.array.episode_cache_size_entries)
        val values = res!!.getStringArray(R.array.episode_cache_size_values)
        val maxEntry = entries[entries.size - 1]
        val maxValue = Integer.valueOf(values[values.size - 1])!!
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.waitForText(solo!!.getString(R.string.pref_automatic_download_title))
        if (!UserPreferences.isEnableAutodownload()) {
            solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        }
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cache_title))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(maxEntry)
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getEpisodeCacheSize() == maxValue }, Timeout.getLargeTimeout()))
    }

    fun testAutomaticDownload() {
        val automaticDownload = UserPreferences.isEnableAutodownload()
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.waitForText(solo!!.getString(R.string.pref_automatic_download_title))
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        Assert.assertTrue(solo!!.waitForCondition({ automaticDownload != UserPreferences.isEnableAutodownload() }, Timeout.getLargeTimeout()))
        if (UserPreferences.isEnableAutodownload() == false) {
            solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_title))
        }
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.isEnableAutodownload() == true }, Timeout.getLargeTimeout()))
        val enableAutodownloadOnBattery = UserPreferences.isEnableAutodownloadOnBattery()
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_on_battery_title))
        Assert.assertTrue(solo!!.waitForCondition({ enableAutodownloadOnBattery != UserPreferences.isEnableAutodownloadOnBattery() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_automatic_download_on_battery_title))
        Assert.assertTrue(solo!!.waitForCondition({ enableAutodownloadOnBattery == UserPreferences.isEnableAutodownloadOnBattery() }, Timeout.getLargeTimeout()))
        val enableWifiFilter = UserPreferences.isEnableAutodownloadWifiFilter()
        solo!!.clickOnText(solo!!.getString(R.string.pref_autodl_wifi_filter_title))
        Assert.assertTrue(solo!!.waitForCondition({ enableWifiFilter != UserPreferences.isEnableAutodownloadWifiFilter() }, Timeout.getLargeTimeout()))
        solo!!.clickOnText(solo!!.getString(R.string.pref_autodl_wifi_filter_title))
        Assert.assertTrue(solo!!.waitForCondition({ enableWifiFilter == UserPreferences.isEnableAutodownloadWifiFilter() }, Timeout.getLargeTimeout()))
    }

    fun testEpisodeCleanupQueueOnly() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cleanup_title))
        solo!!.waitForText(solo!!.getString(R.string.episode_cleanup_queue_removal))
        solo!!.clickOnText(solo!!.getString(R.string.episode_cleanup_queue_removal))
        Assert.assertTrue(solo!!.waitForCondition({
            val alg = UserPreferences.getEpisodeCleanupAlgorithm()
            alg is APQueueCleanupAlgorithm
        },
                Timeout.getLargeTimeout()))
    }

    fun testEpisodeCleanupNeverAlg() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cleanup_title))
        solo!!.waitForText(solo!!.getString(R.string.episode_cleanup_never))
        solo!!.clickOnText(solo!!.getString(R.string.episode_cleanup_never))
        Assert.assertTrue(solo!!.waitForCondition({
            val alg = UserPreferences.getEpisodeCleanupAlgorithm()
            alg is APNullCleanupAlgorithm
        },
                Timeout.getLargeTimeout()))
    }

    fun testEpisodeCleanupClassic() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cleanup_title))
        solo!!.waitForText(solo!!.getString(R.string.episode_cleanup_after_listening))
        solo!!.clickOnText(solo!!.getString(R.string.episode_cleanup_after_listening))
        Assert.assertTrue(solo!!.waitForCondition({
            val alg = UserPreferences.getEpisodeCleanupAlgorithm()
            if (alg is APCleanupAlgorithm) {
                val cleanupAlg = alg
                return@solo.waitForCondition cleanupAlg . getNumberOfDaysAfterPlayback () == 0
            }
            false
        },
                Timeout.getLargeTimeout()))
    }

    fun testEpisodeCleanupNumDays() {
        solo!!.clickOnText(solo!!.getString(R.string.pref_episode_cleanup_title))
        solo!!.waitForText(solo!!.getString(R.string.episode_cleanup_after_listening))
        solo!!.clickOnText("5")
        Assert.assertTrue(solo!!.waitForCondition({
            val alg = UserPreferences.getEpisodeCleanupAlgorithm()
            if (alg is APCleanupAlgorithm) {
                val cleanupAlg = alg
                return@solo.waitForCondition cleanupAlg . getNumberOfDaysAfterPlayback () == 5
            }
            false
        },
                Timeout.getLargeTimeout()))
    }


    fun testRewindChange() {
        val seconds = UserPreferences.getRewindSecs()
        val deltas = res!!.getIntArray(R.array.seek_delta_values)

        solo!!.clickOnText(solo!!.getString(R.string.pref_rewind))
        solo!!.waitForDialogToOpen()

        val currentIndex = Arrays.binarySearch(deltas, seconds)
        Assert.assertTrue(currentIndex >= 0 && currentIndex < deltas.size)  // found?

        // Find next value (wrapping around to next)
        val newIndex = (currentIndex + 1) % deltas.size

        solo!!.clickOnText(deltas[newIndex].toString() + " seconds")
        solo!!.clickOnButton("Confirm")

        solo!!.waitForDialogToClose()
        Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getRewindSecs() == deltas[newIndex] },
                Timeout.getLargeTimeout()))
    }

    fun testFastForwardChange() {
        for (i in 2 downTo 1) { // repeat twice to catch any error where fastforward is tracking rewind
            val seconds = UserPreferences.getFastForwardSecs()
            val deltas = res!!.getIntArray(R.array.seek_delta_values)

            solo!!.clickOnText(solo!!.getString(R.string.pref_fast_forward))
            solo!!.waitForDialogToOpen()

            val currentIndex = Arrays.binarySearch(deltas, seconds)
            Assert.assertTrue(currentIndex >= 0 && currentIndex < deltas.size)  // found?

            // Find next value (wrapping around to next)
            val newIndex = (currentIndex + 1) % deltas.size

            solo!!.clickOnText(deltas[newIndex].toString() + " seconds")
            solo!!.clickOnButton("Confirm")

            solo!!.waitForDialogToClose()
            Assert.assertTrue(solo!!.waitForCondition({ UserPreferences.getFastForwardSecs() == deltas[newIndex] },
                    Timeout.getLargeTimeout()))
        }
    }

    companion object {

        private val TAG = "PreferencesTest"
    }
}
