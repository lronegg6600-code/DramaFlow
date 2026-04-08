package service

import (
	"context"
	"testing"
	"time"

	"dramaflow/backend/shared/config"
	"dramaflow/backend/shared/logger"
	"dramaflow/backend/services/playback-service/internal/domain"
	"dramaflow/backend/services/playback-service/internal/signer"
)

type playbackRepoMock struct {
	session domain.SessionCacheRecord
}

func (m playbackRepoMock) GetEpisodeRecord(ctx context.Context, episodeID string) (domain.EpisodeRecord, error) {
	return domain.EpisodeRecord{
		DramaID:        "drama-1",
		EpisodeID:      episodeID,
		Title:          "第一集",
		PreviewSeconds: 30,
		IsPremium:      true,
		PublishStatus:  "published",
		MediaPath:      "video/episode-1.m3u8",
	}, nil
}
func (m playbackRepoMock) InsertSession(ctx context.Context, userID string, record domain.EpisodeRecord, mode domain.PlaybackMode, request domain.CreatePlaybackSessionRequest, expiresAt time.Time) (string, error) {
	return "session-1", nil
}
func (m playbackRepoMock) CacheSession(ctx context.Context, session domain.SessionCacheRecord, ttl time.Duration) error { return nil }
func (m playbackRepoMock) GetCachedSession(ctx context.Context, sessionID string) (domain.SessionCacheRecord, error) {
	return m.session, nil
}
func (m playbackRepoMock) GetSessionDetail(ctx context.Context, sessionID string) (domain.SessionDetail, error) {
	return domain.SessionDetail{SessionID: sessionID}, nil
}
func (m playbackRepoMock) MarkSessionCompleted(ctx context.Context, sessionID string) error { return nil }
func (m playbackRepoMock) DeleteCachedSession(ctx context.Context, sessionID string) error   { return nil }
func (m playbackRepoMock) UpdateSessionExpiry(ctx context.Context, sessionID string, expiresAt time.Time) error {
	return nil
}
func (m playbackRepoMock) GrantDevEntitlement(ctx context.Context, request domain.DevEntitlementGrantRequest) error {
	return nil
}
func (m playbackRepoMock) RevokeDevEntitlement(ctx context.Context, userID string) error { return nil }

type entitlementGatewayMock struct {
	access domain.EntitlementAccess
}

func (m entitlementGatewayMock) ResolveAccess(ctx context.Context, userID string, record domain.EpisodeRecord) (domain.EntitlementAccess, error) {
	return m.access, nil
}

type locatorMock struct{}

func (locatorMock) Locate(record domain.EpisodeRecord) (string, error) { return record.MediaPath, nil }

type signerMock struct{}

func (signerMock) Mode() string { return "dev_passthrough" }
func (signerMock) Sign(mediaPath string, ttl time.Duration) (signer.SignedPlayback, error) {
	return signer.SignedPlayback{URL: "https://cdn.test/" + mediaPath, Headers: map[string]string{}}, nil
}

func testPlaybackConfig() config.Config {
	return config.Config{
		ServiceName: "playback-service",
		Environment: "test",
		Playback: config.PlaybackConfig{
			URLTTL:            5 * time.Minute,
			HeartbeatInterval: 15 * time.Second,
			RefreshBefore:     45 * time.Second,
		},
	}
}

func TestCreateSessionReturnsPreviewDescriptor(t *testing.T) {
	svc := New(
		playbackRepoMock{},
		entitlementGatewayMock{access: domain.EntitlementPreviewOnly},
		locatorMock{},
		signerMock{},
		testPlaybackConfig(),
		logger.New("playback-service", "test"),
	)

	resp, err := svc.CreateSession(context.Background(), "user-1", domain.CreatePlaybackSessionRequest{
		EpisodeID:  "episode-1",
		SourcePage: "feed_home",
		DeviceContext: domain.DeviceContext{
			Platform:   "android",
			AppVersion: "1.0.0",
		},
	}, "req-1", "trace-1")
	if err != nil {
		t.Fatalf("CreateSession() error = %v", err)
	}
	if resp.PlaybackMode != domain.PlaybackModePreview {
		t.Fatalf("expected preview mode, got %#v", resp)
	}
}

func TestHeartbeatReturnsPaywallActionWhenPreviewExhausted(t *testing.T) {
	expiresAt := time.Now().UTC().Add(30 * time.Second).Format(time.RFC3339)
	svc := New(
		playbackRepoMock{session: domain.SessionCacheRecord{
			SessionID:      "session-1",
			UserID:         "user-1",
			EpisodeID:      "episode-1",
			PlaybackMode:   domain.PlaybackModePreview,
			ExpiresAt:      expiresAt,
			PreviewSeconds: 30,
			SignerMode:     "dev_passthrough",
		}},
		entitlementGatewayMock{},
		locatorMock{},
		signerMock{},
		testPlaybackConfig(),
		logger.New("playback-service", "test"),
	)

	resp, err := svc.Heartbeat(context.Background(), "user-1", "session-1", domain.HeartbeatRequest{
		PositionSeconds:         30,
		BufferedPositionSeconds: 30,
		PlayerState:             "playing",
		ClientTime:              "2026-04-08T00:00:00Z",
	}, "req-1", "trace-1")
	if err != nil {
		t.Fatalf("Heartbeat() error = %v", err)
	}
	if resp.NextAction == nil || *resp.NextAction != "show_paywall" {
		t.Fatalf("expected show_paywall action, got %#v", resp)
	}
}
