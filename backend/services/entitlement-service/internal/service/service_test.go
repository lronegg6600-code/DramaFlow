package service

import (
	"context"
	"testing"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/entitlement-service/internal/domain"
)

type entitlementRepoMock struct {
	items []domain.Entitlement
}

func (m entitlementRepoMock) ListByUser(ctx context.Context, userID string) ([]domain.Entitlement, error) {
	return m.items, nil
}
func (m entitlementRepoMock) UpsertGrant(ctx context.Context, request domain.GrantRequest) (domain.Entitlement, *string, error) {
	return domain.Entitlement{UserID: request.UserID, ProductID: request.ProductID, State: request.State}, nil, nil
}
func (m entitlementRepoMock) InsertAuditLog(ctx context.Context, entitlementID *string, userID string, action string, purchaseToken string, stateBefore *string, stateAfter string, reason string, payload map[string]any) error {
	return nil
}
func (m entitlementRepoMock) RevokeByPurchaseToken(ctx context.Context, request domain.RevokeRequest) ([]domain.Entitlement, error) {
	return []domain.Entitlement{{UserID: request.UserID, State: request.State}}, nil
}

type entitlementProjectorMock struct {
	resp domain.RecomputeResponse
	err  error
}

func (m entitlementProjectorMock) Recompute(ctx context.Context, userID string) (domain.RecomputeResponse, error) {
	return m.resp, m.err
}

func TestPlaybackAccessFallsBackToPreview(t *testing.T) {
	svc := New(entitlementRepoMock{}, entitlementProjectorMock{}, config.Config{
		Entitlement: config.EntitlementConfig{DefaultPreviewSeconds: 15},
	})

	resp, err := svc.PlaybackAccess(context.Background(), "user-1", domain.PlaybackAccessQuery{EpisodeID: "episode-1"})
	if err != nil {
		t.Fatalf("PlaybackAccess() error = %v", err)
	}
	if resp.AccessLevel != "preview_only" {
		t.Fatalf("expected preview_only, got %#v", resp)
	}
}

func TestRecomputeRequiresUserID(t *testing.T) {
	svc := New(entitlementRepoMock{}, entitlementProjectorMock{}, config.Config{})

	_, err := svc.Recompute(context.Background(), "")
	if err == nil || err.(apperrors.AppError).Code != apperrors.ErrValidation.Code {
		t.Fatalf("expected validation error, got %v", err)
	}
}
