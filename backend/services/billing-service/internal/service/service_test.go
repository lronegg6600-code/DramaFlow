package service

import (
	"context"
	"testing"

	"dramaflow/backend/shared/config"
	"dramaflow/backend/services/billing-service/internal/domain"
)

type billingRepoMock struct {
	record domain.PurchaseRecord
}

func (m billingRepoMock) UpsertPurchaseRecord(ctx context.Context, verified domain.VerifiedSubscription, source string) (domain.PurchaseRecord, error) {
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
func (m billingRepoMock) UpsertOrder(ctx context.Context, verified domain.VerifiedSubscription, state string) error {
	return nil
}
func (m billingRepoMock) GetPurchaseRecord(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	return m.record, nil
}
func (m billingRepoMock) GetLatestPurchaseByUser(ctx context.Context, userID string) (domain.PurchaseRecord, error) {
	return m.record, nil
}
func (m billingRepoMock) InsertRTDNEvent(ctx context.Context, event domain.RtdnDomainEvent) (bool, error) {
	return false, nil
}
func (m billingRepoMock) MarkRTDNError(ctx context.Context, messageID string, errorMessage string) error { return nil }
func (m billingRepoMock) MarkRTDNProcessed(ctx context.Context, messageID string) error                  { return nil }
func (m billingRepoMock) GetCatalogProducts(ctx context.Context) ([]map[string]any, error)               { return nil, nil }

type billingEntitlementClientMock struct{}

func (billingEntitlementClientMock) Grant(ctx context.Context, payload map[string]any) error { return nil }
func (billingEntitlementClientMock) Revoke(ctx context.Context, payload map[string]any) error { return nil }
func (billingEntitlementClientMock) Me(ctx context.Context, accessToken string) (map[string]any, error) {
	return map[string]any{}, nil
}

type billingPublisherMock struct {
	verified domain.VerifiedSubscription
}

func (m billingPublisherMock) GetSubscriptionPurchaseV2(ctx context.Context, purchaseToken string) (domain.VerifiedSubscription, error) {
	return m.verified, nil
}
func (m billingPublisherMock) AcknowledgeSubscriptionPurchase(ctx context.Context, purchaseToken string) error {
	return nil
}

func TestSyncPurchaseReturnsAcceptedResponse(t *testing.T) {
	start := "2026-04-08T00:00:00Z"
	expiry := "2026-05-08T00:00:00Z"
	svc := New(
		billingRepoMock{},
		billingEntitlementClientMock{},
		billingPublisherMock{verified: domain.VerifiedSubscription{
			PurchaseToken:        "purchase-1",
			UserID:               "user-1",
			PackageName:          "com.dramaflow.app",
			ProductID:            "premium_access",
			PurchaseState:        "active",
			AcknowledgementState: "pending",
			StartTime:            &start,
			ExpiryTime:           &expiry,
		}},
		config.Config{Billing: config.BillingConfig{SyncAckMode: "record_only"}},
	)

	resp, err := svc.SyncPurchase(context.Background(), "user-1", domain.SyncPurchaseRequest{
		PurchaseToken: "purchase-1",
		ProductID:     "premium_access",
		PackageName:   "com.dramaflow.app",
		Source:        "client_sync",
	}, "trace-1")
	if err != nil {
		t.Fatalf("SyncPurchase() error = %v", err)
	}
	if !resp.SyncAccepted || resp.EntitlementState != "active" {
		t.Fatalf("unexpected response: %#v", resp)
	}
}

func TestHandleRTDNEventReturnsDuplicateWhenMessageAlreadyProcessed(t *testing.T) {
	svc := New(
		billingRepoMock{},
		billingEntitlementClientMock{},
		billingPublisherMock{},
		config.Config{},
	)

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
