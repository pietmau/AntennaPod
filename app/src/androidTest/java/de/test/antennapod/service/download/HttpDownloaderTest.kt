package de.test.antennapod.service.download

import android.test.InstrumentationTestCase
import android.util.Log

import java.io.File
import java.io.IOException

import de.danoeh.antennapod.core.feed.FeedFile
import de.danoeh.antennapod.core.preferences.UserPreferences
import de.danoeh.antennapod.core.service.download.DownloadRequest
import de.danoeh.antennapod.core.service.download.DownloadStatus
import de.danoeh.antennapod.core.service.download.Downloader
import de.danoeh.antennapod.core.service.download.HttpDownloader
import de.danoeh.antennapod.core.util.DownloadError
import de.test.antennapod.util.service.download.HTTPBin

class HttpDownloaderTest : InstrumentationTestCase() {

    private var destDir: File? = null

    private var httpServer: HTTPBin? = null

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        val contents = destDir!!.listFiles()
        for (f in contents) {
            Assert.assertTrue(f.delete())
        }

        httpServer!!.stop()
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        UserPreferences.init(instrumentation.targetContext)
        destDir = instrumentation.targetContext.getExternalFilesDir(DOWNLOAD_DIR)
        Assert.assertNotNull(destDir)
        Assert.assertTrue(destDir!!.exists())
        httpServer = HTTPBin()
        httpServer!!.start()
    }

    private fun setupFeedFile(downloadUrl: String, title: String, deleteExisting: Boolean): FeedFileImpl {
        val feedfile = FeedFileImpl(downloadUrl)
        val fileUrl = File(destDir, title).absolutePath
        val file = File(fileUrl)
        if (deleteExisting) {
            Log.d(TAG, "Deleting file: " + file.delete())
        }
        feedfile.file_url = fileUrl
        return feedfile
    }

    private fun download(url: String, title: String, expectedResult: Boolean, deleteExisting: Boolean = true, username: String? = null, password: String? = null, deleteOnFail: Boolean = true): Downloader {
        val feedFile = setupFeedFile(url, title, deleteExisting)
        val request = DownloadRequest(feedFile.file_url, url, title, 0, feedFile.typeAsInt, username, password, deleteOnFail, null)
        val downloader = HttpDownloader(request)
        downloader.call()
        val status = downloader.result
        Assert.assertNotNull(status)
        Assert.assertTrue(status.isSuccessful == expectedResult)
        Assert.assertTrue(status.isDone)
        // the file should not exist if the download has failed and deleteExisting was true
        Assert.assertTrue(!deleteExisting || File(feedFile.file_url).exists() == expectedResult)
        return downloader
    }

    fun testPassingHttp() {
        download(HTTPBin.BASE_URL + "/status/200", "test200", true)
    }

    fun testRedirect() {
        download(HTTPBin.BASE_URL + "/redirect/4", "testRedirect", true)
    }

    fun testGzip() {
        download(HTTPBin.BASE_URL + "/gzip/100", "testGzip", true)
    }

    fun test404() {
        download(URL_404, "test404", false)
    }

    fun testCancel() {
        val url = HTTPBin.BASE_URL + "/delay/3"
        val feedFile = setupFeedFile(url, "delay", true)
        val downloader = HttpDownloader(DownloadRequest(feedFile.file_url, url, "delay", 0, feedFile.typeAsInt))
        val t = object : Thread() {
            override fun run() {
                downloader.call()
            }
        }
        t.start()
        downloader.cancel()
        try {
            t.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val result = downloader.result
        Assert.assertTrue(result.isDone)
        Assert.assertFalse(result.isSuccessful)
        Assert.assertTrue(result.isCancelled)
        Assert.assertFalse(File(feedFile.file_url).exists())
    }

    fun testDeleteOnFailShouldDelete() {
        val downloader = download(URL_404, "testDeleteOnFailShouldDelete", false, true, null, null, true)
        Assert.assertFalse(File(downloader.downloadRequest.destination).exists())
    }

    @Throws(IOException::class)
    fun testDeleteOnFailShouldNotDelete() {
        val filename = "testDeleteOnFailShouldDelete"
        val dest = File(destDir, filename)
        dest.delete()
        Assert.assertTrue(dest.createNewFile())
        val downloader = download(URL_404, filename, false, false, null, null, false)
        Assert.assertTrue(File(downloader.downloadRequest.destination).exists())
    }

    @Throws(InterruptedException::class)
    fun testAuthenticationShouldSucceed() {
        download(URL_AUTH, "testAuthSuccess", true, true, "user", "passwd", true)
    }

    fun testAuthenticationShouldFail() {
        val downloader = download(URL_AUTH, "testAuthSuccess", false, true, "user", "Wrong passwd", true)
        Assert.assertEquals(DownloadError.ERROR_UNAUTHORIZED, downloader.result.reason)
    }

    /* TODO: replace with smaller test file
    public void testUrlWithSpaces() {
        download("http://acedl.noxsolutions.com/ace/Don't Call Salman Rushdie Sneezy in Finland.mp3", "testUrlWithSpaces", true);
    }
    */

    private class FeedFileImpl(download_url: String) : FeedFile(null, download_url, false) {


        override fun getHumanReadableIdentifier(): String {
            return download_url
        }

        override fun getTypeAsInt(): Int {
            return 0
        }
    }

    companion object {
        private val TAG = "HttpDownloaderTest"
        private val DOWNLOAD_DIR = "testdownloads"

        private val successful = true


        private val URL_404 = HTTPBin.BASE_URL + "/status/404"
        private val URL_AUTH = HTTPBin.BASE_URL + "/basic-auth/user/passwd"
    }

}
