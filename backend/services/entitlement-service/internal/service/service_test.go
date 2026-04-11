package service

import (
	"context"
	"encoding/json"
	"errors"
	"sync"
	"testing"
	"time"

	"dramaflow/backend/services/entitlement-service/internal/domain"
	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
)

type entitlementIdemEntry struct {
	RequestHash string
	Status      string
	Response    []byte
}

type entitlementRepoMock struct {
	mu               sync.Mutex
	items            []domain.Entitlement
	idempotency      map[string]entitlementIdemEntry
	upsertGrantCalls int
	revokeCalls      int
}

func newEntitlementRepoMock() *entitlementRepoMock {
	return &entitlementRepoMock{
		idempotency: map[string]entitlementIdemEntry{},
	}
}

func (m *entitlementRepoMock) ListByUser(ctx context.Context, userID string) ([]domain.Entitlement, error) {
	return m.items, nil
}

func (m *entitlementRepoMock) UpsertGrant(ctx context.Context, request domain.GrantRequest) (domain.Entitlement, *string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.upsertGrantCalls++
	stateBefore := "pending"
	item := domain.Entitlement{
		EntitlementID:       "entitlement-1",
		UserID:              request.UserID,
		EntitlementType:     request.EntitlementType,
		ProductID:           request.ProductID,
		ScopeType:           request.ScopeType,
		ScopeRef:            request.ScopeRef,
		State:               request.State,
		StartsAt:            request.StartsAt,
		EndsAt:              request.EndsAt,
		SourcePurchaseToken: request.SourcePurchaseToken,
		LastSyncedAt:        request.StartsAt,
	}
	return item, &stateBefore, nil
}

func (m *entitlementRepoMock) InsertAuditLog(ctx context.Context, entitlementID *string, userID string, action string, purchaseToken string, stateBefore *string, stateAfter string, reason string, payload map[string]any) error {
	return nil
}

func (m *entitlementRepoMock) RevokeByPurchaseToken(ctx context.Context, request domain.RevokeRequest) ([]domain.Entitlement, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.revokeCalls++
	return []domain.Entitlement{
		{
			EntitlementID:       "entitlement-1",
			UserID:              request.UserID,
			EntitlementType:     "subscription",
			ProductID:           "premium_access",
			ScopeType:           "global",
			State:               request.State,
			SourcePurchaseToken: request.SourcePurchaseToken,
			StartsAt:            "2026-04-08T00:00:00Z",
			LastSyncedAt:        "2026-04-08T00:00:00Z",
		},
	}, nil
}

func (m *entitlementRepoMock) AcquireIdempotency(ctx context.Context, scope string, idempotencyKey string, requestHash string, ttl time.Duration) (domain.IdempotencyAcquireResult, error) {
	composite := scope + "|" + idempotencyKey
	m.mu.Lock()
	defer m.mu.Unlock()
	entry, ok := m.idempotency[composite]
	if !ok {
		m.idempotency[composite] = entitlementIdemEntry{RequestHash: requestHash, Status: "processing"}
		return domain.IdempotencyAcquireResult{State: "acquired"}, nil
	}
	if entry.RequestHash != requestHash {
		return domain.IdempotencyAcquireResult{State: "conflict"}, nil
	}
	switch entry.Status {
	case "succeeded":
		return domain.IdempotencyAcquireResult{State: "replay", CachedResponse: entry.Response}, nil
	case "failed":
		entry.Status = "processing"
		m.idempotency[composite] = entry
		return domain.IdempotencyAcquireResult{State: "acquired"}, nil
	default:
		return domain.IdempotencyAcquireResult{State: "in_progress"}, nil
	}
}

