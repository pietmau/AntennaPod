package de.danoeh.antennapod.core.feed

object FeedImageMother {

    fun anyFeedImage(): FeedImage {
        return FeedImage(0, "image", null, "http://example.com/picture", false)
    }

}
