package de.danoeh.antennapod.core.tests.util

import android.test.AndroidTestCase

import de.danoeh.antennapod.core.util.LongIntMap
import junit.framework.Assert

class LongLongMapTest : AndroidTestCase() {

    fun testEmptyMap() {
        val map = LongIntMap()
        Assert.assertEquals(0, map.size())
        Assert.assertEquals("LongLongMap{}", map.toString())
        Assert.assertEquals(0, map.get(42))
        Assert.assertEquals(-1, map.get(42, -1))
        Assert.assertEquals(false, map.delete(42))
        Assert.assertEquals(-1, map.indexOfKey(42))
        Assert.assertEquals(-1, map.indexOfValue(42))
        Assert.assertEquals(1, map.hashCode())
    }

    fun testSingleElement() {
        val map = LongIntMap()
        map.put(17, 42)
        Assert.assertEquals(1, map.size())
        Assert.assertEquals("LongLongMap{17=42}", map.toString())
        Assert.assertEquals(42, map.get(17))
        Assert.assertEquals(42, map.get(17, -1))
        Assert.assertEquals(0, map.indexOfKey(17))
        Assert.assertEquals(0, map.indexOfValue(42))
        Assert.assertEquals(true, map.delete(17))
    }

    fun testAddAndDelete() {
        val map = LongIntMap()
        for (i in 0..99) {
            map.put((i * 17).toLong(), i * 42)
        }
        Assert.assertEquals(100, map.size())
        Assert.assertEquals(0, map.get(0))
        Assert.assertEquals(42, map.get(17))
        Assert.assertEquals(42, map.get(17, -1))
        Assert.assertEquals(1, map.indexOfKey(17))
        Assert.assertEquals(1, map.indexOfValue(42))
        for (i in 0..99) {
            Assert.assertEquals(true, map.delete((i * 17).toLong()))
        }
    }

    fun testOverwrite() {
        val map = LongIntMap()
        map.put(17, 42)
        Assert.assertEquals(1, map.size())
        Assert.assertEquals("LongLongMap{17=42}", map.toString())
        Assert.assertEquals(42, map.get(17))
        map.put(17, 23)
        Assert.assertEquals(1, map.size())
        Assert.assertEquals("LongLongMap{17=23}", map.toString())
        Assert.assertEquals(23, map.get(17))
    }

}
