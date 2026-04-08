package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"dramaflow/backend/services/feed-service/internal/domain"
	"github.com/go-redis/redis/v8"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool  *pgxpool.Pool
	redis *redis.Client
}

type HomeConfig struct {
	Region      string
	Language    string
	Published   map[string]any
}

func New(pool *pgxpool.Pool, redisClient *redis.Client) Repository {
	return Repository{pool: pool, redis: redisClient}
}

func (r Repository) ListPublishedDramas(ctx context.Context) ([]domain.FeedItem, error) {
	const cacheKey = "feed:home:published"
	if cached, err := r.redis.Get(ctx, cacheKey).Result(); err == nil {
		var items []domain.FeedItem
		if json.Unmarshal([]byte(cached), &items) == nil {
			return items, nil
		}
	}

	const query = `
		SELECT id, title, short_description, poster_url, cover_url, tags, is_featured
		FROM dramas
		WHERE publish_status = 'published'
		ORDER BY is_featured DESC, updated_at DESC
	`
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("list feed dramas: %w", err)
	}
	defer rows.Close()

	var items []domain.FeedItem
	for rows.Next() {
		var item domain.FeedItem
		var tags []byte
		if err := rows.Scan(&item.DramaID, &item.Title, &item.ShortDescription, &item.PosterURL, &item.CoverURL, &tags, &item.IsFeatured); err != nil {
			return nil, err
		}
		_ = json.Unmarshal(tags, &item.Tags)
		items = append(items, item)
	}

	if payload, err := json.Marshal(items); err == nil {
		_ = r.redis.Set(ctx, cacheKey, payload, 30*time.Second).Err()
	}
	return items, rows.Err()
}

func (r Repository) ContinueWatching(ctx context.Context, userID string) ([]domain.ContinueWatchingItem, error) {
	const query = `
		SELECT wp.drama_id, wp.episode_id, d.title, FLOOR((wp.position_seconds::decimal / NULLIF(wp.duration_seconds, 0)) * 100), wp.updated_at
		FROM watch_progress wp
		JOIN dramas d ON d.id = wp.drama_id
		WHERE wp.user_id = $1
		ORDER BY wp.updated_at DESC
		LIMIT 10
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("continue watching: %w", err)
	}
	defer rows.Close()

	var items []domain.ContinueWatchingItem
	for rows.Next() {
		var item domain.ContinueWatchingItem
		var updatedAt time.Time
		if err := rows.Scan(&item.DramaID, &item.EpisodeID, &item.Title, &item.ProgressPercent, &updatedAt); err != nil {
			return nil, err
		}
		item.UpdatedAtISO8601 = updatedAt.UTC().Format(time.RFC3339)
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r Repository) LoadPublishedHomeConfig(ctx context.Context, region string, language string) (HomeConfig, error) {
	const query = `
		SELECT region, language, published_payload
		FROM feed_home_configs
		WHERE region = $1 AND language = $2
	`
	var config HomeConfig
	var payload []byte
	if err := r.pool.QueryRow(ctx, query, region, language).Scan(&config.Region, &config.Language, &payload); err != nil {
		return HomeConfig{}, err
	}
	_ = json.Unmarshal(payload, &config.Published)
	return config, nil
}
