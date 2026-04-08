package service

import (
	"context"
	"fmt"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/billing-service/internal/domain"
	"dramaflow/backend/services/billing-service/internal/verifier"
)

type Service struct {
	repo              billingRepository
	entitlementClient billingEntitlementClient
	publisherGateway  verifier.GooglePlayPublisherGateway
	cfg               config.Config
}

type billingRepository interface {
	UpsertPurchaseRecord(ctx context.Context, verified domain.VerifiedSubscription, source string) (domain.PurchaseRecord, error)
	UpsertOrder(ctx context.Context, verified domain.VerifiedSubscription, state string) error
	GetPurchaseRecord(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error)
	GetLatestPurchaseByUser(ctx context.Context, userID string) (domain.PurchaseRecord, error)
	InsertRTDNEvent(ctx context.Context, event domain.RtdnDomainEvent) (bool, error)
	MarkRTDNError(ctx context.Context, messageID string, errorMessage string) error
	MarkRTDNProcessed(ctx context.Context, messageID string) error
	GetCatalogProducts(ctx context.Context) ([]map[string]any, error)
}

type billingEntitlementClient interface {
	Grant(ctx context.Context, payload map[string]any) error
	Revoke(ctx context.Context, payload map[string]any) error
	Me(ctx context.Context, accessToken string) (map[string]any, error)
}

func New(repo billingRepository, entitlementClient billingEntitlementClient, publisherGateway verifier.GooglePlayPublisherGateway, cfg config.Config) Service {
	return Service{
		repo:              repo,
		entitlementClient: entitlementClient,
		publisherGateway:  publisherGateway,
		cfg:               cfg,
	}
}

func (s Service) SyncPurchase(ctx context.Context, userID string, request domain.SyncPurchaseRequest, traceID string) (domain.SyncPurchaseResponse, error) {
	if err := ValidateSync(request); err != nil {
		return domain.SyncPurchaseResponse{}, err
	}
	verified, err := s.publisherGateway.GetSubscriptionPurchaseV2(ctx, request.PurchaseToken)
	if err != nil {
		return domain.SyncPurchaseResponse{}, err
	}
	if verified.ProductID == "" {
		verified.ProductID = request.ProductID
	}
	if verified.PackageName == "" {
		verified.PackageName = request.PackageName
	}
	if verified.UserID == "" {
		verified.UserID = userID
	}
	record, err := s.repo.UpsertPurchaseRecord(ctx, verified, request.Source)
	if err != nil {
		return domain.SyncPurchaseResponse{}, err
	}
	if err := s.repo.UpsertOrder(ctx, verified, record.PurchaseState); err != nil {
		return domain.SyncPurchaseResponse{}, err
	}
	entitlementState, effectiveAt, nextAction, err := s.applyEntitlement(ctx, record)
	if err != nil {
		return domain.SyncPurchaseResponse{}, err
	}
	if record.AcknowledgementState != "acknowledged" && s.cfg.Billing.SyncAckMode == "auto_ack" {
		if err := s.publisherGateway.AcknowledgeSubscriptionPurchase(ctx, record.PurchaseToken); err == nil {
			record.AcknowledgementState = "acknowledged"
		}
	}
	return domain.SyncPurchaseResponse{
		SyncAccepted:           true,
		PurchaseState:          record.PurchaseState,
		AcknowledgementState:   record.AcknowledgementState,
		EntitlementState:       entitlementState,
		EntitlementEffectiveAt: effectiveAt,
		NextAction:             nextAction,
		TraceID:                traceID,
	}, nil
}

func (s Service) ResyncPurchase(ctx context.Context, userID string, purchaseToken string, traceID string) (domain.SyncPurchaseResponse, error) {
	if record, err := s.repo.GetPurchaseRecord(ctx, purchaseToken); err == nil {
		return s.SyncPurchase(ctx, userID, domain.SyncPurchaseRequest{
			PurchaseToken: purchaseToken,
			ProductID:     record.ProductID,
			PackageName:   record.PackageName,
			Source:        "manual_sync",
		}, traceID)
	}
	return s.SyncPurchase(ctx, userID, domain.SyncPurchaseRequest{
		PurchaseToken: purchaseToken,
		ProductID:     "premium_access",
		PackageName:   s.cfg.Billing.GooglePlayPackageName,
		Source:        "manual_sync",
	}, traceID)
}

func (s Service) SubscriptionStatus(ctx context.Context, userID string, authorizationHeader string) (domain.SubscriptionStatusResponse, error) {
	status := domain.SubscriptionStatusResponse{
		UserID:               userID,
		PurchaseState:        "not_found",
		AcknowledgementState: "unknown",
		EntitlementState:     "pending",
		Entitlements:         []map[string]any{},
	}
	if record, err := s.repo.GetLatestPurchaseByUser(ctx, userID); err == nil {
		status.PurchaseToken = &record.PurchaseToken
		status.ProductID = &record.ProductID
		status.PurchaseState = record.PurchaseState
		status.AcknowledgementState = record.AcknowledgementState
	}
	if data, err := s.entitlementClient.Me(ctx, authorizationHeader); err == nil {
		if raw, ok := data["entitlements"].([]any); ok {
			status.Entitlements = make([]map[string]any, 0, len(raw))
			for _, item := range raw {
				if cast, ok := item.(map[string]any); ok {
					status.Entitlements = append(status.Entitlements, cast)
					if state, ok := cast["state"].(string); ok && (state == "active" || state == "grace") {
						status.EntitlementState = state
					}
				}
			}
		}
	}
	return status, nil
}

