package com.dramaflow.core.database

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val episodeId: String,
    val dramaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Float,
    val lastUpdatedEpochMs: Long,
    val completed: Boolean,
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val episodeId: String,
    val dramaId: String,
    val title: String,
    val artworkUrl: String,
    val watchedAtEpochMs: Long,
    val progressPercent: Float,
)

@Dao
interface WatchStateDao {
    @Query("SELECT * FROM watch_progress WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getProgressByEpisode(episodeId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE dramaId = :dramaId ORDER BY lastUpdatedEpochMs DESC LIMIT 1")
    suspend fun getLatestProgressForDrama(dramaId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress ORDER BY lastUpdatedEpochMs DESC")
    fun observeAllProgress(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: WatchProgressEntity)

    @Query("SELECT * FROM watch_history ORDER BY watchedAtEpochMs DESC")
    fun observeHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE dramaId = :dramaId")
    suspend fun deleteHistoryByDramaId(dramaId: String)

    @Query("DELETE FROM watch_progress WHERE dramaId = :dramaId")
    suspend fun deleteProgressByDramaId(dramaId: String)

    @Query("DELETE FROM watch_progress")
    suspend fun clearProgress()

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()

    @Transaction
    suspend fun deleteDramaWatchState(dramaId: String) {
        deleteHistoryByDramaId(dramaId)
        deleteProgressByDramaId(dramaId)
    }

    @Transaction
    suspend fun clearAllWatchState() {
        clearProgress()
        clearHistory()
    }
}

@Database(
    entities = [WatchProgressEntity::class, WatchHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DramaFlowDatabase : RoomDatabase() {
    abstract fun watchStateDao(): WatchStateDao
}

class DramaFlowPreferenceStore(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { File(context.filesDir, "dramaflow_preferences.preferences_pb") },
    )

    private val premiumStateKey = stringPreferencesKey("mock_premium_state")
    private val premiumProductKey = stringPreferencesKey("mock_premium_product")
    private val premiumSourceKey = stringPreferencesKey("mock_premium_source")
    private val premiumUpdatedAtKey = longPreferencesKey("mock_premium_updated_at")
    private val premiumExpiresAtKey = longPreferencesKey("mock_premium_expires_at")
    private val lastSelectedProductKey = stringPreferencesKey("last_selected_product")
    private val lastSelectedOfferKey = stringPreferencesKey("last_selected_offer")
    private val lastPlayedEpisodeKey = stringPreferencesKey("last_played_episode")
    private val accessTokenKey = stringPreferencesKey("auth_access_token")
    private val refreshTokenKey = stringPreferencesKey("auth_refresh_token")
    private val accessTokenExpiryKey = stringPreferencesKey("auth_access_token_expiry")
    private val authUserIdKey = stringPreferencesKey("auth_user_id")
    private val likedDramaIdsKey = stringSetPreferencesKey("liked_drama_ids")
    private val favoriteDramaIdsKey = stringSetPreferencesKey("favorite_drama_ids")
    private val interactionUpdatedAtKey = longPreferencesKey("interaction_updated_at")

    val premiumState: Flow<String> = dataStore.data.map { it[premiumStateKey] ?: "free" }
    val premiumProductId: Flow<String?> = dataStore.data.map { it[premiumProductKey] }
    val premiumSourceLabel: Flow<String> = dataStore.data.map { it[premiumSourceKey] ?: "free_tier" }
    val premiumUpdatedAtEpochMs: Flow<Long> = dataStore.data.map { it[premiumUpdatedAtKey] ?: 0L }
    val premiumExpiresAtEpochMs: Flow<Long?> = dataStore.data.map { it[premiumExpiresAtKey] }
    val lastSelectedProductId: Flow<String?> = dataStore.data.map { it[lastSelectedProductKey] }
    val lastSelectedOfferId: Flow<String?> = dataStore.data.map { it[lastSelectedOfferKey] }
    val lastPlayedEpisodeId: Flow<String?> = dataStore.data.map { it[lastPlayedEpisodeKey] }
    val accessToken: Flow<String?> = dataStore.data.map { it[accessTokenKey] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[refreshTokenKey] }
    val accessTokenExpiry: Flow<String?> = dataStore.data.map { it[accessTokenExpiryKey] }
    val authUserId: Flow<String?> = dataStore.data.map { it[authUserIdKey] }
    val likedDramaIds: Flow<Set<String>> = dataStore.data.map { it[likedDramaIdsKey] ?: emptySet() }
    val favoriteDramaIds: Flow<Set<String>> = dataStore.data.map { it[favoriteDramaIdsKey] ?: emptySet() }
    val interactionUpdatedAt: Flow<Long> = dataStore.data.map { it[interactionUpdatedAtKey] ?: 0L }

    suspend fun setPremiumState(
        premiumState: String,
        activeProductId: String?,
        sourceLabel: String = if (premiumState == "premium") "mock_purchase" else "free_tier",
        updatedAtEpochMs: Long = System.currentTimeMillis(),
        expiresAtEpochMs: Long? = null,
    ) {
        dataStore.edit { prefs ->
            prefs[premiumStateKey] = premiumState
            if (activeProductId == null) {
                prefs.remove(premiumProductKey)
            } else {
                prefs[premiumProductKey] = activeProductId
            }
            prefs[premiumSourceKey] = sourceLabel
            prefs[premiumUpdatedAtKey] = updatedAtEpochMs
            if (expiresAtEpochMs == null) {
                prefs.remove(premiumExpiresAtKey)
            } else {
                prefs[premiumExpiresAtKey] = expiresAtEpochMs
            }
        }
    }

    suspend fun setLastSelectedProduct(productId: String?, offerId: String?) {
        dataStore.edit { prefs ->
            if (productId == null) prefs.remove(lastSelectedProductKey) else prefs[lastSelectedProductKey] = productId
            if (offerId == null) prefs.remove(lastSelectedOfferKey) else prefs[lastSelectedOfferKey] = offerId
        }
    }

    suspend fun setLastPlayedEpisode(episodeId: String?) {
        dataStore.edit { prefs ->
            if (episodeId == null) prefs.remove(lastPlayedEpisodeKey) else prefs[lastPlayedEpisodeKey] = episodeId
        }
    }

    suspend fun setAuthSession(
        accessToken: String?,
        refreshToken: String?,
        expiresAt: String?,
        userId: String?,
    ) {
        dataStore.edit { prefs ->
            if (accessToken == null) prefs.remove(accessTokenKey) else prefs[accessTokenKey] = accessToken
            if (refreshToken == null) prefs.remove(refreshTokenKey) else prefs[refreshTokenKey] = refreshToken
            if (expiresAt == null) prefs.remove(accessTokenExpiryKey) else prefs[accessTokenExpiryKey] = expiresAt
            if (userId == null) prefs.remove(authUserIdKey) else prefs[authUserIdKey] = userId
        }
    }

    suspend fun clearAuthSession() {
        setAuthSession(accessToken = null, refreshToken = null, expiresAt = null, userId = null)
    }

    suspend fun resetPremiumState() {
        setPremiumState(
            premiumState = "free",
            activeProductId = null,
            sourceLabel = "free_tier",
            updatedAtEpochMs = System.currentTimeMillis(),
            expiresAtEpochMs = null,
        )
    }

    suspend fun setDramaInteractionState(
        likedDramaIds: Set<String>,
        favoriteDramaIds: Set<String>,
        updatedAt: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { prefs ->
            prefs[likedDramaIdsKey] = likedDramaIds
            prefs[favoriteDramaIdsKey] = favoriteDramaIds
            prefs[interactionUpdatedAtKey] = updatedAt
        }
    }

    suspend fun snapshotDramaInteractionState(): DramaInteractionPreferenceSnapshot {
        val prefs = dataStore.data.first()
        return DramaInteractionPreferenceSnapshot(
            likedDramaIds = prefs[likedDramaIdsKey] ?: emptySet(),
            favoriteDramaIds = prefs[favoriteDramaIdsKey] ?: emptySet(),
            updatedAt = prefs[interactionUpdatedAtKey] ?: 0L,
        )
    }

    suspend fun snapshotEntitlementState(): EntitlementPreferenceSnapshot {
        val prefs = dataStore.data.first()
        return EntitlementPreferenceSnapshot(
            premiumState = prefs[premiumStateKey] ?: "free",
            activeProductId = prefs[premiumProductKey],
            sourceLabel = prefs[premiumSourceKey] ?: "free_tier",
            updatedAtEpochMs = prefs[premiumUpdatedAtKey] ?: 0L,
            expiresAtEpochMs = prefs[premiumExpiresAtKey],
        )
    }

    suspend fun snapshotSelectedProduct(): Pair<String?, String?> {
        val prefs = dataStore.data.first()
        return prefs[lastSelectedProductKey] to prefs[lastSelectedOfferKey]
    }

    suspend fun snapshotAuthSession(): AuthPreferenceSnapshot {
        val prefs = dataStore.data.first()
        return AuthPreferenceSnapshot(
            accessToken = prefs[accessTokenKey],
            refreshToken = prefs[refreshTokenKey],
            expiresAt = prefs[accessTokenExpiryKey],
            userId = prefs[authUserIdKey],
        )
    }
}

data class AuthPreferenceSnapshot(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: String?,
    val userId: String?,
)

data class DramaInteractionPreferenceSnapshot(
    val likedDramaIds: Set<String>,
    val favoriteDramaIds: Set<String>,
    val updatedAt: Long,
)

data class EntitlementPreferenceSnapshot(
    val premiumState: String,
    val activeProductId: String?,
    val sourceLabel: String,
    val updatedAtEpochMs: Long,
    val expiresAtEpochMs: Long?,
)

fun createDatabase(context: Context): DramaFlowDatabase {
    return Room.databaseBuilder(
        context = context,
        klass = DramaFlowDatabase::class.java,
        name = "dramaflow.db",
    ).build()
}
