package com.dramaflow.core.common.auth

import android.content.Context
import android.provider.Settings
import com.dramaflow.core.database.DramaFlowPreferenceStore
import com.dramaflow.core.network.DramaFlowNetworkModule
import com.dramaflow.core.network.source.AuthRemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class AuthSessionSnapshot(
    val userId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAtEpochMs: Long?,
) {
    val isValid: Boolean
        get() = !accessToken.isNullOrBlank() && (expiresAtEpochMs ?: 0L) > System.currentTimeMillis()
}

interface TokenStore : DramaFlowNetworkModule.AccessTokenProvider {
    fun currentSession(): AuthSessionSnapshot
    suspend fun persistSession(
        userId: String?,
        accessToken: String?,
        refreshToken: String?,
        expiresAtIso: String?,
    )
    suspend fun clear()
}

@Singleton
class PreferenceTokenStore @Inject constructor(
    private val preferences: DramaFlowPreferenceStore,
) : TokenStore {
    override fun getAccessToken(): String? = currentSession().accessToken

    override fun currentSession(): AuthSessionSnapshot = runBlocking {
        val snapshot = preferences.snapshotAuthSession()
        AuthSessionSnapshot(
            userId = snapshot.userId,
            accessToken = snapshot.accessToken,
            refreshToken = snapshot.refreshToken,
            expiresAtEpochMs = snapshot.expiresAt?.let { Instant.parse(it).toEpochMilli() },
        )
    }

    override suspend fun persistSession(
        userId: String?,
        accessToken: String?,
        refreshToken: String?,
        expiresAtIso: String?,
    ) {
        preferences.setAuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAtIso,
            userId = userId,
        )
    }

    override suspend fun clear() {
        preferences.clearAuthSession()
    }
}

interface AuthSessionManager {
    val sessionFlow: Flow<AuthSessionSnapshot>
    suspend fun ensureGuestSession(): AuthSessionSnapshot
    suspend fun refreshIfNeeded(): String?
}

@Singleton
class DefaultAuthSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val preferences: DramaFlowPreferenceStore,
) : AuthSessionManager {
    override val sessionFlow: Flow<AuthSessionSnapshot> =
        preferences.accessToken.map {
            tokenStore.currentSession()
        }

    override suspend fun ensureGuestSession(): AuthSessionSnapshot {
        val current = tokenStore.currentSession()
        if (current.isValid) return current

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "dramaflow-device"
        val session = authRemoteDataSource.createGuestSessionPayload(deviceId)
        tokenStore.persistSession(
            userId = session.userId,
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAtIso = session.expiresAt,
        )
        return tokenStore.currentSession()
    }

    override suspend fun refreshIfNeeded(): String? {
        val current = tokenStore.currentSession()
        if (current.isValid) return current.accessToken
        val refreshToken = current.refreshToken ?: return ensureGuestSession().accessToken
        val refreshed = authRemoteDataSource.refreshSessionPayload(refreshToken) ?: return ensureGuestSession().accessToken
        tokenStore.persistSession(
            userId = refreshed.userId,
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            expiresAtIso = refreshed.expiresAt,
        )
        return refreshed.accessToken
    }
}
