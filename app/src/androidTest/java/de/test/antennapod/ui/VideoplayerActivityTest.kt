package de.test.antennapod.ui

import android.test.ActivityInstrumentationTestCase2

import com.robotium.solo.Solo

import de.danoeh.antennapod.activity.VideoplayerActivity

/**
 * Test class for VideoplayerActivity
 */
class VideoplayerActivityTest : ActivityInstrumentationTestCase2<VideoplayerActivity>(VideoplayerActivity::class.java) {

    private var solo: Solo? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        solo = Solo(instrumentation, activity)
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        solo!!.finishOpenedActivities()
        super.tearDown()
    }

    /**
     * Test if activity can be started.
     */
    @Throws(Exception::class)
    fun testStartActivity() {
        solo!!.waitForActivity(VideoplayerActivity::class.java)
    }
}
