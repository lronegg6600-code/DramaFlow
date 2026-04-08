package service

import (
	"context"

	"dramaflow/backend/services/feed-service/internal/domain"
	"dramaflow/backend/services/feed-service/internal/repository"
)

type Service struct {
	repo feedRepository
}

type feedRepository interface {
	ListPublishedDramas(ctx context.Context) ([]domain.FeedItem, error)
	LoadPublishedHomeConfig(ctx context.Context, region string, language string) (repository.HomeConfig, error)
	ContinueWatching(ctx context.Context, userID string) ([]domain.ContinueWatchingItem, error)
}

func New(repo feedRepository) Service {
	return Service{repo: repo}
}

func (s Service) Home(ctx context.Context, userID string) (domain.HomeFeed, error) {
	items, err := s.repo.ListPublishedDramas(ctx)
	if err != nil {
		return domain.HomeFeed{}, err
	}
	featured := take(items, 1)
	trending := take(items, 4)
	recommended := reverse(items)
	if config, err := s.repo.LoadPublishedHomeConfig(ctx, "US", "en"); err == nil {
		featured = reorderFromConfig(items, config.Published["featured"])
		trending = reorderFromConfig(items, config.Published["trending"])
		recommended = reorderFromConfig(items, config.Published["recommended"])
		if len(featured) == 0 {
			featured = take(items, 1)
		}
		if len(trending) == 0 {
			trending = take(items, 4)
		}
		if len(recommended) == 0 {
			recommended = reverse(items)
		}
	}
	continueWatching := []domain.ContinueWatchingItem{}
	if userID != "" {
		continueWatching, _ = s.repo.ContinueWatching(ctx, userID)
	}
	return domain.HomeFeed{
		Featured:         featured,
		Trending:         trending,
		Recommended:      recommended,
		ContinueWatching: continueWatching,
	}, nil
}

func (s Service) ContinueWatching(ctx context.Context, userID string) ([]domain.ContinueWatchingItem, error) {
	return s.repo.ContinueWatching(ctx, userID)
}

func take(items []domain.FeedItem, n int) []domain.FeedItem {
	if len(items) < n {
		return items
	}
	return items[:n]
}

func reverse(items []domain.FeedItem) []domain.FeedItem {
	out := make([]domain.FeedItem, 0, len(items))
	for idx := len(items) - 1; idx >= 0; idx-- {
		out = append(out, items[idx])
	}
	return out
}

func reorderFromConfig(items []domain.FeedItem, raw any) []domain.FeedItem {
	list, ok := raw.([]any)
	if !ok || len(list) == 0 {
		return nil
	}
	index := make(map[string]domain.FeedItem, len(items))
	for _, item := range items {
		index[item.DramaID] = item
	}
	out := make([]domain.FeedItem, 0, len(list))
	for _, entry := range list {
		cast, ok := entry.(map[string]any)
		if !ok {
			continue
		}
		dramaID, _ := cast["dramaId"].(string)
		if dramaID == "" {
			continue
		}
		if item, ok := index[dramaID]; ok {
			out = append(out, item)
		}
	}
	return out
}
