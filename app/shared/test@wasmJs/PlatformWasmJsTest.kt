package com.example.lauzhack26_multiplatform

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformWasmJsTest {
    @Test
    fun `platform name is reported`() {
        assertTrue(getPlatform().name.isNotBlank())
    }
}
