package repository

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/playback-service/internal/domain"
	"github.com/go-redis/redis/v8"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool  *pgxpool.Pool
	redis *redis.Client
}

func New(pool *pgxpool.Pool, redisClient *redis.Client) Repository {
	return Repository{pool: pool, redis: redisClient}
}

func (r Repository) GetEpisodeRecord(ctx context.Context, episodeID string) (domain.EpisodeRecord, error) {
	const query = `
		SELECT e.drama_id, e.id, e.title, e.episode_no, e.duration_seconds, e.preview_seconds, e.is_premium, e.publish_status, e.stream_key_placeholder,
		       next_e.id, next_e.title
		FROM episodes e
		LEFT JOIN episodes next_e
		    ON next_e.drama_id = e.drama_id
		   AND next_e.episode_no = e.episode_no + 1
		   AND next_e.publish_status = 'published'
		WHERE e.id = $1
	`
	var record domain.EpisodeRecord
	err := r.pool.QueryRow(ctx, query, episodeID).Scan(
		&record.DramaID,
		&record.EpisodeID,
		&record.Title,
		&record.EpisodeNo,
		&record.DurationSeconds,
		&record.PreviewSeconds,
		&record.IsPremium,
		&record.PublishStatus,
		&record.MediaPath,
		&record.NextEpisodeID,
		&record.NextEpisodeTitle,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.EpisodeRecord{}, apperrors.ErrNotFound
		}
		return domain.EpisodeRecord{}, fmt.Errorf("get episode record: %w", err)
	}
	return record, nil
}

func (r Repository) InsertSession(ctx context.Context, userID string, record domain.EpisodeRecord, mode domain.PlaybackMode, request domain.CreatePlaybackSessionRequest, expiresAt time.Time) (string, error) {
	const query = `
		INSERT INTO playback_sessions (
			id, user_id, drama_id, episode_id, playback_mode, media_path, session_status, issued_at, expires_at,
			client_app, client_platform, client_version, created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, NOW(), $8, $9, $10, $11, NOW(), NOW())
	`
	sessionID := uuid.NewString()
	_, err := r.pool.Exec(
		ctx,
		query,
		sessionID,
		userID,
		record.DramaID,
		record.EpisodeID,
		string(mode),
		record.MediaPath,
		string(domain.SessionStatusActive),
		expiresAt,
		"dramaflow-android",
		request.DeviceContext.Platform,
		request.DeviceContext.AppVersion,
	)
	if err != nil {
		return "", fmt.Errorf("insert playback session: %w", err)
	}
	return sessionID, nil
}

func (r Repository) UpdateSessionExpiry(ctx context.Context, sessionID string, expiresAt time.Time) error {
	const query = `
		UPDATE playback_sessions
		SET expires_at = $2, updated_at = NOW()
		WHERE id = $1
	`
	_, err := r.pool.Exec(ctx, query, sessionID, expiresAt)
	if err != nil {
		return fmt.Errorf("update session expiry: %w", err)
	}
	return nil
}

func (r Repository) MarkSessionCompleted(ctx context.Context, sessionID string) error {
	const query = `
		UPDATE playback_sessions
		SET session_status = $2, completed_at = NOW(), updated_at = NOW()
		WHERE id = $1
	`
	_, err := r.pool.Exec(ctx, query, sessionID, string(domain.SessionStatusCompleted))
	if err != nil {
		return fmt.Errorf("mark session completed: %w", err)
	}
	return nil
}

func (r Repository) GetSessionDetail(ctx context.Context, sessionID string) (domain.SessionDetail, error) {
	const query = `
		SELECT id, user_id, drama_id, episode_id, playback_mode, session_status, media_path, issued_at, expires_at, completed_at
		FROM playback_sessions
		WHERE id = $1
	`
	var detail domain.SessionDetail
	var issuedAt time.Time
	var expiresAt time.Time
	var completedAt *time.Time
	err := r.pool.QueryRow(ctx, query, sessionID).Scan(
		&detail.SessionID,
		&detail.UserID,
		&detail.DramaID,
		&detail.EpisodeID,
		&detail.PlaybackMode,
		&detail.SessionStatus,
		&detail.MediaPath,
		&issuedAt,
		&expiresAt,
		&completedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.SessionDetail{}, apperrors.ErrNotFound
		}
		return domain.SessionDetail{}, fmt.Errorf("get session detail: %w", err)
	}
	detail.IssuedAt = issuedAt.UTC().Format(time.RFC3339)
	detail.ExpiresAt = expiresAt.UTC().Format(time.RFC3339)
	if completedAt != nil {
		value := completedAt.UTC().Format(time.RFC3339)
		detail.CompletedAt = &value
	}
	return detail, nil
}

