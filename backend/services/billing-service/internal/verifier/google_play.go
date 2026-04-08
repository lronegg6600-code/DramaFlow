package verifier

import (
	"context"
	"fmt"
	"strings"
	"time"

	"dramaflow/backend/shared/config"
	"dramaflow/backend/services/billing-service/internal/domain"
)

type GooglePlayPublisherGateway interface {
	GetSubscriptionPurchaseV2(ctx context.Context, purchaseToken string) (domain.VerifiedSubscription, error)
	AcknowledgeSubscriptionPurchase(ctx context.Context, purchaseToken string) error
}

type StubGooglePlayPublisherGateway struct {
	cfg config.Config
}

func NewGooglePlayPublisherGateway(cfg config.Config) GooglePlayPublisherGateway {
	return StubGooglePlayPublisherGateway{cfg: cfg}
}

func (g StubGooglePlayPublisherGateway) GetSubscriptionPurchaseV2(ctx context.Context, purchaseToken string) (domain.VerifiedSubscription, error) {
	now := time.Now().UTC()
	expiry := now.Add(30 * 24 * time.Hour)
	basePlan := "monthly"
	if strings.Contains(purchaseToken, "quarter") {
		basePlan = "quarterly"
	}
	orderID := fmt.Sprintf("GPA.%d", now.Unix())
	return domain.VerifiedSubscription{
		PurchaseToken:        purchaseToken,
		OrderID:              &orderID,
		PackageName:          g.cfg.Billing.GooglePlayPackageName,
		ProductID:            "premium_access",
		BasePlanID:           &basePlan,
		PurchaseState:        "active",
		AcknowledgementState: "pending",
		StartTime:            stringPtr(now.Format(time.RFC3339)),
		ExpiryTime:           stringPtr(expiry.Format(time.RFC3339)),
		AutoRenewEnabled:     boolPtr(true),
		LineItemsRaw: []map[string]any{
			{"productId": "premium_access", "basePlanId": basePlan, "expiryTime": expiry.Format(time.RFC3339)},
		},
		RawPayload:    map[string]any{"kind": "androidpublisher#subscriptionPurchaseV2", "purchaseToken": purchaseToken},
		RawAPIVersion: "purchases.subscriptionsv2",
		UserID:        deriveUserID(purchaseToken),
		AmountMicros:  int64Ptr(12990000),
		CurrencyCode:  stringPtr("USD"),
		CountryCode:   stringPtr("US"),
	}, nil
}

func (g StubGooglePlayPublisherGateway) AcknowledgeSubscriptionPurchase(ctx context.Context, purchaseToken string) error {
	return nil
}

func deriveUserID(purchaseToken string) string {
	if strings.Contains(purchaseToken, ":") {
		parts := strings.SplitN(purchaseToken, ":", 2)
		if parts[0] != "" {
			return parts[0]
		}
	}
	return "guest-user"
}

func stringPtr(value string) *string { return &value }
func boolPtr(value bool) *bool { return &value }
func int64Ptr(value int64) *int64 { return &value }
