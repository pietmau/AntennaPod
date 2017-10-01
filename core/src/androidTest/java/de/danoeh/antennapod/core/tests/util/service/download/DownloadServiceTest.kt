package de.danoeh.antennapod.core.tests.util.service.download

import android.test.AndroidTestCase

import java.util.ArrayList

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedImage
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.service.download.DownloadService
import junit.framework.Assert

class DownloadServiceTest : AndroidTestCase() {

    fun testRemoveDuplicateImages() {
        val items = ArrayList<FeedItem>()
        for (i in 0..49) {
            val item = FeedItem()
            val url = if (i % 5 == 0) "dupe_url" else String.format("url_%d", i)
            item.image = FeedImage(null, url, "")
            items.add(item)
        }
        val feed = Feed()
        feed.items = items

        DownloadService.removeDuplicateImages(feed)

        Assert.assertEquals(50, items.size)
        for (i in items.indices) {
            val item = items[i]
            val want = if (i == 0) "dupe_url" else if (i % 5 == 0) null else String.format("url_%d", i)
            Assert.assertEquals(want, item.imageLocation)
        }
    }
}
