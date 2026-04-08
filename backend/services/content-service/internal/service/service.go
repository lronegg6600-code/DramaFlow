package service

import (
	"context"

	"dramaflow/backend/services/content-service/internal/domain"
)

type Service struct {
	repo contentRepository
}

type contentRepository interface {
	ListDramas(ctx context.Context) ([]domain.Drama, error)
	GetDrama(ctx context.Context, dramaID string) (domain.Drama, error)
	ListEpisodes(ctx context.Context, dramaID string) ([]domain.Episode, error)
	GetEpisode(ctx context.Context, episodeID string) (domain.Episode, error)
}

func New(repo contentRepository) Service {
	return Service{repo: repo}
}

func (s Service) ListDramas(ctx context.Context) ([]domain.Drama, error)          { return s.repo.ListDramas(ctx) }
func (s Service) GetDrama(ctx context.Context, dramaID string) (domain.Drama, error) { return s.repo.GetDrama(ctx, dramaID) }
func (s Service) ListEpisodes(ctx context.Context, dramaID string) ([]domain.Episode, error) {
	return s.repo.ListEpisodes(ctx, dramaID)
}
func (s Service) GetEpisode(ctx context.Context, episodeID string) (domain.Episode, error) {
	return s.repo.GetEpisode(ctx, episodeID)
}
