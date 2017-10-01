package de.test.antennapod.util.service.download

import android.util.Base64
import android.util.Log

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.util.ArrayList
import java.util.Arrays
import java.util.Random
import java.util.zip.GZIPOutputStream

import de.danoeh.antennapod.BuildConfig

/**
 * Http server for testing purposes
 *
 *
 * Supported features:
 *
 *
 * /status/code: Returns HTTP response with the given status code
 * /redirect/n:  Redirects n times
 * /delay/n:     Delay response for n seconds
 * /basic-auth/username/password: Basic auth with username and password
 * /gzip/n:      Send gzipped data of size n bytes
 * /files/id:     Accesses the file with the specified ID (this has to be added first via serveFile).
 */
class HTTPBin : NanoHTTPD(PORT) {

    private val servedFiles: MutableList<File>

    init {
        this.servedFiles = ArrayList<File>()
    }

    /**
     * Adds the given file to the server.

     * @return The ID of the file or -1 if the file could not be added to the server.
     */
    @Synchronized fun serveFile(file: File?): Int {
        if (file == null) throw IllegalArgumentException("file = null")
        if (!file.exists()) {
            return -1
        }
        for (i in servedFiles.indices) {
            if (servedFiles[i].absolutePath == file.absolutePath) {
                return i
            }
        }
        servedFiles.add(file)
        return servedFiles.size - 1
    }

    /**
     * Removes the file with the given ID from the server.

     * @return True if a file was removed, false otherwise
     */
    @Synchronized fun removeFile(id: Int): Boolean {
        if (id < 0) throw IllegalArgumentException("ID < 0")
        if (id >= servedFiles.size) {
            return false
        } else {
            return servedFiles.removeAt(id) != null
        }
    }

    @Synchronized fun accessFile(id: Int): File? {
        if (id < 0 || id >= servedFiles.size) {
            return null
        } else {
            return servedFiles[id]
        }
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response? {

        if (BuildConfig.DEBUG) Log.d(TAG, "Requested url: " + session.uri)

        val segments = session.uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (segments.size < 3) {
            Log.w(TAG, String.format("Invalid number of URI segments: %d %s", segments.size, Arrays.toString(segments)))
            404E rror
        }

        val func = segments[1]
        val param = segments[2]
        val headers = session.headers

        if (func.equals("status", ignoreCase = true)) {
            try {
                val code = Integer.parseInt(param)
                return getStatus(code)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return internalError
            }

        } else if (func.equals("redirect", ignoreCase = true)) {
            try {
                val times = Integer.parseInt(param)
                if (times < 0) {
                    throw NumberFormatException("times <= 0: " + times)
                }

                return getRedirectResponse(times - 1)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return internalError
            }

        } else if (func.equals("delay", ignoreCase = true)) {
            try {
                val sec = Integer.parseInt(param)
                if (sec <= 0) {
                    throw NumberFormatException("sec <= 0: " + sec)
                }

                Thread.sleep(sec * 1000L)
                return okResponse
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return internalError
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return internalError
            }

        } else if (func.equals("basic-auth", ignoreCase = true)) {
            if (!headers.containsKey("authorization")) {
                Log.w(TAG, "No credentials provided")
                return unauthorizedResponse
            }
            try {
                val credentials = String(Base64.decode(headers["authorization"].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1], 0), "UTF-8")
                val credentialParts = credentials.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (credentialParts.size != 2) {
                    Log.w(TAG, "Unable to split credentials: " + Arrays.toString(credentialParts))
                    return internalError
                }
                if (credentialParts[0] == segments[2] && credentialParts[1] == segments[3]) {
                    Log.i(TAG, "Credentials accepted")
                    return okResponse
                } else {
                    Log.w(TAG, String.format("Invalid credentials. Expected %s, %s, but was %s, %s",
                            segments[2], segments[3], credentialParts[0], credentialParts[1]))
                    return unauthorizedResponse
                }

            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return internalError
            }

        } else if (func.equals("gzip", ignoreCase = true)) {
            try {
                val size = Integer.parseInt(param)
                if (size <= 0) {
                    Log.w(TAG, "Invalid size for gzipped data: " + size)
                    throw NumberFormatException()
                }

                return getGzippedResponse(size)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return internalError
            } catch (e: IOException) {
                e.printStackTrace()
                return internalError
            }

        } else if (func.equals("files", ignoreCase = true)) {
            try {
                val id = Integer.parseInt(param)
                if (id < 0) {
                    Log.w(TAG, "Invalid ID: " + id)
                    throw NumberFormatException()
                }
                return getFileAccessResponse(id, headers)

            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return internalError
            }

        }

        return 404E rror
    }