func (r Repository) CacheSession(ctx context.Context, session domain.SessionCacheRecord, ttl time.Duration) error {
	payload, err := json.Marshal(session)
	if err != nil {
		return fmt.Errorf("marshal session cache: %w", err)
	}
	if err := r.redis.Set(ctx, r.sessionKey(session.SessionID), payload, ttl).Err(); err != nil {
		return fmt.Errorf("cache session: %w", err)
	}
	return nil
}

func (r Repository) GetCachedSession(ctx context.Context, sessionID string) (domain.SessionCacheRecord, error) {
	value, err := r.redis.Get(ctx, r.sessionKey(sessionID)).Result()
	if err != nil {
		if errors.Is(err, redis.Nil) {
			return domain.SessionCacheRecord{}, apperrors.ErrNotFound
		}
		return domain.SessionCacheRecord{}, fmt.Errorf("get cached session: %w", err)
	}
	var record domain.SessionCacheRecord
	if err := json.Unmarshal([]byte(value), &record); err != nil {
		return domain.SessionCacheRecord{}, fmt.Errorf("unmarshal cached session: %w", err)
	}
	return record, nil
}

func (r Repository) DeleteCachedSession(ctx context.Context, sessionID string) error {
	if err := r.redis.Del(ctx, r.sessionKey(sessionID)).Err(); err != nil {
		return fmt.Errorf("delete cached session: %w", err)
	}
	return nil
}

func (r Repository) GrantDevEntitlement(ctx context.Context, request domain.DevEntitlementGrantRequest) error {
	const query = `
		INSERT INTO dev_entitlements (
			id, user_id, entitlement_type, drama_id, episode_id, active, source, expires_at, created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, TRUE, 'dev_api', $6, NOW(), NOW())
	`
	var expiresAt any
	if request.ExpiresAt != nil {
		expiresAt = *request.ExpiresAt
	}
	_, err := r.pool.Exec(ctx, query, uuid.NewString(), request.UserID, request.EntitlementType, request.DramaID, request.EpisodeID, expiresAt)
	if err != nil {
		return fmt.Errorf("grant dev entitlement: %w", err)
	}
	return nil
}

func (r Repository) RevokeDevEntitlement(ctx context.Context, userID string) error {
	const query = `
		UPDATE dev_entitlements
		SET active = FALSE, updated_at = NOW()
		WHERE user_id = $1
	`
	_, err := r.pool.Exec(ctx, query, userID)
	if err != nil {
		return fmt.Errorf("revoke dev entitlement: %w", err)
	}
	return nil
}

func (r Repository) FindDevEntitlements(ctx context.Context, userID string, dramaID string, episodeID string) ([]string, error) {
	const query = `
		SELECT entitlement_type
		FROM dev_entitlements
		WHERE user_id = $1
		  AND active = TRUE
		  AND (expires_at IS NULL OR expires_at > NOW())
		  AND (drama_id IS NULL OR drama_id = $2)
		  AND (episode_id IS NULL OR episode_id = $3)
	`
	rows, err := r.pool.Query(ctx, query, userID, dramaID, episodeID)
	if err != nil {
		return nil, fmt.Errorf("find dev entitlements: %w", err)
	}
	defer rows.Close()

	var values []string
	for rows.Next() {
		var entitlementType string
		if err := rows.Scan(&entitlementType); err != nil {
			return nil, err
		}
		values = append(values, entitlementType)
	}
	return values, rows.Err()
}

func (r Repository) sessionKey(sessionID string) string {
	return fmt.Sprintf("playback:session:%s", sessionID)
}
