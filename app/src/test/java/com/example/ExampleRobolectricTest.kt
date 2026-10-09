package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and dual engine templates from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val primaryTemplate = context.getString(R.string.primary_url_template_default)
        val secondaryTemplate = context.getString(R.string.secondary_url_template_default)

        assertEquals("Synapse Browser", appName)
        assertEquals("https://gemini.google.com/search?q=%s", primaryTemplate)
        assertEquals("https://copilot.microsoft.com/search?q=%s", secondaryTemplate)
    }
}
