package com.bgd.myapplication

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.bgd.myapplication.core.network.SessionCookieJar
import java.io.File
import java.util.UUID
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class SessionCookieJarTest {
    @Test fun encryptedSessionRestoresAndOnlySendsMatchingCookies() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(base.cacheDir, "session-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getNoBackupFilesDir() = directory }
        try {
            val url = "https://wanandroid.com/".toHttpUrl()
            val jar = SessionCookieJar(context)
            jar.saveFromResponse(url, listOf(Cookie.Builder().name("test-session").value("secret-test-token")
                .hostOnlyDomain("wanandroid.com").path("/").secure().httpOnly()
                .expiresAt(java.lang.System.currentTimeMillis() + 60_000).build()))
            jar.setUser("test-user", setOf(42))
            assertFalse(File(directory, "wanandroid-session").readBytes().decodeToString().contains("secret-test-token"))
            val restored = SessionCookieJar(context)
            assertEquals("test-user", restored.username())
            assertEquals(setOf(42), restored.collectedIds())
            assertEquals("secret-test-token", restored.loadForRequest(url).single().value)
            assertTrue(restored.loadForRequest("https://example.com/".toHttpUrl()).isEmpty())
            restored.clear()
            assertNull(SessionCookieJar(context).username())
        } finally { directory.deleteRecursively() }
    }
}
