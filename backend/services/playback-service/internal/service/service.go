package service

import (
	"context"
	"fmt"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	sharedlogger "dramaflow/backend/shared/logger"
	"dramaflow/backend/services/playback-service/internal/domain"
	"dramaflow/backend/services/playback-service/internal/gateway"
	"dramaflow/backend/services/playback-service/internal/signer"
	"github.com/rs/zerolog"
)

type Service struct {
	repo        playbackRepository
	entitlement gateway.EntitlementGateway
	locator     signer.PlaybackAssetLocator
	signer      signer.PlaybackUrlSigner
	cfg         config.Config
	baseLogger  zerolog.Logger
}

type playbackRepository interface {
	GetEpisodeRecord(ctx context.Context, episodeID string) (domain.EpisodeRecord, error)
	InsertSession(ctx context.Context, userID string, record domain.EpisodeRecord, mode domain.PlaybackMode, request domain.CreatePlaybackSessionRequest, expiresAt time.Time) (string, error)
	CacheSession(ctx context.Context, session domain.SessionCacheRecord, ttl time.Duration) error
	GetCachedSession(ctx context.Context, sessionID string) (domain.SessionCacheRecord, error)
	GetSessionDetail(ctx context.Context, sessionID string) (domain.SessionDetail, error)
	MarkSessionCompleted(ctx context.Context, sessionID string) error
	DeleteCachedSession(ctx context.Context, sessionID string) error
	UpdateSessionExpiry(ctx context.Context, sessionID string, expiresAt time.Time) error
	GrantDevEntitlement(ctx context.Context, request domain.DevEntitlementGrantRequest) error
	RevokeDevEntitlement(ctx context.Context, userID string) error
}

func New(
	repo playbackRepository,
	entitlement gateway.EntitlementGateway,
	locator signer.PlaybackAssetLocator,
	playbackSigner signer.PlaybackUrlSigner,
	cfg config.Config,
	baseLogger zerolog.Logger,
) Service {
	return Service{
		repo:        repo,
		entitlement: entitlement,
		locator:     locator,
		signer:      playbackSigner,
		cfg:         cfg,
		baseLogger:  baseLogger,
	}
}

func (s Service) CreateSession(ctx context.Context, userID string, request domain.CreatePlaybackSessionRequest, requestID string, traceID string) (domain.PlaybackDescriptor, error) {
	record, err := s.repo.GetEpisodeRecord(ctx, request.EpisodeID)
	if err != nil {
		return domain.PlaybackDescriptor{}, err
	}
	if record.PublishStatus != "published" {
		return domain.PlaybackDescriptor{}, apperrors.New(404, "playback.episode_unpublished", "The episode is not published.")
	}
	access, err := s.entitlement.ResolveAccess(ctx, userID, record)
	if err != nil {
		return domain.PlaybackDescriptor{}, err
	}

	mode := domain.PlaybackModeFull
	switch access {
	case domain.EntitlementFullAccess:
		mode = domain.PlaybackModeFull
	case domain.EntitlementPreviewOnly:
		mode = domain.PlaybackModePreview
	default:
		return domain.PlaybackDescriptor{}, apperrors.New(403, "playback.entitlement_required", "This episode requires access that is not currently available.")
	}

	mediaPath, err := s.locator.Locate(record)
	if err != nil {
		return domain.PlaybackDescriptor{}, err
	}
	signed, err := s.signer.Sign(mediaPath, s.cfg.Playback.URLTTL)
	if err != nil {
		return domain.PlaybackDescriptor{}, err
	}
	expiresAt := time.Now().UTC().Add(s.cfg.Playback.URLTTL)
	sessionID, err := s.repo.InsertSession(ctx, userID, record, mode, request, expiresAt)
	if err != nil {
		return domain.PlaybackDescriptor{}, err
	}

	cacheRecord := domain.SessionCacheRecord{
		SessionID:                sessionID,
		UserID:                   userID,
		DramaID:                  record.DramaID,
		EpisodeID:                record.EpisodeID,
		PlaybackMode:             mode,
		MediaPath:                mediaPath,
		ExpiresAt:                expiresAt.Format(time.RFC3339),
		PreviewSeconds:           record.PreviewSeconds,
		HeartbeatIntervalSeconds: int(s.cfg.Playback.HeartbeatInterval.Seconds()),
		RefreshAfterSeconds:      int(s.cfg.Playback.RefreshBefore.Seconds()),
		SourcePage:               request.SourcePage,
		SignerMode:               s.signer.Mode(),
		NextEpisodeID:            record.NextEpisodeID,
		NextEpisodeTitle:         record.NextEpisodeTitle,
	}
	if err := s.repo.CacheSession(ctx, cacheRecord, s.cfg.Playback.URLTTL); err != nil {
		return domain.PlaybackDescriptor{}, err
	}

	logger := sharedlogger.WithRequest(s.baseLogger, requestID, traceID).With().
		Str("session_id", sessionID).
		Str("user_id", userID).
		Str("episode_id", record.EpisodeID).
		Str("playback_mode", string(mode)).
		Str("signer_mode", s.signer.Mode()).
		Logger()
	logger.Info().Msg("playback session created")

	return s.toDescriptor(cacheRecord, signed), nil
}

