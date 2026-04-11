package com.dramaflow.core.network

import android.os.Build
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

enum class NetworkEnvironment(
    val authBaseUrl: String,
    val contentBaseUrl: String,
    val feedBaseUrl: String,
    val progressBaseUrl: String,
    val playbackBaseUrl: String,
    val entitlementBaseUrl: String,
    val billingBaseUrl: String,
) {
    Debug(
        authBaseUrl = BuildConfig.AUTH_BASE_URL,
        contentBaseUrl = BuildConfig.CONTENT_BASE_URL,
        feedBaseUrl = BuildConfig.API_BASE_URL,
        progressBaseUrl = BuildConfig.PROGRESS_BASE_URL,
        playbackBaseUrl = BuildConfig.PLAYBACK_BASE_URL,
        entitlementBaseUrl = BuildConfig.ENTITLEMENT_BASE_URL,
        billingBaseUrl = BuildConfig.BILLING_BASE_URL,
    ),
    Release(
        authBaseUrl = BuildConfig.AUTH_BASE_URL,
        contentBaseUrl = BuildConfig.CONTENT_BASE_URL,
        feedBaseUrl = BuildConfig.API_BASE_URL,
        progressBaseUrl = BuildConfig.PROGRESS_BASE_URL,
        playbackBaseUrl = BuildConfig.PLAYBACK_BASE_URL,
        entitlementBaseUrl = BuildConfig.ENTITLEMENT_BASE_URL,
        billingBaseUrl = BuildConfig.BILLING_BASE_URL,
    ),
}

object DramaFlowNetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun interface AccessTokenProvider {
        fun getAccessToken(): String?
    }

    interface TokenRefreshCoordinator {
        suspend fun refreshTokenIfNeeded(): String?
    }

    private fun baseClient(accessTokenProvider: AccessTokenProvider? = null): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val token = accessTokenProvider?.getAccessToken()
                    val request = if (token.isNullOrBlank()) {
                        chain.request()
                    } else {
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    }
                    chain.proceed(request)
                },
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()

    fun createRetrofit(
        baseUrl: String,
        accessTokenProvider: AccessTokenProvider? = null,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(resolvedBaseUrl(baseUrl))
            .client(baseClient(accessTokenProvider))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT

    private fun debugRuntimeUrl(url: String): String =
        if (isEmulator()) {
            url
        } else {
            url.replace("10.0.2.2", "127.0.0.1")
        }

    private fun resolvedBaseUrl(url: String): String =
        if (BuildConfig.NETWORK_ENV == "release") {
            url
        } else {
            debugRuntimeUrl(url)
        }

    fun currentEnvironment(): NetworkEnvironment =
        when (BuildConfig.NETWORK_ENV) {
            "release" -> NetworkEnvironment.Release
            else -> NetworkEnvironment.Debug
        }
}
