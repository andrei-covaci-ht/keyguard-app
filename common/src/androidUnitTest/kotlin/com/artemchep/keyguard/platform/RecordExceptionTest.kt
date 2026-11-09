package com.artemchep.keyguard.platform

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * The privacy build has no remote crash reporter. These calls remain safe no-ops
 * because they are used from recovery paths that have already handled an error.
 */
class RecordExceptionTest {
    @Test
    fun `recordException does not propagate a reporter failure`() {
        recordException(IllegalStateException("reported failure"))
    }

    @Test
    fun `recordLog does not propagate a reporter failure`() {
        recordLog("breadcrumb")
    }

    @Test
    fun `crashlyticsIsEnabled is always disabled`() {
        assertFalse(crashlyticsIsEnabled() == true)
    }

    @Test
    fun `crashlyticsIsEnabledFlow is always disabled`() = runTest {
        assertFalse(crashlyticsIsEnabledFlow().first() == true)
    }

    @Test
    fun `crashlyticsSetEnabled does not propagate a reporter failure`() {
        crashlyticsSetEnabled(false)
    }
}
