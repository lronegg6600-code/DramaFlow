package repository

import (
	"context"
	"errors"
	"fmt"
	"time"

	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/progress-service/internal/domain"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) GetEpisodeProgress(ctx context.Context, userID string, episodeID string) (domain.EpisodeProgress, error) {
	const query = `
		SELECT user_id, drama_id, episode_id, position_seconds, duration_seconds, completed, updated_at
		FROM watch_progress
		WHERE user_id = $1 AND episode_id = $2
	`
	var progress domain.EpisodeProgress
	var updatedAt time.Time
	err := r.pool.QueryRow(ctx, query, userID, episodeID).Scan(&progress.UserID, &progress.DramaID, &progress.EpisodeID, &progress.PositionSeconds, &progress.DurationSeconds, &progress.Completed, &updatedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.EpisodeProgress{}, apperrors.ErrNotFound
		}
		return domain.EpisodeProgress{}, fmt.Errorf("get episode progress: %w", err)
	}
	progress.UpdatedAt = updatedAt.UTC().Format(time.RFC3339)
	return progress, nil
}

func (r Repository) UpsertEpisodeProgress(ctx context.Context, userID string, episodeID string, input domain.ProgressUpsertRequest) (domain.EpisodeProgress, error) {
	const progressQuery = `
		INSERT INTO watch_progress (id, user_id, drama_id, episode_id, position_seconds, duration_seconds, completed, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
		ON CONFLICT (user_id, episode_id) DO UPDATE
		SET drama_id = EXCLUDED.drama_id,
			position_seconds = EXCLUDED.position_seconds,
			duration_seconds = EXCLUDED.duration_seconds,
			completed = EXCLUDED.completed,
			updated_at = NOW()
		RETURNING user_id, drama_id, episode_id, position_seconds, duration_seconds, completed, updated_at
	`
	var progress domain.EpisodeProgress
	var updatedAt time.Time
	err := r.pool.QueryRow(ctx, progressQuery, uuid.NewString(), userID, input.DramaID, episodeID, input.PositionSeconds, input.DurationSeconds, input.Completed).Scan(
		&progress.UserID, &progress.DramaID, &progress.EpisodeID, &progress.PositionSeconds, &progress.DurationSeconds, &progress.Completed, &updatedAt,
	)
	if err != nil {
		return domain.EpisodeProgress{}, fmt.Errorf("upsert progress: %w", err)
	}

	const historyQuery = `
		INSERT INTO watch_history (id, user_id, drama_id, episode_id, watched_at)
		VALUES ($1, $2, $3, $4, NOW())
		ON CONFLICT (user_id, episode_id) DO UPDATE SET watched_at = NOW()
	`
	if _, err := r.pool.Exec(ctx, historyQuery, uuid.NewString(), userID, input.DramaID, episodeID); err != nil {
		return domain.EpisodeProgress{}, fmt.Errorf("upsert history: %w", err)
	}
	progress.UpdatedAt = updatedAt.UTC().Format(time.RFC3339)
	return progress, nil
}

func (r Repository) RecentHistory(ctx context.Context, userID string) ([]domain.RecentHistoryItem, error) {
	const query = `
		SELECT drama_id, episode_id, watched_at
		FROM watch_history
		WHERE user_id = $1
		ORDER BY watched_at DESC
		LIMIT 20
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("recent history: %w", err)
	}
	defer rows.Close()

	var items []domain.RecentHistoryItem
	for rows.Next() {
		var item domain.RecentHistoryItem
		var watchedAt time.Time
		if err := rows.Scan(&item.DramaID, &item.EpisodeID, &watchedAt); err != nil {
			return nil, err
		}
		item.WatchedAt = watchedAt.UTC().Format(time.RFC3339)
		items = append(items, item)
	}
	return items, rows.Err()
}
