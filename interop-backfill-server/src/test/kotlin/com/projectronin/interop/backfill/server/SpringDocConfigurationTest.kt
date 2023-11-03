package com.projectronin.interop.backfill.server

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SpringDocConfigurationTest {
    @Test
    fun `works wow`() {
        assertNotNull(SpringDocConfiguration().apiInfo())
    }
}
