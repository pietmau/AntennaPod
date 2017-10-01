package de.danoeh.antennapod.core.feed

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage
import de.danoeh.antennapod.core.feed.FeedMother.anyFeed
import junit.framework.Assert

class FeedTest : AndroidTestCase() {

    private var original: Feed? = null
    private var originalImage: FeedImage? = null
    private var changedFeed: Feed? = null

    override fun setUp() {
        original = anyFeed()
        originalImage = original!!.image
        changedFeed = anyFeed()
    }

    @Throws(Exception::class)
    fun testCompareWithOther_feedImageDownloadUrlChanged() {
        setNewFeedImageDownloadUrl()

        feedHasChanged()
    }

    @Throws(Exception::class)
    fun testCompareWithOther_sameFeedImage() {
        changedFeed!!.image = anyFeedImage()

        feedHasNotChanged()
    }

    @Throws(Exception::class)
    fun testCompareWithOther_feedImageRemoved() {
        feedImageRemoved()

        feedHasNotChanged()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedImageDownloadUrlChanged() {
        setNewFeedImageDownloadUrl()

        original!!.updateFromOther(changedFeed)

        feedImageWasUpdated()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedImageRemoved() {
        feedImageRemoved()

        original!!.updateFromOther(changedFeed)

        feedImageWasNotUpdated()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedImageAdded() {
        feedHadNoImage()
        setNewFeedImageDownloadUrl()

        original!!.updateFromOther(changedFeed)

        feedImageWasUpdated()
    }

    private fun feedHasNotChanged() {
        Assert.assertFalse(original!!.compareWithOther(changedFeed))
    }

    private fun feedHadNoImage() {
        original!!.image = null
    }

    private fun setNewFeedImageDownloadUrl() {
        changedFeed!!.image.setDownload_url("http://example.com/new_picture")
    }

    private fun feedHasChanged() {
        Assert.assertTrue(original!!.compareWithOther(changedFeed))
    }

    private fun feedImageRemoved() {
        changedFeed!!.image = null
    }

    private fun feedImageWasUpdated() {
        Assert.assertEquals(original!!.image.getDownload_url(), changedFeed!!.image.getDownload_url())
    }

    private fun feedImageWasNotUpdated() {
        Assert.assertTrue(originalImage === original!!.image)
    }

}