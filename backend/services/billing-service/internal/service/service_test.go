package service

import (
	"context"
	"encoding/json"
	"errors"
	"sync"
	"testing"
	"time"

	"dramaflow/backend/services/billing-service/internal/domain"
	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
)

type idemEntry struct {
	RequestHash string
	Status      string
	Response    []byte
}

type billingRepoMock struct {
	mu                  sync.Mutex
	record              domain.PurchaseRecord
	idempotency         map[string]idemEntry
	upsertPurchaseCalls int
	upsertOrderCalls    int
}

func newBillingRepoMock() *billingRepoMock {
	return &billingRepoMock{
		idempotency: map[string]idemEntry{},
	}
}

func (m *billingRepoMock) UpsertPurchaseRecord(ctx context.Context, verified domain.VerifiedSubscription, source string) (domain.PurchaseRecord, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.upsertPurchaseCalls++
	return domain.PurchaseRecord{
		PurchaseToken:        verified.PurchaseToken,
		UserID:               verified.UserID,
		PackageName:          verified.PackageName,
		ProductID:            verified.ProductID,
		PurchaseState:        verified.PurchaseState,
		AcknowledgementState: verified.AcknowledgementState,
		Source:               source,
		StartTime:            verified.StartTime,
		ExpiryTime:           verified.ExpiryTime,
	}, nil
}

func (m *billingRepoMock) UpsertOrder(ctx context.Context, verified domain.VerifiedSubscription, state string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.upsertOrderCalls++
	return nil
}

func (m *billingRepoMock) GetPurchaseRecord(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	return m.record, nil
}

func (m *billingRepoMock) GetLatestPurchaseByUser(ctx context.Context, userID string) (domain.PurchaseRecord, error) {
	return m.record, nil
}

func (m *billingRepoMock) InsertRTDNEvent(ctx context.Context, event domain.RtdnDomainEvent) (bool, error) {
	return false, nil
}

func (m *billingRepoMock) MarkRTDNError(ctx context.Context, messageID string, errorMessage string) error {
	return nil
}

func (m *billingRepoMock) MarkRTDNProcessed(ctx context.Context, messageID string) error {
	return nil
}

func (m *billingRepoMock) GetCatalogProducts(ctx context.Context) ([]map[string]any, error) {
	return nil, nil
}

