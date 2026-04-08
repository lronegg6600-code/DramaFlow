package service

import (
	"context"
	"testing"

	"dramaflow/backend/services/progress-service/internal/domain"
)

type progressRepoMock struct {
	progress domain.EpisodeProgress
}

func (m progressRepoMock) GetEpisodeProgress(ctx context.Context, userID string, episodeID string) (domain.EpisodeProgress, error) {
	return m.progress, nil
}
func (m progressRepoMock) UpsertEpisodeProgress(ctx context.Context, userID string, episodeID string, input domain.ProgressUpsertRequest) (domain.EpisodeProgress, error) {
	return domain.EpisodeProgress{UserID: userID, EpisodeID: episodeID, PositionSeconds: input.PositionSeconds}, nil
}
func (m progressRepoMock) RecentHistory(ctx context.Context, userID string) ([]domain.RecentHistoryItem, error) {
	return []domain.RecentHistoryItem{{EpisodeID: "episode-1"}}, nil
}

func TestUpsertEpisodeProgressPersistsPosition(t *testing.T) {
	svc := New(progressRepoMock{})

	item, err := svc.UpsertEpisodeProgress(context.Background(), "user-1", "episode-1", domain.ProgressUpsertRequest{PositionSeconds: 42})
	if err != nil {
		t.Fatalf("UpsertEpisodeProgress() error = %v", err)
	}
	if item.PositionSeconds != 42 {
		t.Fatalf("expected position 42, got %#v", item)
	}
}
