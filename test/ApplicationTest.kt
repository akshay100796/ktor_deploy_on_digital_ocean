package com.codexdroid

import io.ktor.http.*
//import kotlin.test.*
import io.ktor.server.testing.*
import junit.framework.Assert.assertEquals
import org.junit.Test

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