func (s Service) HandleRTDNEvent(ctx context.Context, event domain.RtdnDomainEvent, traceID string) (domain.SyncPurchaseResponse, bool, error) {
	inserted, err := s.repo.InsertRTDNEvent(ctx, event)
	if err != nil {
		return domain.SyncPurchaseResponse{}, false, err
	}
	if !inserted {
		return domain.SyncPurchaseResponse{
			SyncAccepted:         true,
			PurchaseState:        "duplicate",
			AcknowledgementState: "unchanged",
			EntitlementState:     "unchanged",
			NextAction:           "ignore_duplicate_rtdn",
			TraceID:              traceID,
		}, true, nil
	}
	response, err := s.SyncPurchase(ctx, "", domain.SyncPurchaseRequest{
		PurchaseToken: event.PurchaseToken,
		ProductID:     "premium_access",
		PackageName:   event.PackageName,
		Source:        "rtdn",
	}, traceID)
	if err != nil {
		_ = s.repo.MarkRTDNError(ctx, event.MessageID, err.Error())
		return domain.SyncPurchaseResponse{}, false, err
	}
	if err := s.repo.MarkRTDNProcessed(ctx, event.MessageID); err != nil {
		return domain.SyncPurchaseResponse{}, false, err
	}
	return response, false, nil
}

func (s Service) Catalog(ctx context.Context) ([]map[string]any, error) {
	return s.repo.GetCatalogProducts(ctx)
}

func (s Service) applyEntitlement(ctx context.Context, record domain.PurchaseRecord) (string, *string, string, error) {
	payload := map[string]any{
		"userId":              record.UserID,
		"sourcePurchaseToken": record.PurchaseToken,
		"payloadSnapshot": map[string]any{
			"productId":     record.ProductID,
			"purchaseState": record.PurchaseState,
		},
	}

	switch record.PurchaseState {
	case "active", "grace", "hold", "paused":
		payload["entitlementType"] = "subscription"
		payload["productId"] = record.ProductID
		payload["scopeType"] = "global"
		payload["state"] = record.PurchaseState
		payload["startsAt"] = coalesce(record.StartTime, time.Now().UTC().Format(time.RFC3339))
		payload["endsAt"] = record.ExpiryTime
		payload["reason"] = "purchase_sync"
		if err := s.entitlementClient.Grant(ctx, payload); err != nil {
			return "", nil, "", err
		}
		effectiveAt := coalesce(record.StartTime, time.Now().UTC().Format(time.RFC3339))
		return record.PurchaseState, &effectiveAt, decideNextAction(record.PurchaseState, record.AcknowledgementState), nil
	case "revoked", "expired", "canceled":
		payload["state"] = mapRevocationState(record.PurchaseState)
		payload["reason"] = "purchase_sync"
		if err := s.entitlementClient.Revoke(ctx, payload); err != nil {
			return "", nil, "", err
		}
		return mapRevocationState(record.PurchaseState), nil, "refresh_entitlement", nil
	default:
		return "pending", nil, "wait_for_payment_confirmation", nil
	}
}

func RequireSecret(enabled bool, secret string, actual string) error {
	if enabled && secret != "" && secret != actual {
		return apperrors.New(401, "billing.rtdn_unauthorized", "RTDN push secret mismatch")
	}
	return nil
}

func ValidateSync(request domain.SyncPurchaseRequest) error {
	if request.PurchaseToken == "" || request.PackageName == "" || request.ProductID == "" || request.Source == "" {
		return apperrors.ErrValidation
	}
	return nil
}

func decideNextAction(purchaseState string, acknowledgementState string) string {
	if purchaseState == "active" && acknowledgementState != "acknowledged" {
		return "refresh_entitlement_and_wait_ack"
	}
	return "refresh_entitlement"
}

func mapRevocationState(state string) string {
	switch state {
	case "canceled":
		return "expired"
	default:
		return state
	}
}

func coalesce(value *string, fallback string) string {
	if value == nil || *value == "" {
		return fallback
	}
	return *value
}

func EnsureRTDNEnabled(enabled bool) error {
	if !enabled {
		return apperrors.New(404, "billing.rtdn_disabled", "RTDN receiver is disabled.")
	}
	return nil
}

func AuthorizationHeader(raw string) string {
	if raw == "" {
		return ""
	}
	return raw
}

func ManualSyncReason(source string) string {
	if source == "" {
		return fmt.Sprintf("sync@%s", time.Now().UTC().Format(time.RFC3339))
	}
	return source
}
