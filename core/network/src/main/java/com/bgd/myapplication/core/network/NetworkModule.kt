package com.bgd.myapplication.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Create a Retrofit instance only after the app supplies its real HTTPS endpoint. */
@Singleton
class RetrofitFactory @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    fun create(baseUrl: HttpUrl): Retrofit {
        require(baseUrl.isHttps) { "The API endpoint must use HTTPS." }
        require(baseUrl.encodedPath.endsWith("/")) { "The base URL must end with /." }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(sessionInterceptor: SessionInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(sessionInterceptor)
        .followRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
}
