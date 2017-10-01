package de.danoeh.antennapod.core.feed

import de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage

object FeedMother {

    fun anyFeed(): Feed {
        val image = anyFeedImage()
        return Feed(0,
                null, "title", "http://example.com", "This is the description",
                "http://example.com/payment", "Daniel", "en", null, "http://example.com/feed", image, null, "http://example.com/feed", true)
    }

}
