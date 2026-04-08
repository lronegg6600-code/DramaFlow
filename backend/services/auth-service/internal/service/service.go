package service

import (
	"context"
	"fmt"
	"time"

	sharedauth "dramaflow/backend/shared/auth"
	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/auth-service/internal/domain"
)

type Service struct {
	repo   authRepository
	tokens sharedauth.TokenManager
	cfg    config.Config
}

type authRepository interface {
	FindOrCreateGuestUser(ctx context.Context, anonymousDeviceID string) (domain.User, error)
	StoreRefreshToken(ctx context.Context, userID string, tokenHash string, expiresAt time.Time) error
	FindUserByRefreshToken(ctx context.Context, tokenHash string) (domain.User, error)
	RevokeRefreshToken(ctx context.Context, tokenHash string) error
	FindUserByID(ctx context.Context, userID string) (domain.User, error)
}

func New(repo authRepository, cfg config.Config) Service {
	return Service{
		repo:   repo,
		tokens: sharedauth.NewTokenManager(cfg.JWT),
		cfg:    cfg,
	}
}

func (s Service) GuestSession(ctx context.Context, req domain.GuestSessionRequest) (domain.SessionResponse, error) {
	user, err := s.repo.FindOrCreateGuestUser(ctx, req.AnonymousDeviceID)
	if err != nil {
		return domain.SessionResponse{}, err
	}
	return s.issueSession(ctx, user)
}

func (s Service) Refresh(ctx context.Context, req domain.RefreshRequest) (domain.SessionResponse, error) {
	user, err := s.repo.FindUserByRefreshToken(ctx, sharedauth.HashRefreshToken(req.RefreshToken))
	if err != nil {
		return domain.SessionResponse{}, apperrors.ErrUnauthorized
	}
	_ = s.repo.RevokeRefreshToken(ctx, sharedauth.HashRefreshToken(req.RefreshToken))
	return s.issueSession(ctx, user)
}

func (s Service) Me(ctx context.Context, userID string) (domain.UserSummary, error) {
	user, err := s.repo.FindUserByID(ctx, userID)
	if err != nil {
		return domain.UserSummary{}, apperrors.ErrUnauthorized
	}
	return domain.UserSummary{ID: user.ID, Status: user.Status}, nil
}

func (s Service) TokenManager() sharedauth.TokenManager {
	return s.tokens
}

func (s Service) issueSession(ctx context.Context, user domain.User) (domain.SessionResponse, error) {
	tokens, tokenHash, err := s.tokens.CreateTokenPair(user.ID)
	if err != nil {
		return domain.SessionResponse{}, fmt.Errorf("create token pair: %w", err)
	}
	if err := s.repo.StoreRefreshToken(ctx, user.ID, tokenHash, time.Now().UTC().Add(s.cfg.JWT.RefreshTokenTTL)); err != nil {
		return domain.SessionResponse{}, err
	}
	return domain.SessionResponse{
		AccessToken:  tokens.AccessToken,
		RefreshToken: tokens.RefreshToken,
		ExpiresAt:    tokens.ExpiresAt,
		User:         domain.UserSummary{ID: user.ID, Status: user.Status},
	}, nil
}
