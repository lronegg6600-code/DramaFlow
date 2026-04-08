package signer

import (
	"fmt"
	"net/url"
	"strings"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
)

type SignedPlayback struct {
	URL     string
	Headers map[string]string
}

type PlaybackUrlSigner interface {
	Mode() string
	Sign(mediaPath string, ttl time.Duration) (SignedPlayback, error)
}

type DevPassthroughSigner struct {
	cfg config.Config
}

func NewDevPassthroughSigner(cfg config.Config) DevPassthroughSigner {
	return DevPassthroughSigner{cfg: cfg}
}

func (s DevPassthroughSigner) Mode() string {
	return "dev_passthrough"
}

func (s DevPassthroughSigner) Sign(mediaPath string, ttl time.Duration) (SignedPlayback, error) {
	if mediaPath == "" {
		return SignedPlayback{}, apperrors.New(500, "playback.media_path_missing", "The media path is missing for this episode.")
	}
	if strings.HasPrefix(mediaPath, "http://") || strings.HasPrefix(mediaPath, "https://") {
		return SignedPlayback{URL: mediaPath, Headers: map[string]string{}}, nil
	}
	baseURL, err := url.Parse(strings.TrimSuffix(s.cfg.Playback.CDNBaseURL, "/"))
	if err != nil {
		return SignedPlayback{}, apperrors.New(500, "playback.signer_invalid_base_url", "The playback base URL is invalid.")
	}
	baseURL.Path = strings.TrimSuffix(baseURL.Path, "/") + "/" + strings.TrimPrefix(mediaPath, "/")
	return SignedPlayback{URL: baseURL.String(), Headers: map[string]string{}}, nil
}

type CloudFrontSignedUrlSigner struct {
	cfg config.Config
}

func NewCloudFrontSignedUrlSigner(cfg config.Config) CloudFrontSignedUrlSigner {
	return CloudFrontSignedUrlSigner{cfg: cfg}
}

func (s CloudFrontSignedUrlSigner) Mode() string {
	return "cloudfront_signed_url"
}

func (s CloudFrontSignedUrlSigner) Sign(mediaPath string, ttl time.Duration) (SignedPlayback, error) {
	if s.cfg.Playback.CloudFrontKeyPairID == "" || s.cfg.Playback.CloudFrontPrivateKeyPEM == "" || s.cfg.Playback.CDNBaseURL == "" {
		return SignedPlayback{}, apperrors.New(500, "playback.signer_config_missing", "CloudFront signing configuration is incomplete.")
	}
	base := strings.TrimSuffix(s.cfg.Playback.CDNBaseURL, "/")
	if mediaPath == "" {
		return SignedPlayback{}, apperrors.New(500, "playback.media_path_missing", "The media path is missing for this episode.")
	}
	return SignedPlayback{
		URL: fmt.Sprintf("%s/%s?Policy=placeholder&Key-Pair-Id=%s&Signature=placeholder", base, strings.TrimPrefix(mediaPath, "/"), s.cfg.Playback.CloudFrontKeyPairID),
		Headers: map[string]string{},
	}, nil
}
