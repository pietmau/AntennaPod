package de.danoeh.antennapod.core.tests

import android.test.InstrumentationTestRunner
import android.test.suitebuilder.TestSuiteBuilder
import junit.framework.TestSuite

class AntennaPodTestRunner : InstrumentationTestRunner() {

    override fun getAllTests(): TestSuite {
        return TestSuiteBuilder(AntennaPodTestRunner::class.java)
                .includeAllPackagesUnderHere()
                .build()
    }
}