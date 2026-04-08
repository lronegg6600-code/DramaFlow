package com.dramaflow.core.player

import com.dramaflow.core.model.PlaybackDescriptor

data class MediaPlaybackSource(
    val mediaUrl: String,
    val requestHeaders: Map<String, String> = emptyMap(),
)

fun PlaybackDescriptor.toMediaPlaybackSource(): MediaPlaybackSource =
    MediaPlaybackSource(
        mediaUrl = mediaUrl,
        requestHeaders = requestHeaders.associate { it.key to it.value },
    )
