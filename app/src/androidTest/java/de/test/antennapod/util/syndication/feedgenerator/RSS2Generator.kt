package de.test.antennapod.util.syndication.feedgenerator

import android.util.Xml
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.feed.FeedItem
import de.danoeh.antennapod.core.util.DateUtils
import org.xmlpull.v1.XmlSerializer

import java.io.IOException
import java.io.OutputStream

/**
 * Creates RSS 2.0 feeds. See FeedGenerator for more information.
 */
class RSS2Generator : FeedGenerator {

    @Throws(IOException::class)
    override fun writeFeed(feed: Feed?, outputStream: OutputStream?, encoding: String?, flags: Long) {
        if (feed == null) throw IllegalArgumentException("feed = null")
        if (outputStream == null) throw IllegalArgumentException("outputStream = null")
        if (encoding == null) throw IllegalArgumentException("encoding = null")

        val xml = Xml.newSerializer()
        xml.setOutput(outputStream, encoding)
        xml.startDocument(encoding, null)

        xml.setPrefix("atom", "http://www.w3.org/2005/Atom")
        xml.startTag(null, "rss")
        xml.attribute(null, "version", "2.0")
        xml.startTag(null, "channel")

        // Write Feed data
        if (feed.title != null) {
            xml.startTag(null, "title")
            xml.text(feed.title)
            xml.endTag(null, "title")
        }
        if (feed.description != null) {
            xml.startTag(null, "description")
            xml.text(feed.description)
            xml.endTag(null, "description")
        }
        if (feed.link != null) {
            xml.startTag(null, "link")
            xml.text(feed.link)
            xml.endTag(null, "link")
        }
        if (feed.language != null) {
            xml.startTag(null, "language")
            xml.text(feed.language)
            xml.endTag(null, "language")
        }

        if (feed.paymentLink != null) {
            GeneratorUtil.addPaymentLink(xml, feed.paymentLink, true)
        }

        // Write FeedItem data
        if (feed.items != null) {
            for (item in feed.items) {
                xml.startTag(null, "item")

                if (item.title != null) {
                    xml.startTag(null, "title")
                    xml.text(item.title)
                    xml.endTag(null, "title")
                }
                if (item.description != null) {
                    xml.startTag(null, "description")
                    xml.text(item.description)
                    xml.endTag(null, "description")
                }
                if (item.link != null) {
                    xml.startTag(null, "link")
                    xml.text(item.link)
                    xml.endTag(null, "link")
                }
                if (item.pubDate != null) {
                    xml.startTag(null, "pubDate")
                    xml.text(DateUtils.formatRFC822Date(item.pubDate))
                    xml.endTag(null, "pubDate")
                }
                if (flags and FEATURE_WRITE_GUID != 0) {
                    xml.startTag(null, "guid")
                    xml.text(item.itemIdentifier)
                    xml.endTag(null, "guid")
                }
                if (item.media != null) {
                    xml.startTag(null, "enclosure")
                    xml.attribute(null, "url", item.media!!.download_url)
                    xml.attribute(null, "length", item.media!!.size.toString())
                    xml.attribute(null, "type", item.media!!.mime_type)
                    xml.endTag(null, "enclosure")
                }
                if (item.paymentLink != null) {
                    GeneratorUtil.addPaymentLink(xml, item.paymentLink, true)
                }

                xml.endTag(null, "item")
            }
        }

        xml.endTag(null, "channel")
        xml.endTag(null, "rss")

        xml.endDocument()
    }

    companion object {

        val FEATURE_WRITE_GUID: Long = 1
    }
}
