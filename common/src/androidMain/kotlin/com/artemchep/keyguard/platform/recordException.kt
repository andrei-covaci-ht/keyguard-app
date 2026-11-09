package com.artemchep.keyguard.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Privacy-first Android build: no crash or usage data leaves the device. */
actual fun recordException(e: Throwable) = Unit

actual fun recordLog(message: String) = Unit

actual fun crashlyticsIsEnabled(): Boolean? = false

actual fun crashlyticsIsEnabledFlow(): Flow<Boolean?> = flowOf(false)

actual fun crashlyticsSetEnabled(enabled: Boolean?) = Unit
