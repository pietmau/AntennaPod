package de.test.antennapod.util

import java.io.File
import java.io.IOException

import de.danoeh.antennapod.core.util.FileNameGenerator
import android.test.AndroidTestCase
import junit.framework.Assert

class FilenameGeneratorTest : AndroidTestCase() {

    @Throws(IOException::class)
    fun testGenerateFileName() {
        val result = FileNameGenerator.generateFileName(VALID1)
        Assert.assertEquals(result, VALID1)
        createFiles(result)
    }

    @Throws(IOException::class)
    fun testGenerateFileName1() {
        val result = FileNameGenerator.generateFileName(INVALID1)
        Assert.assertEquals(result, VALID1)
        createFiles(result)
    }

    @Throws(IOException::class)
    fun testGenerateFileName2() {
        val result = FileNameGenerator.generateFileName(INVALID2)
        Assert.assertEquals(result, VALID1)
        createFiles(result)
    }

    /**
     * Tests if files can be created.

     * @throws IOException
     */
    @Throws(IOException::class)
    private fun createFiles(name: String) {
        val cache = context.externalCacheDir
        val testFile = File(cache, name)
        testFile.mkdir()
        Assert.assertTrue(testFile.exists())
        testFile.delete()
        Assert.assertTrue(testFile.createNewFile())

    }

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        val f = File(context.externalCacheDir, VALID1)
        f.delete()
    }

    companion object {

        private val VALID1 = "abc abc"
        private val INVALID1 = "ab/c: <abc"
        private val INVALID2 = "abc abc "
    }

}