func (m *billingRepoMock) AcquireIdempotency(ctx context.Context, scope string, idempotencyKey string, requestHash string, ttl time.Duration) (domain.IdempotencyAcquireResult, error) {
	composite := scope + "|" + idempotencyKey
	m.mu.Lock()
	defer m.mu.Unlock()
	entry, ok := m.idempotency[composite]
	if !ok {
		m.idempotency[composite] = idemEntry{RequestHash: requestHash, Status: "processing"}
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

func (m *billingRepoMock) MarkIdempotencySucceeded(ctx context.Context, scope string, idempotencyKey string, response any) error {
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

func (m *billingRepoMock) MarkIdempotencyFailed(ctx context.Context, scope string, idempotencyKey string, appErr apperrors.AppError) error {
	composite := scope + "|" + idempotencyKey
	m.mu.Lock()
	defer m.mu.Unlock()
	entry := m.idempotency[composite]
	entry.Status = "failed"
	m.idempotency[composite] = entry
	return nil
}

type billingEntitlementClientMock struct{}

func (billingEntitlementClientMock) Grant(ctx context.Context, payload map[string]any) error {
	return nil
}
func (billingEntitlementClientMock) Revoke(ctx context.Context, payload map[string]any) error {
	return nil
}
func (billingEntitlementClientMock) Me(ctx context.Context, accessToken string) (map[string]any, error) {
	return map[string]any{}, nil
}

type billingPublisherMock struct {
	verified domain.VerifiedSubscription
	err      error
}

func (m billingPublisherMock) GetSubscriptionPurchaseV2(ctx context.Context, purchaseToken string) (domain.VerifiedSubscription, error) {
	if m.err != nil {
		return domain.VerifiedSubscription{}, m.err
	}
	return m.verified, nil
}

func (m billingPublisherMock) AcknowledgeSubscriptionPurchase(ctx context.Context, purchaseToken string) error {
	return nil
}

func testSyncRequest() domain.SyncPurchaseRequest {
	return domain.SyncPurchaseRequest{
		PurchaseToken: "purchase-1",
		ProductID:     "premium_access",
		PackageName:   "com.dramaflow.app",
		Source:        "client_sync",
	}
}

func testService(repo *billingRepoMock, publisher billingPublisherMock) Service {
	return New(
		repo,
		billingEntitlementClientMock{},
		publisher,
		config.Config{Billing: config.BillingConfig{SyncAckMode: "record_only"}},
	)
}

func TestSyncPurchase_FirstSuccessAndReplay(t *testing.T) {
	start := "2026-04-08T00:00:00Z"
	expiry := "2026-05-08T00:00:00Z"
	repo := newBillingRepoMock()
	svc := testService(repo, billingPublisherMock{
		verified: domain.VerifiedSubscription{
			PurchaseToken:        "purchase-1",
			UserID:               "user-1",
			PackageName:          "com.dramaflow.app",
			ProductID:            "premium_access",
			PurchaseState:        "active",
			AcknowledgementState: "pending",
			StartTime:            &start,
			ExpiryTime:           &expiry,
		},
	})

	first, err := svc.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-1", "idem-1")
	if err != nil {
		t.Fatalf("first SyncPurchase() error = %v", err)
	}
	second, err := svc.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-2", "idem-1")
	if err != nil {
		t.Fatalf("replay SyncPurchase() error = %v", err)
	}

	if !first.SyncAccepted || !second.SyncAccepted {
		t.Fatalf("unexpected response first=%#v second=%#v", first, second)
	}
	if repo.upsertPurchaseCalls != 1 {
		t.Fatalf("expected one upsert call, got %d", repo.upsertPurchaseCalls)
	}
}

func TestSyncPurchase_ConcurrentDuplicateRequests(t *testing.T) {
	start := "2026-04-08T00:00:00Z"
	expiry := "2026-05-08T00:00:00Z"
	repo := newBillingRepoMock()
	svc := testService(repo, billingPublisherMock{
		verified: domain.VerifiedSubscription{
			PurchaseToken:        "purchase-1",
			UserID:               "user-1",
			PackageName:          "com.dramaflow.app",
			ProductID:            "premium_access",
			PurchaseState:        "active",
			AcknowledgementState: "pending",
			StartTime:            &start,
			ExpiryTime:           &expiry,
		},
	})

	var wg sync.WaitGroup
	wg.Add(2)
	errs := make(chan error, 2)
	responses := make(chan domain.SyncPurchaseResponse, 2)
	for i := 0; i < 2; i++ {
		go func() {
			defer wg.Done()
			resp, err := svc.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-concurrent", "idem-concurrent")
			responses <- resp
			errs <- err
		}()
	}
	wg.Wait()
	close(errs)
	close(responses)

	var inProgressCount int
	var successCount int
	for err := range errs {
		if err == nil {
			successCount++
			continue
		}
		var appErr apperrors.AppError
		if errors.As(err, &appErr) && appErr.Code == "billing.idempotency_in_progress" {
			inProgressCount++
		}
	}
	for resp := range responses {
		if resp.SyncAccepted == false && successCount > 0 {
			t.Fatalf("expected accepted response on successful calls, got %#v", resp)
		}
	}
	// Depending on timing, second caller may hit:
	// 1) in_progress (while first is still processing), or
	// 2) replay (after first already committed idempotent success).
	if successCount < 1 {
		t.Fatalf("expected at least one successful response, got success=%d inProgress=%d", successCount, inProgressCount)
	}
	if successCount+inProgressCount != 2 {
		t.Fatalf("unexpected concurrent outcome success=%d inProgress=%d", successCount, inProgressCount)
	}
	if repo.upsertPurchaseCalls != 1 {
		t.Fatalf("expected exactly one business write, got %d", repo.upsertPurchaseCalls)
	}
}

func TestSyncPurchase_IdempotencyConflictOnDifferentPayload(t *testing.T) {
	start := "2026-04-08T00:00:00Z"
	expiry := "2026-05-08T00:00:00Z"
	repo := newBillingRepoMock()
	svc := testService(repo, billingPublisherMock{
		verified: domain.VerifiedSubscription{
			PurchaseToken:        "purchase-1",
			UserID:               "user-1",
			PackageName:          "com.dramaflow.app",
			ProductID:            "premium_access",
			PurchaseState:        "active",
			AcknowledgementState: "pending",
			StartTime:            &start,
			ExpiryTime:           &expiry,
		},
	})

	_, err := svc.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-1", "idem-same")
	if err != nil {
		t.Fatalf("first SyncPurchase() error = %v", err)
	}

	different := testSyncRequest()
	different.ProductID = "other_product"
	_, err = svc.SyncPurchase(context.Background(), "user-1", different, "trace-2", "idem-same")
	if err == nil {
		t.Fatal("expected idempotency key conflict error")
	}
	var appErr apperrors.AppError
	if !errors.As(err, &appErr) || appErr.Code != "billing.idempotency_key_conflict" {
		t.Fatalf("expected billing.idempotency_key_conflict, got %v", err)
	}
}

func TestSyncPurchase_RetryAfterFailureUsesSameKey(t *testing.T) {
	repo := newBillingRepoMock()
	svcFail := testService(repo, billingPublisherMock{
		err: errors.New("publisher down"),
	})
	_, firstErr := svcFail.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-fail", "idem-retry")
	if firstErr == nil {
		t.Fatal("expected first call to fail")
	}

	start := "2026-04-08T00:00:00Z"
	expiry := "2026-05-08T00:00:00Z"
	svcRecover := testService(repo, billingPublisherMock{
		verified: domain.VerifiedSubscription{
			PurchaseToken:        "purchase-1",
			UserID:               "user-1",
			PackageName:          "com.dramaflow.app",
			ProductID:            "premium_access",
			PurchaseState:        "active",
			AcknowledgementState: "pending",
			StartTime:            &start,
			ExpiryTime:           &expiry,
		},
	})
	_, secondErr := svcRecover.SyncPurchase(context.Background(), "user-1", testSyncRequest(), "trace-retry", "idem-retry")
	if secondErr != nil {
		t.Fatalf("expected retry success, got %v", secondErr)
	}
}

func TestHandleRTDNEventReturnsDuplicateWhenMessageAlreadyProcessed(t *testing.T) {
	repo := newBillingRepoMock()
	svc := testService(repo, billingPublisherMock{})

	resp, duplicate, err := svc.HandleRTDNEvent(context.Background(), domain.RtdnDomainEvent{
		MessageID:     "msg-1",
		PurchaseToken: "purchase-1",
		PackageName:   "com.dramaflow.app",
	}, "trace-1")
	if err != nil {
		t.Fatalf("HandleRTDNEvent() error = %v", err)
	}
	if !duplicate || resp.PurchaseState != "duplicate" {
		t.Fatalf("expected duplicate RTDN handling, got %#v duplicate=%v", resp, duplicate)
	}
}