func (s Service) GetSession(ctx context.Context, sessionID string) (domain.SessionDetail, error) {
	return s.repo.GetSessionDetail(ctx, sessionID)
}

func (s Service) Heartbeat(ctx context.Context, userID string, sessionID string, request domain.HeartbeatRequest, requestID string, traceID string) (domain.HeartbeatResponse, error) {
	session, err := s.repo.GetCachedSession(ctx, sessionID)
	if err != nil {
		return domain.HeartbeatResponse{}, err
	}
	if session.UserID != userID {
		return domain.HeartbeatResponse{}, apperrors.ErrForbidden
	}
	expiresAt, err := time.Parse(time.RFC3339, session.ExpiresAt)
	if err != nil {
		return domain.HeartbeatResponse{}, fmt.Errorf("parse session expiry: %w", err)
	}
	if time.Now().UTC().After(expiresAt) {
		return domain.HeartbeatResponse{}, apperrors.New(410, "playback.session_expired", "The playback session has expired.")
	}

	var previewRemaining *int
	var nextAction *string
	if session.PlaybackMode == domain.PlaybackModePreview && session.PreviewSeconds > 0 {
		remaining := session.PreviewSeconds - request.PositionSeconds
		if remaining < 0 {
			remaining = 0
		}
		previewRemaining = &remaining
		if remaining == 0 {
			action := "show_paywall"
			nextAction = &action
		}
	}
	response := domain.HeartbeatResponse{
		KeepAlive:               true,
		ExpiresAt:               session.ExpiresAt,
		ShouldRefreshURL:        expiresAt.Sub(time.Now().UTC()) <= s.cfg.Playback.RefreshBefore,
		PreviewRemainingSeconds: previewRemaining,
		NextAction:              nextAction,
	}

	logger := sharedlogger.WithRequest(s.baseLogger, requestID, traceID).With().
		Str("session_id", sessionID).
		Str("user_id", userID).
		Str("episode_id", session.EpisodeID).
		Str("playback_mode", string(session.PlaybackMode)).
		Str("signer_mode", session.SignerMode).
		Logger()
	logger.Info().
		Int("position_seconds", request.PositionSeconds).
		Msg("playback heartbeat accepted")

	return response, nil
}

func (s Service) Complete(ctx context.Context, userID string, sessionID string, request domain.CompleteRequest, requestID string, traceID string) (domain.CompleteResponse, error) {
	session, err := s.repo.GetCachedSession(ctx, sessionID)
	if err != nil {
		return domain.CompleteResponse{}, err
	}
	if session.UserID != userID {
		return domain.CompleteResponse{}, apperrors.ErrForbidden
	}
	if err := s.repo.MarkSessionCompleted(ctx, sessionID); err != nil {
		return domain.CompleteResponse{}, err
	}
	_ = s.repo.DeleteCachedSession(ctx, sessionID)

	logger := sharedlogger.WithRequest(s.baseLogger, requestID, traceID).With().
		Str("session_id", sessionID).
		Str("user_id", userID).
		Str("episode_id", session.EpisodeID).
		Str("playback_mode", string(session.PlaybackMode)).
		Str("signer_mode", session.SignerMode).
		Logger()
	logger.Info().
		Int("watched_seconds", request.WatchedSeconds).
		Bool("completed", request.Completed).
		Msg("playback session completed")

	return domain.CompleteResponse{Accepted: true, NextEpisodeID: session.NextEpisodeID}, nil
}

