package de.test.antennapod.entities

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.test.InstrumentationTestCase

import de.danoeh.antennapod.core.feed.MediaType
import de.danoeh.antennapod.core.util.playback.ExternalMedia

/**
 * Tests for [ExternalMedia] entity.
 */
class ExternalMediaTest : InstrumentationTestCase() {

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        clearSharedPrefs()
    }

    @SuppressLint("CommitPrefEdits")
    private fun clearSharedPrefs() {
        val prefs = defaultSharedPrefs
        val editor = prefs.edit()
        editor.clear()
        editor.commit()
    }

    private val defaultSharedPrefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)

    fun testSaveCurrentPositionUpdatesPreferences() {
        val POSITION = 50
        val LAST_PLAYED_TIME = 1650

        Assert.assertEquals(NOT_SET, defaultSharedPrefs.getInt(ExternalMedia.PREF_POSITION, NOT_SET))
        Assert.assertEquals(NOT_SET.toLong(), defaultSharedPrefs.getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET.toLong()))

        val media = ExternalMedia("source", MediaType.AUDIO)
        media.saveCurrentPosition(defaultSharedPrefs, POSITION, LAST_PLAYED_TIME.toLong())

        Assert.assertEquals(POSITION, defaultSharedPrefs.getInt(ExternalMedia.PREF_POSITION, NOT_SET))
        Assert.assertEquals(LAST_PLAYED_TIME.toLong(), defaultSharedPrefs.getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, NOT_SET.toLong()))
    }

    companion object {

        private val NOT_SET = -1
    }
}
