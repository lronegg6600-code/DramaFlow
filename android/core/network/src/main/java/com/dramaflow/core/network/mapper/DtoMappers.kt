package com.dramaflow.core.network.mapper

import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.DramaTag
import com.dramaflow.core.model.Episode
import com.dramaflow.core.model.PlaybackAccessMode
import com.dramaflow.core.model.PlaybackAnalyticsContext
import com.dramaflow.core.model.PlaybackCompletion
import com.dramaflow.core.model.PlaybackDescriptor
import com.dramaflow.core.model.PlaybackHeader
import com.dramaflow.core.model.PlaybackHeartbeat
import com.dramaflow.core.model.WatchHistoryItem
import com.dramaflow.core.model.WatchProgress
import com.dramaflow.core.network.dto.ContinueWatchingItemDto
import com.dramaflow.core.network.dto.DramaDto
import com.dramaflow.core.network.dto.EpisodeDto
import com.dramaflow.core.network.dto.EpisodeProgressDto
import com.dramaflow.core.network.dto.FeedItemDto
import com.dramaflow.core.network.dto.CompletionDto
import com.dramaflow.core.network.dto.HeartbeatDto
import com.dramaflow.core.network.dto.PlaybackSessionDto
import com.dramaflow.core.network.dto.RecentHistoryItemDto
import java.time.Instant

fun DramaDto.toDomain(): Drama =
    Drama(
        id = id,
        title = title,
        shortDescription = shortDescription,
        longDescription = longDescription,
        portraitPosterUrl = posterUrl,
        heroImageUrl = coverUrl,
        heatScore = if (isFeatured) "Featured" else "Rising",
        totalEpisodes = 0,
        tags = tags.map { DramaTag(id = it.lowercase(), label = it) },
        cast = emptyList(),
        labels = listOf(region.uppercase(), language.uppercase()),
        heroNote = shortDescription,
        isFeatured = isFeatured,
        isPremiumSeries = false,
    )

fun EpisodeDto.toDomain(sampleStreamUrl: String): Episode =
    Episode(
        id = id,
        dramaId = dramaId,
        episodeNumber = episodeNo,
        title = title,
        synopsis = description,
        durationSeconds = durationSeconds.toLong(),
        streamUrl = sampleStreamUrl,
        artworkUrl = "",
        isPreviewEnabled = previewSeconds > 0,
        requiresPremium = isPremium,
        previewWindowSeconds = previewSeconds.takeIf { it > 0 },
    )

fun FeedItemDto.toDramaCard(): DramaCard =
    DramaCard(
        drama = Drama(
            id = dramaId,
            title = title,
            shortDescription = shortDescription,
            longDescription = shortDescription,
            portraitPosterUrl = posterUrl,
            heroImageUrl = coverUrl,
            heatScore = if (isFeatured) "Featured" else "Trending",
            totalEpisodes = 0,
            tags = tags.map { DramaTag(id = it.lowercase(), label = it) },
            cast = emptyList(),
            labels = emptyList(),
            heroNote = shortDescription,
            isFeatured = isFeatured,
            isPremiumSeries = isPremium,
        ),
        lastProgress = null,
        isUpdated = false,
        isLockedForUser = isPremium,
        statusLabel = if (isPremium) "Premium" else null,
    )

fun ContinueWatchingItemDto.toDramaCard(): DramaCard =
    DramaCard(
        drama = Drama(
            id = dramaId,
            title = title,
            shortDescription = "Continue where you left off",
            longDescription = "Continue where you left off",
            portraitPosterUrl = "",
            heroImageUrl = "",
            heatScore = "Continue",
            totalEpisodes = 0,
            tags = emptyList(),
            cast = emptyList(),
            labels = emptyList(),
            heroNote = updatedAt,
            isFeatured = false,
            isPremiumSeries = false,
        ),
        lastProgress = WatchProgress(
            dramaId = dramaId,
            episodeId = episodeId,
            positionMs = 0,
            durationMs = 0,
            progressPercent = progressPercent / 100f,
            lastUpdatedEpochMs = Instant.parse(updatedAt).toEpochMilli(),
            completed = false,
        ),
        isUpdated = false,
        isLockedForUser = false,
        statusLabel = "Continue",
    )

fun EpisodeProgressDto.toDomain(): WatchProgress =
    WatchProgress(
        dramaId = dramaId,
        episodeId = episodeId,
        positionMs = positionSeconds * 1000L,
        durationMs = durationSeconds * 1000L,
        progressPercent = if (durationSeconds == 0) 0f else positionSeconds.toFloat() / durationSeconds.toFloat(),
        lastUpdatedEpochMs = Instant.parse(updatedAt).toEpochMilli(),
        completed = completed,
    )

fun RecentHistoryItemDto.toDomain(title: String, artworkUrl: String): WatchHistoryItem =
    WatchHistoryItem(
        dramaId = dramaId,
        episodeId = episodeId,
        dramaTitle = title,
        episodeTitle = "Episode",
        episodeNumber = 0,
        artworkUrl = artworkUrl,
        watchedAtEpochMs = Instant.parse(watchedAt).toEpochMilli(),
        progressPercent = 0f,
        positionMs = 0L,
        durationMs = 0L,
    )

fun PlaybackSessionDto.toDomain(): PlaybackDescriptor =
    PlaybackDescriptor(
        sessionId = sessionId,
        playbackMode = if (playbackMode.equals("preview", ignoreCase = true)) PlaybackAccessMode.PREVIEW else PlaybackAccessMode.FULL,
        mediaUrl = mediaUrl,
        requestHeaders = requestHeaders.map { PlaybackHeader(it.key, it.value) },
        expiresAtEpochMs = Instant.parse(expiresAt).toEpochMilli(),
        previewSeconds = previewSeconds,
        heartbeatIntervalSeconds = heartbeatIntervalSeconds,
        refreshAfterSeconds = refreshAfterSeconds,
        nextEpisodeHint = nextEpisodeHint?.let { com.dramaflow.core.model.NextEpisodeHint(it.episodeId, it.title, 0) },
        analyticsContext = PlaybackAnalyticsContext(
            sessionId = analyticsContext.sessionId,
            dramaId = analyticsContext.dramaId,
            episodeId = analyticsContext.episodeId,
            playbackMode = analyticsContext.playbackMode,
            sourcePage = analyticsContext.sourcePage,
            signerMode = analyticsContext.signerMode,
        ),
    )

fun HeartbeatDto.toDomain(): PlaybackHeartbeat =
    PlaybackHeartbeat(
        keepAlive = keepAlive,
        expiresAtEpochMs = Instant.parse(expiresAt).toEpochMilli(),
        shouldRefreshUrl = shouldRefreshUrl,
        previewRemainingSeconds = previewRemainingSeconds,
        nextAction = nextAction,
    )

fun CompletionDto.toDomain(): PlaybackCompletion =
    PlaybackCompletion(
        accepted = accepted,
        nextEpisodeId = nextEpisodeId,
    )