func (s Service) Refresh(ctx context.Context, userID string, sessionID string, requestID string, traceID string) (domain.SessionRefreshResponse, error) {
	session, err := s.repo.GetCachedSession(ctx, sessionID)
	if err != nil {
		return domain.SessionRefreshResponse{}, err
	}
	if session.UserID != userID {
		return domain.SessionRefreshResponse{}, apperrors.ErrForbidden
	}
	signed, err := s.signer.Sign(session.MediaPath, s.cfg.Playback.URLTTL)
	if err != nil {
		return domain.SessionRefreshResponse{}, err
	}
	expiresAt := time.Now().UTC().Add(s.cfg.Playback.URLTTL)
	session.ExpiresAt = expiresAt.Format(time.RFC3339)
	if err := s.repo.CacheSession(ctx, session, s.cfg.Playback.URLTTL); err != nil {
		return domain.SessionRefreshResponse{}, err
	}
	if err := s.repo.UpdateSessionExpiry(ctx, sessionID, expiresAt); err != nil {
		return domain.SessionRefreshResponse{}, err
	}

	logger := sharedlogger.WithRequest(s.baseLogger, requestID, traceID).With().
		Str("session_id", sessionID).
		Str("user_id", userID).
		Str("episode_id", session.EpisodeID).
		Str("playback_mode", string(session.PlaybackMode)).
		Str("signer_mode", session.SignerMode).
		Logger()
	logger.Info().Msg("playback session refreshed")

	return domain.SessionRefreshResponse{Descriptor: s.toDescriptor(session, signed)}, nil
}

func (s Service) GrantDevEntitlement(ctx context.Context, request domain.DevEntitlementGrantRequest) error {
	return s.repo.GrantDevEntitlement(ctx, request)
}

func (s Service) RevokeDevEntitlement(ctx context.Context, userID string) error {
	return s.repo.RevokeDevEntitlement(ctx, userID)
}

func (s Service) toDescriptor(session domain.SessionCacheRecord, signed signer.SignedPlayback) domain.PlaybackDescriptor {
	headers := make([]domain.HeaderKV, 0, len(signed.Headers))
	for key, value := range signed.Headers {
		headers = append(headers, domain.HeaderKV{Key: key, Value: value})
	}
	var nextHint *domain.NextEpisodeHint
	if session.NextEpisodeID != nil {
		title := "Next episode"
		if session.NextEpisodeTitle != nil {
			title = *session.NextEpisodeTitle
		}
		nextHint = &domain.NextEpisodeHint{EpisodeID: *session.NextEpisodeID, Title: title}
	}
	return domain.PlaybackDescriptor{
		SessionID:                session.SessionID,
		PlaybackMode:             session.PlaybackMode,
		MediaURL:                 signed.URL,
		RequestHeaders:           headers,
		ExpiresAt:                session.ExpiresAt,
		PreviewSeconds:           session.PreviewSeconds,
		HeartbeatIntervalSeconds: session.HeartbeatIntervalSeconds,
		RefreshAfterSeconds:      session.RefreshAfterSeconds,
		NextEpisodeHint:          nextHint,
		AnalyticsContext: domain.AnalyticsContext{
			SessionID:    session.SessionID,
			DramaID:      session.DramaID,
			EpisodeID:    session.EpisodeID,
			PlaybackMode: string(session.PlaybackMode),
			SourcePage:   session.SourcePage,
			SignerMode:   session.SignerMode,
		},
	}
}
