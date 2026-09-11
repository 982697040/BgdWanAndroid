package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** Responses from requests started before logout/account switching cannot restore old cookies. */
@Singleton
class SessionInterceptor @Inject constructor(private val storage: SessionStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = try {
        interceptSession(chain)
    } catch (error: IOException) { throw error }
    catch (error: Exception) { throw IOException("Session access failed", error) }

    private fun interceptSession(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != "wanandroid.com" || !request.url.isHttps) return chain.proceed(request)
        storage.initialize()
        val snapshot = storage.snapshots.value
        val cookies = validCookies(snapshot).filter { it.matches(request.url) }
        val authenticated = request.newBuilder().apply {
            if (cookies.isNotEmpty()) header("Cookie", cookies.joinToString("; ") { "${it.name}=${it.value}" })
        }.build()
        val response = chain.proceed(authenticated)
        if (response.request.url.host != "wanandroid.com") return response
        val received = Cookie.parseAll(response.request.url, response.headers)
        if (received.isNotEmpty() && storage.snapshots.value.generation == snapshot.generation) {
            try {
                storage.update(snapshot.generation) { current ->
                    val merged = validCookies(current).toMutableList()
                    received.forEach { cookie ->
                        merged.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
                        if (cookie.expiresAt > System.currentTimeMillis()) merged.add(cookie)
                    }
                    current.copy(cookies = merged.map(Cookie::toString))
                }
            } catch (error: Exception) {
                response.close()
                throw error
            }
        }
        return response
    }
}

internal fun validCookies(snapshot: SessionSnapshot): List<Cookie> = snapshot.cookies
    .mapNotNull { Cookie.parse("https://wanandroid.com/".toHttpUrl(), it) }
    .filter { it.expiresAt > System.currentTimeMillis() }

fun SessionStorage.hasSessionCookies(): Boolean = validCookies(snapshots.value).isNotEmpty()
