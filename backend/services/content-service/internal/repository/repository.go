package repository

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/content-service/internal/domain"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) ListDramas(ctx context.Context) ([]domain.Drama, error) {
	const query = `
		SELECT id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured
		FROM dramas
		WHERE publish_status = 'published'
		ORDER BY is_featured DESC, updated_at DESC
	`
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("list dramas: %w", err)
	}
	defer rows.Close()

	var dramas []domain.Drama
	for rows.Next() {
		var item domain.Drama
		var rawTags []byte
		if err := rows.Scan(&item.ID, &item.Title, &item.ShortDescription, &item.LongDescription, &item.PosterURL, &item.CoverURL, &rawTags, &item.Region, &item.Language, &item.PublishStatus, &item.IsFeatured); err != nil {
			return nil, err
		}
		_ = json.Unmarshal(rawTags, &item.Tags)
		dramas = append(dramas, item)
	}
	return dramas, rows.Err()
}

func (r Repository) GetDrama(ctx context.Context, dramaID string) (domain.Drama, error) {
	const query = `
		SELECT id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured
		FROM dramas
		WHERE id = $1
	`
	var item domain.Drama
	var rawTags []byte
	err := r.pool.QueryRow(ctx, query, dramaID).Scan(&item.ID, &item.Title, &item.ShortDescription, &item.LongDescription, &item.PosterURL, &item.CoverURL, &rawTags, &item.Region, &item.Language, &item.PublishStatus, &item.IsFeatured)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.Drama{}, apperrors.ErrNotFound
		}
		return domain.Drama{}, fmt.Errorf("get drama: %w", err)
	}
	_ = json.Unmarshal(rawTags, &item.Tags)
	return item, nil
}

func (r Repository) ListEpisodes(ctx context.Context, dramaID string) ([]domain.Episode, error) {
	const query = `
		SELECT id, drama_id, episode_no, title, description, duration_seconds, preview_seconds, is_premium, stream_key_placeholder, publish_status, sort_order
		FROM episodes
		WHERE drama_id = $1 AND publish_status = 'published'
		ORDER BY sort_order ASC
	`
	rows, err := r.pool.Query(ctx, query, dramaID)
	if err != nil {
		return nil, fmt.Errorf("list episodes: %w", err)
	}
	defer rows.Close()

	var episodes []domain.Episode
	for rows.Next() {
		var item domain.Episode
		if err := rows.Scan(&item.ID, &item.DramaID, &item.EpisodeNo, &item.Title, &item.Description, &item.DurationSeconds, &item.PreviewSeconds, &item.IsPremium, &item.StreamKeyPlaceholder, &item.PublishStatus, &item.SortOrder); err != nil {
			return nil, err
		}
		episodes = append(episodes, item)
	}
	return episodes, rows.Err()
}

func (r Repository) GetEpisode(ctx context.Context, episodeID string) (domain.Episode, error) {
	const query = `
		SELECT id, drama_id, episode_no, title, description, duration_seconds, preview_seconds, is_premium, stream_key_placeholder, publish_status, sort_order
		FROM episodes
		WHERE id = $1
	`
	var item domain.Episode
	err := r.pool.QueryRow(ctx, query, episodeID).Scan(&item.ID, &item.DramaID, &item.EpisodeNo, &item.Title, &item.Description, &item.DurationSeconds, &item.PreviewSeconds, &item.IsPremium, &item.StreamKeyPlaceholder, &item.PublishStatus, &item.SortOrder)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.Episode{}, apperrors.ErrNotFound
		}
		return domain.Episode{}, fmt.Errorf("get episode: %w", err)
	}
	return item, nil
}
