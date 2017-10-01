package de.test.antennapod.util.syndication.feedgenerator

import android.util.Xml

import org.xmlpull.v1.XmlSerializer

import java.io.IOException
import java.io.OutputStream

import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.feed.FeedMedia
import de.danoeh.antennapod.core.util.DateUtils

/**
 * Creates Atom feeds. See FeedGenerator for more information.
 */
class AtomGenerator : FeedGenerator {

    @Throws(IOException::class)
    override fun writeFeed(feed: Feed?, outputStream: OutputStream?, encoding: String?, flags: Long) {
        if (feed == null) throw IllegalArgumentException("feed = null")
        if (outputStream == null) throw IllegalArgumentException("outputStream = null")
        if (encoding == null) throw IllegalArgumentException("encoding = null")

        val xml = Xml.newSerializer()
        xml.setOutput(outputStream, encoding)
        xml.startDocument(encoding, null)

        xml.startTag(null, "feed")
        xml.attribute(null, "xmlns", NS_ATOM)

        // Write Feed data
        if (feed.identifyingValue != null) {
            xml.startTag(null, "id")
            xml.text(feed.identifyingValue)
            xml.endTag(null, "id")
        }
        if (feed.title != null) {
            xml.startTag(null, "title")
            xml.text(feed.title)
            xml.endTag(null, "title")
        }
        if (feed.link != null) {
            xml.startTag(null, "link")
            xml.attribute(null, "rel", "alternate")
            xml.attribute(null, "href", feed.link)
            xml.endTag(null, "link")
        }
        if (feed.description != null) {
            xml.startTag(null, "subtitle")
            xml.text(feed.description)
            xml.endTag(null, "subtitle")
        }

        if (feed.paymentLink != null) {
            GeneratorUtil.addPaymentLink(xml, feed.paymentLink, false)
        }

        // Write FeedItem data
        if (feed.items != null) {
            for (item in feed.items) {
                xml.startTag(null, "entry")

                if (item.identifyingValue != null) {
                    xml.startTag(null, "id")
                    xml.text(item.identifyingValue)
                    xml.endTag(null, "id")
                }
                if (item.title != null) {
                    xml.startTag(null, "title")
                    xml.text(item.title)
                    xml.endTag(null, "title")
                }
                if (item.link != null) {
                    xml.startTag(null, "link")
                    xml.attribute(null, "rel", "alternate")
                    xml.attribute(null, "href", item.link)
                    xml.endTag(null, "link")
                }
                if (item.pubDate != null) {
                    xml.startTag(null, "published")
                    if (flags and FEATURE_USE_RFC3339LOCAL != 0) {
                        xml.text(DateUtils.formatRFC3339Local(item.pubDate))
                    } else {
                        xml.text(DateUtils.formatRFC3339UTC(item.pubDate))
                    }
                    xml.endTag(null, "published")
                }
                if (item.description != null) {
                    xml.startTag(null, "content")
                    xml.text(item.description)
                    xml.endTag(null, "content")
                }
                if (item.media != null) {
                    val media = item.media
                    xml.startTag(null, "link")
                    xml.attribute(null, "rel", "enclosure")
                    xml.attribute(null, "href", media!!.download_url)
                    xml.attribute(null, "type", media.mime_type)
                    xml.attribute(null, "length", media.size.toString())
                    xml.endTag(null, "link")
                }

                if (item.paymentLink != null) {
                    GeneratorUtil.addPaymentLink(xml, item.paymentLink, false)
                }

                xml.endTag(null, "entry")
            }
        }

        xml.endTag(null, "feed")
        xml.endDocument()
    }

    companion object {

        private val NS_ATOM = "http://www.w3.org/2005/Atom"

        val FEATURE_USE_RFC3339LOCAL: Long = 1
    }
}
