package de.test.antennapod.feed

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.feed.FeedFilter
import de.danoeh.antennapod.core.feed.FeedItem

class FeedFilterTest : AndroidTestCase() {

    @Throws(Exception::class)
    fun testNullFilter() {
        val filter = FeedFilter()
        val item = FeedItem()
        item.title = "Hello world"

        Assert.assertTrue(!filter.excludeOnly())
        Assert.assertTrue(!filter.includeOnly())
        Assert.assertEquals("", filter.excludeFilter)
        Assert.assertEquals("", filter.includeFilter)
        Assert.assertTrue(filter.shouldAutoDownload(item))
    }

    @Throws(Exception::class)
    fun testBasicIncludeFilter() {
        val includeFilter = "Hello"
        val filter = FeedFilter(includeFilter, "")
        val item = FeedItem()
        item.title = "Hello world"

        val item2 = FeedItem()
        item2.title = "Don't include me"

        Assert.assertTrue(!filter.excludeOnly())
        Assert.assertTrue(filter.includeOnly())
        Assert.assertEquals("", filter.excludeFilter)
        Assert.assertEquals(includeFilter, filter.includeFilter)
        Assert.assertTrue(filter.shouldAutoDownload(item))
        Assert.assertTrue(!filter.shouldAutoDownload(item2))
    }

    @Throws(Exception::class)
    fun testBasicExcludeFilter() {
        val excludeFilter = "Hello"
        val filter = FeedFilter("", excludeFilter)
        val item = FeedItem()
        item.title = "Hello world"

        val item2 = FeedItem()
        item2.title = "Item2"

        Assert.assertTrue(filter.excludeOnly())
        Assert.assertTrue(!filter.includeOnly())
        Assert.assertEquals(excludeFilter, filter.excludeFilter)
        Assert.assertEquals("", filter.includeFilter)
        Assert.assertTrue(!filter.shouldAutoDownload(item))
        Assert.assertTrue(filter.shouldAutoDownload(item2))
    }

    @Throws(Exception::class)
    fun testComplexIncludeFilter() {
        val includeFilter = "Hello \n\"Two words\""
        val filter = FeedFilter(includeFilter, "")
        val item = FeedItem()
        item.title = "hello world"

        val item2 = FeedItem()
        item2.title = "Two three words"

        val item3 = FeedItem()
        item3.title = "One two words"

        Assert.assertTrue(!filter.excludeOnly())
        Assert.assertTrue(filter.includeOnly())
        Assert.assertEquals("", filter.excludeFilter)
        Assert.assertEquals(includeFilter, filter.includeFilter)
        Assert.assertTrue(filter.shouldAutoDownload(item))
        Assert.assertTrue(!filter.shouldAutoDownload(item2))
        Assert.assertTrue(filter.shouldAutoDownload(item3))
    }

    @Throws(Exception::class)
    fun testComplexExcludeFilter() {
        val excludeFilter = "Hello \"Two words\""
        val filter = FeedFilter("", excludeFilter)
        val item = FeedItem()
        item.title = "hello world"

        val item2 = FeedItem()
        item2.title = "One three words"

        val item3 = FeedItem()
        item3.title = "One two words"

        Assert.assertTrue(filter.excludeOnly())
        Assert.assertTrue(!filter.includeOnly())
        Assert.assertEquals(excludeFilter, filter.excludeFilter)
        Assert.assertEquals("", filter.includeFilter)
        Assert.assertTrue(!filter.shouldAutoDownload(item))
        Assert.assertTrue(filter.shouldAutoDownload(item2))
        Assert.assertTrue(!filter.shouldAutoDownload(item3))
    }

    @Throws(Exception::class)
    fun testComboFilter() {
        val includeFilter = "Hello world"
        val excludeFilter = "dislike"
        val filter = FeedFilter(includeFilter, excludeFilter)

        val download = FeedItem()
        download.title = "Hello everyone!"
        // because, while it has words from the include filter it also has exclude words
        val doNotDownload = FeedItem()
        doNotDownload.title = "I dislike the world"
        // because it has no words from the include filter
        val doNotDownload2 = FeedItem()
        doNotDownload2.title = "no words to include"

        Assert.assertTrue(filter.hasExcludeFilter())
        Assert.assertTrue(filter.hasIncludeFilter())
        Assert.assertTrue(filter.shouldAutoDownload(download))
        Assert.assertTrue(!filter.shouldAutoDownload(doNotDownload))
        Assert.assertTrue(!filter.shouldAutoDownload(doNotDownload2))
    }

}
