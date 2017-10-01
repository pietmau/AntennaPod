package de.danoeh.antennapod.core.feed

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.feed.FeedItemMother.anyFeedItemWithImage
import junit.framework.Assert

class FeedItemTest : AndroidTestCase() {

    private var original: FeedItem? = null
    private var originalImage: FeedImage? = null
    private var changedFeedItem: FeedItem? = null

    override fun setUp() {
        original = anyFeedItemWithImage()
        originalImage = original!!.image
        changedFeedItem = anyFeedItemWithImage()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedItemImageDownloadUrlChanged() {
        setNewFeedItemImageDownloadUrl()

        original!!.updateFromOther(changedFeedItem)

        feedItemImageWasUpdated()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedItemImageRemoved() {
        feedItemImageRemoved()

        original!!.updateFromOther(changedFeedItem)

        feedItemImageWasNotUpdated()
    }

    @Throws(Exception::class)
    fun testUpdateFromOther_feedItemImageAdded() {
        feedItemHadNoImage()
        setNewFeedItemImageDownloadUrl()

        original!!.updateFromOther(changedFeedItem)

        feedItemImageWasUpdated()
    }

    private fun feedItemHadNoImage() {
        original!!.image = null
    }

    private fun setNewFeedItemImageDownloadUrl() {
        changedFeedItem!!.image.setDownload_url("http://example.com/new_picture")
    }

    private fun feedItemImageRemoved() {
        changedFeedItem!!.image = null
    }

    private fun feedItemImageWasUpdated() {
        Assert.assertEquals(original!!.image.getDownload_url(), changedFeedItem!!.image.getDownload_url())
    }

    private fun feedItemImageWasNotUpdated() {
        Assert.assertTrue(originalImage === original!!.image)
    }

}