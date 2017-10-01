package de.test.antennapod

import android.test.InstrumentationTestRunner
import android.test.suitebuilder.TestSuiteBuilder

import junit.framework.TestSuite

class AntennaPodTestRunner : InstrumentationTestRunner() {

    override fun getAllTests(): TestSuite {
        return TestSuiteBuilder(AntennaPodTestRunner::class.java)
                .includeAllPackagesUnderHere()
                .excludePackages("de.test.antennapod.gpodnet")
                .build()
    }

}
