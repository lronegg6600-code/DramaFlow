package service

import (
	"context"
	"testing"

	"dramaflow/backend/services/content-service/internal/domain"
)

type contentRepoMock struct {
	dramas   []domain.Drama
	episodes []domain.Episode
}

func (m contentRepoMock) ListDramas(ctx context.Context) ([]domain.Drama, error) { return m.dramas, nil }
func (m contentRepoMock) GetDrama(ctx context.Context, dramaID string) (domain.Drama, error) {
	return m.dramas[0], nil
}
func (m contentRepoMock) ListEpisodes(ctx context.Context, dramaID string) ([]domain.Episode, error) {
	return m.episodes, nil
}
func (m contentRepoMock) GetEpisode(ctx context.Context, episodeID string) (domain.Episode, error) {
	return m.episodes[0], nil
}

func TestListDramasDelegatesToRepository(t *testing.T) {
	svc := New(contentRepoMock{dramas: []domain.Drama{{ID: "drama-1", Title: "长安谜局"}}})

	items, err := svc.ListDramas(context.Background())
	if err != nil {
		t.Fatalf("ListDramas() error = %v", err)
	}
	if len(items) != 1 || items[0].Title != "长安谜局" {
		t.Fatalf("unexpected dramas: %#v", items)
	}
}
