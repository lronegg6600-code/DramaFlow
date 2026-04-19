package com.dramaflow.core.common

import android.util.Log
import com.dramaflow.core.database.DramaFlowPreferenceStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DramaInteractionLogTag = "DramaInteraction"

data class DramaInteractionState(
    val likedDramaIds: Set<String> = emptySet(),
    val favoriteDramaIds: Set<String> = emptySet(),
    val interactionUpdatedAt: Long = 0L,
)

data class DramaInteractionFlags(
    val isLiked: Boolean,
    val isFavorited: Boolean,
)

interface DramaInteractionRepository {
    fun observeInteractionState(): Flow<DramaInteractionState>
    suspend fun isLiked(dramaId: String): Boolean
    suspend fun isFavorited(dramaId: String): Boolean
    suspend fun toggleLike(dramaId: String): DramaInteractionState
    suspend fun toggleFavorite(dramaId: String): DramaInteractionState
    fun backfill(dramaIds: Collection<String>, state: DramaInteractionState): Map<String, DramaInteractionFlags>
}

@Singleton
class DramaInteractionLocalDataSource @Inject constructor(
    private val preferences: DramaFlowPreferenceStore,
) {
    fun observeState(): Flow<DramaInteractionState> {
        return combine(
            preferences.likedDramaIds,
            preferences.favoriteDramaIds,
            preferences.interactionUpdatedAt,
        ) { likedDramaIds, favoriteDramaIds, updatedAt ->
            DramaInteractionState(
                likedDramaIds = likedDramaIds,
                favoriteDramaIds = favoriteDramaIds,
                interactionUpdatedAt = updatedAt,
            )
        }
    }

    suspend fun currentState(): DramaInteractionState {
        val snapshot = preferences.snapshotDramaInteractionState()
        return DramaInteractionState(
            likedDramaIds = snapshot.likedDramaIds,
            favoriteDramaIds = snapshot.favoriteDramaIds,
            interactionUpdatedAt = snapshot.updatedAt,
        )
    }

    suspend fun saveState(state: DramaInteractionState) {
        preferences.setDramaInteractionState(
            likedDramaIds = state.likedDramaIds,
            favoriteDramaIds = state.favoriteDramaIds,
            updatedAt = state.interactionUpdatedAt,
        )
    }
}

@Singleton
class DefaultDramaInteractionRepository @Inject constructor(
    private val localDataSource: DramaInteractionLocalDataSource,
) : DramaInteractionRepository {
    override fun observeInteractionState(): Flow<DramaInteractionState> = localDataSource.observeState()

    override suspend fun isLiked(dramaId: String): Boolean {
        return observeInteractionState()
            .map { dramaId in it.likedDramaIds }
            .first()
    }

    override suspend fun isFavorited(dramaId: String): Boolean {
        return observeInteractionState()
            .map { dramaId in it.favoriteDramaIds }
            .first()
    }

    override suspend fun toggleLike(dramaId: String): DramaInteractionState {
        val current = localDataSource.currentState()
        val nextLikedIds = current.likedDramaIds.toMutableSet().apply {
            if (!add(dramaId)) remove(dramaId)
        }
        val nextState = current.copy(
            likedDramaIds = nextLikedIds,
            interactionUpdatedAt = System.currentTimeMillis(),
        )
        localDataSource.saveState(nextState)
        Log.d(
            DramaInteractionLogTag,
            "toggle_like_persisted drama=$dramaId liked=${dramaId in nextState.likedDramaIds}",
        )
        return nextState
    }

    override suspend fun toggleFavorite(dramaId: String): DramaInteractionState {
        val current = localDataSource.currentState()
        val nextFavoriteIds = current.favoriteDramaIds.toMutableSet().apply {
            if (!add(dramaId)) remove(dramaId)
        }
        val nextState = current.copy(
            favoriteDramaIds = nextFavoriteIds,
            interactionUpdatedAt = System.currentTimeMillis(),
        )
        localDataSource.saveState(nextState)
        Log.d(
            DramaInteractionLogTag,
            "toggle_favorite_persisted drama=$dramaId favorited=${dramaId in nextState.favoriteDramaIds}",
        )
        return nextState
    }

    override fun backfill(
        dramaIds: Collection<String>,
        state: DramaInteractionState,
    ): Map<String, DramaInteractionFlags> {
        return dramaIds.associateWith { dramaId ->
            DramaInteractionFlags(
                isLiked = dramaId in state.likedDramaIds,
                isFavorited = dramaId in state.favoriteDramaIds,
            )
        }
    }
}
