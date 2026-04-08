package service

import (
	"context"
	"testing"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/admin-service/internal/audit"
	"dramaflow/backend/services/admin-service/internal/domain"
)

type adminRepoMock struct {
	feedConfig domain.FeedHomeConfig
	feedErr    error
}

func (m adminRepoMock) EnsureBootstrapAdmin(ctx context.Context, email string, passwordHash string) error {
	return nil
}
func (m adminRepoMock) FindAdminByEmail(ctx context.Context, email string) (domain.AdminUser, string, error) {
	return domain.AdminUser{}, "", nil
}
func (m adminRepoMock) CreateSession(ctx context.Context, adminUserID string, sessionHash string, expiresAt time.Time) error {
	return nil
}
func (m adminRepoMock) TouchAdminLogin(ctx context.Context, adminUserID string) error { return nil }
func (m adminRepoMock) FindAdminBySessionHash(ctx context.Context, sessionHash string) (domain.AdminUser, error) {
	return domain.AdminUser{}, nil
}
func (m adminRepoMock) RevokeSession(ctx context.Context, sessionHash string) error { return nil }
func (m adminRepoMock) DashboardStats(ctx context.Context) (domain.DashboardStats, error) {
	return domain.DashboardStats{}, nil
}
func (m adminRepoMock) ListDramas(ctx context.Context, search string, status string, page int, pageSize int) (domain.DramaListResponse, error) {
	return domain.DramaListResponse{}, nil
}
func (m adminRepoMock) CreateDrama(ctx context.Context, request domain.DramaMutationRequest) (domain.DramaRecord, error) {
	return domain.DramaRecord{}, nil
}
func (m adminRepoMock) GetDrama(ctx context.Context, dramaID string) (domain.DramaRecord, error) {
	return domain.DramaRecord{}, nil
}
func (m adminRepoMock) UpdateDrama(ctx context.Context, dramaID string, request domain.DramaMutationRequest) (domain.DramaRecord, error) {
	return domain.DramaRecord{}, nil
}
func (m adminRepoMock) ListEpisodes(ctx context.Context, dramaID string) ([]domain.EpisodeRecord, error) {
	return nil, nil
}
func (m adminRepoMock) CreateEpisode(ctx context.Context, dramaID string, request domain.EpisodeMutationRequest) (domain.EpisodeRecord, error) {
	return domain.EpisodeRecord{}, nil
}
func (m adminRepoMock) GetEpisode(ctx context.Context, episodeID string) (domain.EpisodeRecord, error) {
	return domain.EpisodeRecord{}, nil
}
func (m adminRepoMock) UpdateEpisode(ctx context.Context, episodeID string, request domain.EpisodeMutationRequest) (domain.EpisodeRecord, error) {
	return domain.EpisodeRecord{}, nil
}
func (m adminRepoMock) SetEpisodePublishStatus(ctx context.Context, episodeID string, status string) error { return nil }
func (m adminRepoMock) GetFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error) {
	return m.feedConfig, m.feedErr
}
func (m adminRepoMock) SaveFeedDraft(ctx context.Context, request domain.FeedConfigMutationRequest) (domain.FeedHomeConfig, error) {
	return domain.FeedHomeConfig{}, nil
}
func (m adminRepoMock) PublishFeedConfig(ctx context.Context, region string, language string, actorID string) (domain.FeedHomeConfig, error) {
	return domain.FeedHomeConfig{}, nil
}
func (m adminRepoMock) RollbackFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error) {
	return domain.FeedHomeConfig{}, nil
}
func (m adminRepoMock) ListUsers(ctx context.Context, query string, page int, pageSize int) (domain.UserSummaryResponse, error) {
	return domain.UserSummaryResponse{}, nil
}
func (m adminRepoMock) ListPurchases(ctx context.Context, query string, page int, pageSize int) (domain.PurchaseSearchResponse, error) {
	return domain.PurchaseSearchResponse{}, nil
}
func (m adminRepoMock) GetPurchase(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	return domain.PurchaseRecord{}, nil
}
func (m adminRepoMock) ListEntitlements(ctx context.Context, userID string, page int, pageSize int) (domain.EntitlementSearchResponse, error) {
	return domain.EntitlementSearchResponse{}, nil
}
func (m adminRepoMock) ListRTDNEvents(ctx context.Context, query string, page int, pageSize int) (domain.RTDNEventResponse, error) {
	return domain.RTDNEventResponse{}, nil
}
func (m adminRepoMock) GetRTDNEvent(ctx context.Context, messageID string) (domain.RTDNEventRecord, error) {
	return domain.RTDNEventRecord{}, nil
}
func (m adminRepoMock) ListPlaybackSessions(ctx context.Context, query string, page int, pageSize int) (domain.PlaybackSessionResponse, error) {
	return domain.PlaybackSessionResponse{}, nil
}
func (m adminRepoMock) GetPlaybackSession(ctx context.Context, sessionID string) (domain.PlaybackSessionRecord, error) {
	return domain.PlaybackSessionRecord{}, nil
}
func (m adminRepoMock) ListAuditLogs(ctx context.Context, query string, page int, pageSize int) (domain.AuditLogResponse, error) {
	return domain.AuditLogResponse{}, nil
}

type billingClientMock struct{}

func (billingClientMock) ResyncPurchase(ctx context.Context, purchaseToken string) error { return nil }
func (billingClientMock) ReplayRTDN(ctx context.Context, payload map[string]any) error    { return nil }

type entitlementClientMock struct{}

func (entitlementClientMock) Recompute(ctx context.Context, userID string) error          { return nil }
func (entitlementClientMock) Grant(ctx context.Context, payload map[string]any) error     { return nil }
func (entitlementClientMock) Revoke(ctx context.Context, payload map[string]any) error    { return nil }

func TestGetFeedConfigReturnsDefaultDraftWhenConfigMissing(t *testing.T) {
	svc := New(
		adminRepoMock{feedErr: apperrors.ErrNotFound},
		audit.MetricsWriter{},
		billingClientMock{},
		entitlementClientMock{},
		config.Config{},
	)

	resp, err := svc.GetFeedConfig(context.Background(), "US", "zh-CN")
	if err != nil {
		t.Fatalf("GetFeedConfig() error = %v", err)
	}
	if resp.Region != "US" || resp.Language != "zh-CN" {
		t.Fatalf("unexpected fallback config: %#v", resp)
	}
}

func TestGrantTempEntitlementRequiresPermission(t *testing.T) {
	svc := New(adminRepoMock{}, audit.MetricsWriter{}, billingClientMock{}, entitlementClientMock{}, config.Config{})

	err := svc.GrantTempEntitlement(context.Background(), "user-1", domain.GrantTempRequest{
		ProductID: "premium_access",
		Reason:    "test",
	}, domain.AdminUser{Role: "support_agent"}, "req-1", "trace-1")
	if err == nil {
		t.Fatalf("expected forbidden error")
	}
}