func (m *entitlementRepoMock) MarkIdempotencySucceeded(ctx context.Context, scope string, idempotencyKey string, response any) error {
	composite := scope + "|" + idempotencyKey
	raw, err := json.Marshal(response)
	if err != nil {
		return err
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	entry := m.idempotency[composite]
	entry.Status = "succeeded"
	entry.Response = raw
	m.idempotency[composite] = entry
	return nil
}

func (m *entitlementRepoMock) MarkIdempotencyFailed(ctx context.Context, scope string, idempotencyKey string, appErr apperrors.AppError) error {
	composite := scope + "|" + idempotencyKey
	m.mu.Lock()
	defer m.mu.Unlock()
	entry := m.idempotency[composite]
	entry.Status = "failed"
	m.idempotency[composite] = entry
	return nil
}

type entitlementProjectorMock struct {
	resp domain.RecomputeResponse
	err  error
}

func (m entitlementProjectorMock) Recompute(ctx context.Context, userID string) (domain.RecomputeResponse, error) {
	return m.resp, m.err
}

func testEntitlementService(repo *entitlementRepoMock) Service {
	return New(repo, entitlementProjectorMock{}, config.Config{
		Entitlement: config.EntitlementConfig{DefaultPreviewSeconds: 15},
	})
}

func testGrantRequest() domain.GrantRequest {
	return domain.GrantRequest{
		UserID:              "user-1",
		EntitlementType:     "subscription",
		ProductID:           "premium_access",
		ScopeType:           "global",
		State:               "active",
		StartsAt:            "2026-04-08T00:00:00Z",
		SourcePurchaseToken: "purchase-1",
		Reason:              "purchase_sync",
	}
}

func testRevokeRequest() domain.RevokeRequest {
	return domain.RevokeRequest{
		UserID:              "user-1",
		SourcePurchaseToken: "purchase-1",
		State:               "expired",
		Reason:              "purchase_sync",
	}
}

func TestGrant_FirstSuccessAndReplay(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	first, err := svc.Grant(context.Background(), testGrantRequest(), "idem-grant-1")
	if err != nil {
		t.Fatalf("first Grant() error = %v", err)
	}
	second, err := svc.Grant(context.Background(), testGrantRequest(), "idem-grant-1")
	if err != nil {
		t.Fatalf("replay Grant() error = %v", err)
	}
	if first.EntitlementID == "" || second.EntitlementID == "" {
		t.Fatalf("unexpected entitlement response first=%#v second=%#v", first, second)
	}
	if repo.upsertGrantCalls != 1 {
		t.Fatalf("expected one grant upsert call, got %d", repo.upsertGrantCalls)
	}
}

func TestGrant_ConcurrentDuplicateRequests(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	var wg sync.WaitGroup
	wg.Add(2)
	errs := make(chan error, 2)
	for i := 0; i < 2; i++ {
		go func() {
			defer wg.Done()
			_, err := svc.Grant(context.Background(), testGrantRequest(), "idem-grant-concurrent")
			errs <- err
		}()
	}
	wg.Wait()
	close(errs)

	var successCount int
	var inProgressCount int
	for err := range errs {
		if err == nil {
			successCount++
			continue
		}
		var appErr apperrors.AppError
		if errors.As(err, &appErr) && appErr.Code == "entitlement.idempotency_in_progress" {
			inProgressCount++
		}
	}
	if successCount < 1 {
		t.Fatalf("expected at least one successful grant, got success=%d in_progress=%d", successCount, inProgressCount)
	}
	if successCount+inProgressCount != 2 {
		t.Fatalf("unexpected concurrent outcome success=%d in_progress=%d", successCount, inProgressCount)
	}
	if repo.upsertGrantCalls != 1 {
		t.Fatalf("expected exactly one grant write, got %d", repo.upsertGrantCalls)
	}
}

func TestRevoke_FirstSuccessAndReplay(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	first, err := svc.Revoke(context.Background(), testRevokeRequest(), "idem-revoke-1")
	if err != nil {
		t.Fatalf("first Revoke() error = %v", err)
	}
	second, err := svc.Revoke(context.Background(), testRevokeRequest(), "idem-revoke-1")
	if err != nil {
		t.Fatalf("replay Revoke() error = %v", err)
	}
	if len(first) == 0 || len(second) == 0 {
		t.Fatalf("unexpected revoke response first=%#v second=%#v", first, second)
	}
	if repo.revokeCalls != 1 {
		t.Fatalf("expected one revoke call, got %d", repo.revokeCalls)
	}
}

func TestRevoke_IdempotencyConflictOnDifferentPayload(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	_, err := svc.Revoke(context.Background(), testRevokeRequest(), "idem-revoke-same")
	if err != nil {
		t.Fatalf("first Revoke() error = %v", err)
	}

	diff := testRevokeRequest()
	diff.State = "revoked"
	_, err = svc.Revoke(context.Background(), diff, "idem-revoke-same")
	if err == nil {
		t.Fatal("expected idempotency key conflict error")
	}
	var appErr apperrors.AppError
	if !errors.As(err, &appErr) || appErr.Code != "entitlement.idempotency_key_conflict" {
		t.Fatalf("expected entitlement.idempotency_key_conflict, got %v", err)
	}
}

func TestPlaybackAccessFallsBackToPreview(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	resp, err := svc.PlaybackAccess(context.Background(), "user-1", domain.PlaybackAccessQuery{EpisodeID: "episode-1"})
	if err != nil {
		t.Fatalf("PlaybackAccess() error = %v", err)
	}
	if resp.AccessLevel != "preview_only" {
		t.Fatalf("expected preview_only, got %#v", resp)
	}
}

func TestRecomputeRequiresUserID(t *testing.T) {
	repo := newEntitlementRepoMock()
	svc := testEntitlementService(repo)

	_, err := svc.Recompute(context.Background(), "")
	if err == nil {
		t.Fatal("expected validation error")
	}
	var appErr apperrors.AppError
	if !errors.As(err, &appErr) || appErr.Code != apperrors.ErrValidation.Code {
		t.Fatalf("expected validation code %s, got %v", apperrors.ErrValidation.Code, err)
	}
}

func TestMeResponseSerializesEntitlementsAsArray(t *testing.T) {
	repo := newEntitlementRepoMock()
	repo.items = nil
	svc := testEntitlementService(repo)

	resp, err := svc.Me(context.Background(), "user-1")
	if err != nil {
		t.Fatalf("Me() error = %v", err)
	}
	payload, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	var decoded map[string]any
	if err := json.Unmarshal(payload, &decoded); err != nil {
		t.Fatalf("json.Unmarshal() error = %v", err)
	}
	entitlements, ok := decoded["entitlements"].([]any)
	if !ok {
		t.Fatalf("expected entitlements array, got %#v", decoded["entitlements"])
	}
	if len(entitlements) != 0 {
		t.Fatalf("expected empty entitlements array, got %d", len(entitlements))
	}
}
