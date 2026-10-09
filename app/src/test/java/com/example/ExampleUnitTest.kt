package com.example

import com.example.data.remote.DualEngineCollector
import com.example.viewmodel.SynapseBrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun dualEngineUrlFormatting_isCorrect() {
        val collector = DualEngineCollector()
        val primaryUrl = collector.formatEngineUrl(
            SynapseBrowserViewModel.DEFAULT_PRIMARY_TEMPLATE,
            "quantum computing 2026"
        )
        val secondaryUrl = collector.formatEngineUrl(
            SynapseBrowserViewModel.DEFAULT_SECONDARY_TEMPLATE,
            "quantum computing 2026"
        )

        assertEquals(
            "https://gemini.google.com/search?q=quantum+computing+2026",
            primaryUrl
        )
        assertEquals(
            "https://copilot.microsoft.com/search?q=quantum+computing+2026",
            secondaryUrl
        )
        assertTrue(primaryUrl.startsWith("https://gemini.google.com/search?q="))
        assertTrue(secondaryUrl.startsWith("https://copilot.microsoft.com/search?q="))
    }
}
