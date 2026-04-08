package service

import (
	"context"

	"dramaflow/backend/services/progress-service/internal/domain"
)

type Service struct {
	repo progressRepository
}

type progressRepository interface {
	GetEpisodeProgress(ctx context.Context, userID string, episodeID string) (domain.EpisodeProgress, error)
	UpsertEpisodeProgress(ctx context.Context, userID string, episodeID string, input domain.ProgressUpsertRequest) (domain.EpisodeProgress, error)
	RecentHistory(ctx context.Context, userID string) ([]domain.RecentHistoryItem, error)
}

func New(repo progressRepository) Service {
	return Service{repo: repo}
}

func (s Service) GetEpisodeProgress(ctx context.Context, userID string, episodeID string) (domain.EpisodeProgress, error) {
	return s.repo.GetEpisodeProgress(ctx, userID, episodeID)
}

func (s Service) UpsertEpisodeProgress(ctx context.Context, userID string, episodeID string, input domain.ProgressUpsertRequest) (domain.EpisodeProgress, error) {
	return s.repo.UpsertEpisodeProgress(ctx, userID, episodeID, input)
}

func (s Service) RecentHistory(ctx context.Context, userID string) ([]domain.RecentHistoryItem, error) {
	return s.repo.RecentHistory(ctx, userID)
}
