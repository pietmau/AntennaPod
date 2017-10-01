package de.danoeh.antennapod.core.feed

import de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage
import de.danoeh.antennapod.core.feed.FeedMother.anyFeed
import java.util.*

internal object FeedItemMother {

    fun anyFeedItemWithImage(): FeedItem {
        val item = FeedItem(0, "Item", "Item", "url", Date(), FeedItem.PLAYED, anyFeed())
        item.image = anyFeedImage()
        return item
    }

}
