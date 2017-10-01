package de.test.antennapod.gpodnet

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.gpoddernet.GpodnetService
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetDevice
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetTag

import java.util.ArrayList
import java.util.Arrays

/**
 * Test class for GpodnetService
 */
class GPodnetServiceTest : AndroidTestCase() {

    private var service: GpodnetService? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        service = GpodnetService()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
    }

    @Throws(GpodnetServiceException::class)
    private fun authenticate() {
        service!!.authenticate(USER, PW)
    }

    @Throws(GpodnetServiceException::class)
    fun testUploadSubscription() {
        authenticate()
        val l = ArrayList<String>()
        l.add("http://bitsundso.de/feed")
        service!!.uploadSubscriptions(USER, "radio", l)
    }

    @Throws(GpodnetServiceException::class)
    fun testUploadSubscription2() {
        authenticate()
        val l = ArrayList<String>()
        l.add("http://bitsundso.de/feed")
        l.add("http://gamesundso.de/feed")
        service!!.uploadSubscriptions(USER, "radio", l)
    }

    @Throws(GpodnetServiceException::class)
    fun testUploadChanges() {
        authenticate()
        val URLS = arrayOf("http://bitsundso.de/feed", "http://gamesundso.de/feed", "http://cre.fm/feed/mp3/", "http://freakshow.fm/feed/m4a/")
        val subscriptions = Arrays.asList(URLS[0], URLS[1])
        val removed = Arrays.asList(URLS[0])
        val added = Arrays.asList(URLS[2], URLS[3])
        service!!.uploadSubscriptions(USER, "radio", subscriptions)
        service!!.uploadChanges(USER, "radio", added, removed)
    }

    @Throws(GpodnetServiceException::class)
    fun testGetSubscriptionChanges() {
        authenticate()
        service!!.getSubscriptionChanges(USER, "radio", 1362322610L)
    }

    @Throws(GpodnetServiceException::class)
    fun testGetSubscriptionsOfUser() {
        authenticate()
        service!!.getSubscriptionsOfUser(USER)
    }

    @Throws(GpodnetServiceException::class)
    fun testGetSubscriptionsOfDevice() {
        authenticate()
        service!!.getSubscriptionsOfDevice(USER, "radio")
    }

    @Throws(GpodnetServiceException::class)
    fun testConfigureDevices() {
        authenticate()
        service!!.configureDevice(USER, "foo", "This is an updated caption",
                GpodnetDevice.DeviceType.LAPTOP)
    }

    @Throws(GpodnetServiceException::class)
    fun testGetDevices() {
        authenticate()
        service!!.getDevices(USER)
    }

    @Throws(GpodnetServiceException::class)
    fun testGetSuggestions() {
        authenticate()
        service!!.getSuggestions(10)
    }

    @Throws(GpodnetServiceException::class)
    fun testTags() {
        service!!.getTopTags(20)
    }

    @Throws(GpodnetServiceException::class)
    fun testPodcastForTags() {
        val tags = service!!.getTopTags(20)
        service!!.getPodcastsForTag(tags[1],
                10)
    }

    @Throws(GpodnetServiceException::class)
    fun testSearch() {
        service!!.searchPodcasts("linux", 64)
    }

    @Throws(GpodnetServiceException::class)
    fun testToplist() {
        service!!.getPodcastToplist(10)
    }

    companion object {

        private val USER = ""
        private val PW = ""
    }
}