    @Synchronized private fun getFileAccessResponse(id: Int, header: Map<String, String>): NanoHTTPD.Response {
        val file = accessFile(id)
        if (file == null || !file.exists()) {
            Log.w(TAG, "File not found: " + id)
            return 404E rror
        }
        var inputStream: InputStream? = null
        var contentRange: String? = null
        val status: Response.Status
        var successful = false
        try {
            inputStream = FileInputStream(file)
            if (header.containsKey("range")) {
                // read range header field
                val value = header["range"]
                val segments = value.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (segments.size != 2) {
                    Log.w(TAG, "Invalid segment length: " + Arrays.toString(segments))
                    return internalError
                }
                val type = StringUtils.substringBefore(value, "=")
                if (!type.equals("bytes", ignoreCase = true)) {
                    Log.w(TAG, "Range is not specified in bytes: " + value)
                    return internalError
                }
                try {
                    val start = java.lang.Long.parseLong(StringUtils.substringBefore(segments[1], "-"))
                    if (start >= file.length()) {
                        return rangeNotSatisfiable
                    }

                    // skip 'start' bytes
                    IOUtils.skipFully(inputStream, start)
                    contentRange = "bytes " + start + (file.length() - 1) + "/" + file.length()

                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    return internalError
                } catch (e: IOException) {
                    e.printStackTrace()
                    return internalError
                }

                status = NanoHTTPD.Response.Status.PARTIAL_CONTENT

            } else {
                // request did not contain range header field
                status = NanoHTTPD.Response.Status.OK
            }
            successful = true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()

            return internalError
        } finally {
            if (!successful && inputStream != null) {
                IOUtils.closeQuietly(inputStream)
            }
        }

        val response = NanoHTTPD.Response(status, URLConnection.guessContentTypeFromName(file.absolutePath), inputStream)

        response.addHeader("Accept-Ranges", "bytes")
        if (contentRange != null) {
            response.addHeader("Content-Range", contentRange)
        }
        response.addHeader("Content-Length", file.length().toString())
        return response
    }

    @Throws(IOException::class)
    private fun getGzippedResponse(size: Int): NanoHTTPD.Response {
        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val buffer = ByteArray(size)
        val random = Random(System.currentTimeMillis())
        random.nextBytes(buffer)

        val compressed = ByteArrayOutputStream(buffer.size)
        val gzipOutputStream = GZIPOutputStream(compressed)
        gzipOutputStream.write(buffer)
        gzipOutputStream.close()

        val inputStream = ByteArrayInputStream(compressed.toByteArray())
        val response = NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_PLAIN, inputStream)
        response.addHeader("Content-Encoding", "gzip")
        response.addHeader("Content-Length", compressed.size().toString())
        return response
    }

    private fun getStatus(code: Int): NanoHTTPD.Response {
        val status = if (code == 200)
            NanoHTTPD.Response.Status.OK
        else if (code == 201)
            NanoHTTPD.Response.Status.CREATED
        else if (code == 206)
            NanoHTTPD.Response.Status.PARTIAL_CONTENT
        else if (code == 301)
            NanoHTTPD.Response.Status.REDIRECT
        else if (code == 304)
            NanoHTTPD.Response.Status.NOT_MODIFIED
        else if (code == 400)
            NanoHTTPD.Response.Status.BAD_REQUEST
        else if (code == 401)
            NanoHTTPD.Response.Status.UNAUTHORIZED
        else if (code == 403)
            NanoHTTPD.Response.Status.FORBIDDEN
        else if (code == 404)
            NanoHTTPD.Response.Status.NOT_FOUND
        else if (code == 405)
            NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED
        else if (code == 416)
            NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE
        else if (code == 500)
            NanoHTTPD.Response.Status.INTERNAL_ERROR
        else
            object : Response.IStatus {
                override val requestStatus: Int
                    get() = code

                override val description: String
                    get() = "Unknown"
            }
        return NanoHTTPD.Response(status, MIME_HTML, "")

    }

    private fun getRedirectResponse(times: Int): NanoHTTPD.Response {
        if (times > 0) {
            val response = NanoHTTPD.Response(NanoHTTPD.Response.Status.REDIRECT, MIME_HTML, "This resource has been moved permanently")
            response.addHeader("Location", "/redirect/" + times)
            return response
        } else if (times == 0) {
            return okResponse
        } else {
            return internalError
        }
    }

    private val unauthorizedResponse: NanoHTTPD.Response
        get() {
            val response = NanoHTTPD.Response(NanoHTTPD.Response.Status.UNAUTHORIZED, MIME_HTML, "")
            response.addHeader("WWW-Authenticate", "Basic realm=\"Test Realm\"")
            return response
        }

    private val okResponse: NanoHTTPD.Response
        get() = NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML, "")

    private val internalError: NanoHTTPD.Response
        get() = NanoHTTPD.Response(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_HTML, "The server encountered an internal error")

    private val rangeNotSatisfiable: NanoHTTPD.Response
        get() = NanoHTTPD.Response(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAIN, "")

    private val 404Error:NanoHTTPD.Response
    get()
    {
        return NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_HTML, "The requested URL was not found on this server")
    }

    companion object {
        private val TAG = "HTTPBin"
        val PORT = 8124
        val BASE_URL = "http://127.0.0.1:" + HTTPBin.PORT


        private val MIME_HTML = "text/html"
        private val MIME_PLAIN = "text/plain"
    }
}
