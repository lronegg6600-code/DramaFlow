package signer

import (
	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/playback-service/internal/domain"
)

type PlaybackAssetLocator interface {
	Locate(record domain.EpisodeRecord) (string, error)
}

type DefaultPlaybackAssetLocator struct {
	cfg config.Config
}

func NewDefaultPlaybackAssetLocator(cfg config.Config) DefaultPlaybackAssetLocator {
	return DefaultPlaybackAssetLocator{cfg: cfg}
}

func (l DefaultPlaybackAssetLocator) Locate(record domain.EpisodeRecord) (string, error) {
	if record.MediaPath == "" {
		return "", apperrors.New(500, "playback.media_path_missing", "The media path is missing for this episode.")
	}
	return record.MediaPath, nil
}
