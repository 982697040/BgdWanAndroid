package com.bgd.myapplication.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun unknownOrMissingPreferenceFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("removed-option"))
    }

    @Test fun allModesRoundTripThroughStableStorageKeys() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorage(it.storageKey)) }
    }
}
