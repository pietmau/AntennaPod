package de.test.antennapod.ui

import android.content.Context
import android.content.SharedPreferences
import android.test.ActivityInstrumentationTestCase2
import android.test.FlakyTest
import android.widget.ListView

import com.robotium.solo.Solo

import java.util.ArrayList
import java.util.Arrays

import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.activity.OnlineFeedViewActivity
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.storage.PodDBAdapter
import de.danoeh.antennapod.fragment.DownloadsFragment
import de.danoeh.antennapod.fragment.EpisodesFragment
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment
import de.danoeh.antennapod.fragment.QueueFragment
import de.danoeh.antennapod.preferences.PreferenceController

/**
 * User interface tests for MainActivity
 */
class MainActivityTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {

    private var solo: Solo? = null
    private var uiTestUtils: UITestUtils? = null

    private var prefs: SharedPreferences? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val context = instrumentation.targetContext
        uiTestUtils = UITestUtils(context)
        uiTestUtils!!.setup()

        // create new database
        PodDBAdapter.init(context)
        PodDBAdapter.deleteDatabase()
        val adapter = PodDBAdapter.getInstance()
        adapter.open()
        adapter.close()

        // override first launch preference
        // do this BEFORE calling getActivity()!
        prefs = instrumentation.targetContext.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE)
        prefs!!.edit().putBoolean(MainActivity.PREF_IS_FIRST_LAUNCH, false).commit()

        solo = Solo(instrumentation, activity)
    }

    @Throws(Exception::class)
    override fun tearDown() {
        uiTestUtils!!.tearDown()
        solo!!.finishOpenedActivities()

        PodDBAdapter.deleteDatabase()

        // reset preferences
        prefs!!.edit().clear().commit()

        super.tearDown()
    }

    private fun openNavDrawer() {
        solo!!.clickOnImageButton(0)
        instrumentation.waitForIdleSync()
    }

    @Throws(Exception::class)
    fun testAddFeed() {
        uiTestUtils!!.addHostedFeedData()
        val feed = uiTestUtils!!.hostedFeeds[0]
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.add_feed_label))
        solo!!.enterText(0, feed.download_url)
        solo!!.clickOnButton(solo!!.getString(R.string.confirm_label))
        solo!!.waitForActivity(OnlineFeedViewActivity::class.java)
        solo!!.waitForView(R.id.butSubscribe)
        assertEquals(solo!!.getString(R.string.subscribe_label), solo!!.getButton(0).getText().toString())
        solo!!.clickOnButton(0)
        solo!!.waitForText(solo!!.getString(R.string.subscribed_label))
    }

    @FlakyTest(tolerance = 3)
    @Throws(Exception::class)
    fun testClickNavDrawer() {
        uiTestUtils!!.addLocalFeedData(false)

        UserPreferences.setHiddenDrawerItems(ArrayList<String>())

        // queue
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.queue_label))
        solo!!.waitForView(android.R.id.list)
        assertEquals(solo!!.getString(R.string.queue_label), actionbarTitle)

        // episodes
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.episodes_label))
        solo!!.waitForView(android.R.id.list)
        assertEquals(solo!!.getString(R.string.episodes_label), actionbarTitle)

        // Subscriptions
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.subscriptions_label))
        solo!!.waitForView(R.id.subscriptions_grid)
        assertEquals(solo!!.getString(R.string.subscriptions_label), actionbarTitle)

        // downloads
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.downloads_label))
        solo!!.waitForView(android.R.id.list)
        assertEquals(solo!!.getString(R.string.downloads_label), actionbarTitle)

        // playback history
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.playback_history_label))
        solo!!.waitForView(android.R.id.list)
        assertEquals(solo!!.getString(R.string.playback_history_label), actionbarTitle)

        // add podcast
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.add_feed_label))
        solo!!.waitForView(R.id.txtvFeedurl)
        assertEquals(solo!!.getString(R.string.add_feed_label), actionbarTitle)

        // podcasts
        val list = solo!!.getView(R.id.nav_list) as ListView
        for (i in uiTestUtils!!.hostedFeeds.indices) {
            val f = uiTestUtils!!.hostedFeeds[i]
            openNavDrawer()
            solo!!.scrollListToLine(list, i)
            solo!!.clickOnText(f.title)
            solo!!.waitForView(android.R.id.list)
            Assert.assertEquals("", actionbarTitle)
        }
    }

    private val actionbarTitle: String
        get() = (solo!!.getCurrentActivity() as MainActivity).supportActionBar!!.title!!.toString()

    @FlakyTest(tolerance = 3)
    fun testGoToPreferences() {
        openNavDrawer()
        solo!!.clickOnText(solo!!.getString(R.string.settings_label))
        solo!!.waitForActivity(PreferenceController.getPreferenceActivity())
    }

    fun testDrawerPreferencesHideSomeElements() {
        UserPreferences.setHiddenDrawerItems(ArrayList<String>())
        openNavDrawer()
        solo!!.clickLongOnText(solo!!.getString(R.string.queue_label))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(solo!!.getString(R.string.episodes_label))
        solo!!.clickOnText(solo!!.getString(R.string.playback_history_label))
        solo!!.clickOnText(solo!!.getString(R.string.confirm_label))
        solo!!.waitForDialogToClose()
        val hidden = UserPreferences.getHiddenDrawerItems()
        Assert.assertEquals(2, hidden.size)
        Assert.assertTrue(hidden.contains(EpisodesFragment.TAG))
        Assert.assertTrue(hidden.contains(PlaybackHistoryFragment.TAG))
    }

    fun testDrawerPreferencesUnhideSomeElements() {
        var hidden = Arrays.asList(PlaybackHistoryFragment.TAG, DownloadsFragment.TAG)
        UserPreferences.setHiddenDrawerItems(hidden)
        openNavDrawer()
        solo!!.clickLongOnText(solo!!.getString(R.string.queue_label))
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(solo!!.getString(R.string.downloads_label))
        solo!!.clickOnText(solo!!.getString(R.string.queue_label))
        solo!!.clickOnText(solo!!.getString(R.string.confirm_label))
        solo!!.waitForDialogToClose()
        hidden = UserPreferences.getHiddenDrawerItems()
        Assert.assertEquals(2, hidden.size)
        Assert.assertTrue(hidden.contains(QueueFragment.TAG))
        Assert.assertTrue(hidden.contains(PlaybackHistoryFragment.TAG))
    }

    fun testDrawerPreferencesHideAllElements() {
        UserPreferences.setHiddenDrawerItems(ArrayList<String>())
        val titles = instrumentation.targetContext.resources.getStringArray(R.array.nav_drawer_titles)

        openNavDrawer()
        solo!!.clickLongOnText(solo!!.getString(R.string.queue_label))
        solo!!.waitForDialogToOpen()
        for (title in titles) {
            solo!!.clickOnText(title)
        }
        solo!!.clickOnText(solo!!.getString(R.string.confirm_label))
        solo!!.waitForDialogToClose()
        val hidden = UserPreferences.getHiddenDrawerItems()
        Assert.assertEquals(titles.size, hidden.size)
        for (tag in MainActivity.NAV_DRAWER_TAGS) {
            Assert.assertTrue(hidden.contains(tag))
        }
    }

    fun testDrawerPreferencesHideCurrentElement() {
        UserPreferences.setHiddenDrawerItems(ArrayList<String>())

        openNavDrawer()
        val downloads = solo!!.getString(R.string.downloads_label)
        solo!!.clickOnText(downloads)
        solo!!.waitForView(android.R.id.list)
        openNavDrawer()
        solo!!.clickLongOnText(downloads)
        solo!!.waitForDialogToOpen()
        solo!!.clickOnText(downloads)
        solo!!.clickOnText(solo!!.getString(R.string.confirm_label))
        solo!!.waitForDialogToClose()
        val hidden = UserPreferences.getHiddenDrawerItems()
        Assert.assertEquals(1, hidden.size)
        Assert.assertTrue(hidden.contains(DownloadsFragment.TAG))
    }
}
