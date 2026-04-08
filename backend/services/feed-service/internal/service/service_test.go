package service

import (
	"context"
	"testing"

	"dramaflow/backend/services/feed-service/internal/domain"
	"dramaflow/backend/services/feed-service/internal/repository"
)

type feedRepoMock struct {
	items     []domain.FeedItem
	continueW []domain.ContinueWatchingItem
	config    repository.HomeConfig
}

func (m feedRepoMock) ListPublishedDramas(ctx context.Context) ([]domain.FeedItem, error) {
	return m.items, nil
}
func (m feedRepoMock) LoadPublishedHomeConfig(ctx context.Context, region string, language string) (repository.HomeConfig, error) {
	return m.config, nil
}
func (m feedRepoMock) ContinueWatching(ctx context.Context, userID string) ([]domain.ContinueWatchingItem, error) {
	return m.continueW, nil
}

func TestHomeUsesPublishedConfigOrdering(t *testing.T) {
	svc := New(feedRepoMock{
		items: []domain.FeedItem{
			{DramaID: "a", Title: "A"},
			{DramaID: "b", Title: "B"},
		},
		config: repository.HomeConfig{
			Region:   "US",
			Language: "en",
			Published: map[string]any{
				"featured": []any{map[string]any{"dramaId": "b"}},
			},
		},
	})

	feed, err := svc.Home(context.Background(), "")
	if err != nil {
		t.Fatalf("Home() error = %v", err)
	}
	if len(feed.Featured) == 0 || feed.Featured[0].DramaID != "b" {
		t.Fatalf("expected configured ordering, got %#v", feed.Featured)
	}
}
