package service

import (
	"context"
	"testing"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/auth-service/internal/domain"
)

type authRepoMock struct {
	user domain.User
}

func (m authRepoMock) FindOrCreateGuestUser(ctx context.Context, anonymousDeviceID string) (domain.User, error) {
	return domain.User{ID: "guest-1", AnonymousDeviceID: anonymousDeviceID, Status: "active"}, nil
}
func (m authRepoMock) StoreRefreshToken(ctx context.Context, userID string, tokenHash string, expiresAt time.Time) error {
	return nil
}
func (m authRepoMock) FindUserByRefreshToken(ctx context.Context, tokenHash string) (domain.User, error) {
	if m.user.ID == "" {
		return domain.User{}, apperrors.ErrUnauthorized
	}
	return m.user, nil
}
func (m authRepoMock) RevokeRefreshToken(ctx context.Context, tokenHash string) error { return nil }
func (m authRepoMock) FindUserByID(ctx context.Context, userID string) (domain.User, error) {
	if m.user.ID == "" {
		return domain.User{}, apperrors.ErrUnauthorized
	}
	return m.user, nil
}

func testAuthConfig() config.Config {
	return config.Config{
		JWT: config.JWTConfig{
			Secret:          "test-secret",
			Issuer:          "dramaflow.test",
			AccessTokenTTL:  15 * time.Minute,
			RefreshTokenTTL: 24 * time.Hour,
		},
	}
}

func TestGuestSessionIssuesTokens(t *testing.T) {
	svc := New(authRepoMock{}, testAuthConfig())

	resp, err := svc.GuestSession(context.Background(), domain.GuestSessionRequest{AnonymousDeviceID: "device-1"})
	if err != nil {
		t.Fatalf("GuestSession() error = %v", err)
	}
	if resp.AccessToken == "" || resp.RefreshToken == "" {
		t.Fatalf("expected token pair, got %#v", resp)
	}
}

func TestMeRejectsUnknownUser(t *testing.T) {
	svc := New(authRepoMock{}, testAuthConfig())

	_, err := svc.Me(context.Background(), "missing")
	if err == nil {
		t.Fatalf("expected error")
	}
}
