package service

import (
	"context"
	"time"

	"dramaflow/backend/services/entitlement-service/internal/domain"
	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
)

type Service struct {
	repo      entitlementRepository
	projector entitlementProjector
	cfg       config.Config
}

type entitlementRepository interface {
	ListByUser(ctx context.Context, userID string) ([]domain.Entitlement, error)
	UpsertGrant(ctx context.Context, request domain.GrantRequest) (domain.Entitlement, *string, error)
	InsertAuditLog(ctx context.Context, entitlementID *string, userID string, action string, purchaseToken string, stateBefore *string, stateAfter string, reason string, payload map[string]any) error
	RevokeByPurchaseToken(ctx context.Context, request domain.RevokeRequest) ([]domain.Entitlement, error)
	AcquireIdempotency(ctx context.Context, scope string, idempotencyKey string, requestHash string, ttl time.Duration) (domain.IdempotencyAcquireResult, error)
	MarkIdempotencySucceeded(ctx context.Context, scope string, idempotencyKey string, response any) error
	MarkIdempotencyFailed(ctx context.Context, scope string, idempotencyKey string, appErr apperrors.AppError) error
}

type entitlementProjector interface {
	Recompute(ctx context.Context, userID string) (domain.RecomputeResponse, error)
}

func New(repo entitlementRepository, projector entitlementProjector, cfg config.Config) Service {
	return Service{repo: repo, projector: projector, cfg: cfg}
}

func (s Service) Me(ctx context.Context, userID string) (domain.MeResponse, error) {
	items, err := s.repo.ListByUser(ctx, userID)
	if err != nil {
		return domain.MeResponse{}, err
	}
	items = normalizeEntitlements(items)
	var activeProduct *string
	isPremium := false
	for _, item := range items {
		if item.State == "active" || item.State == "grace" {
			isPremium = true
			activeProduct = &item.ProductID
			break
		}
	}
	return domain.MeResponse{
		UserID:        userID,
		Entitlements:  items,
		IsPremium:     isPremium,
		ActiveProduct: activeProduct,
		Source:        "entitlement-service",
	}, nil
}

func normalizeEntitlements(items []domain.Entitlement) []domain.Entitlement {
	if items == nil {
		return make([]domain.Entitlement, 0)
	}
	return items
}

func (s Service) PlaybackAccess(ctx context.Context, userID string, _ domain.PlaybackAccessQuery) (domain.PlaybackAccessResponse, error) {
	me, err := s.Me(ctx, userID)
	if err != nil {
		return domain.PlaybackAccessResponse{}, err
	}
	for _, item := range me.Entitlements {
		if item.State == "active" || item.State == "grace" {
			return domain.PlaybackAccessResponse{
				AccessLevel:       "full_access",
				EntitlementSource: item.SourcePurchaseToken,
				ExpiresAt:         item.EndsAt,
				Reason:            "subscription entitlement active",
			}, nil
		}
	}
	previewSeconds := s.cfg.Entitlement.DefaultPreviewSeconds
	return domain.PlaybackAccessResponse{
		AccessLevel:       "preview_only",
		EntitlementSource: "policy_preview",
		PreviewSeconds:    &previewSeconds,
		Reason:            "no active entitlement, preview policy applies",
	}, nil
}

func (s Service) Grant(ctx context.Context, request domain.GrantRequest, providedIdempotencyKey string) (domain.Entitlement, error) {
	normalized := s.NormalizeGrant(request)
	idempotencyKey := buildGrantIdempotencyKey(normalized, providedIdempotencyKey)
	requestHash := buildGrantRequestHash(normalized)
	// Grant and revoke are externally retried by billing/replay workers.
	// This gate keeps the write path deterministic under retries and duplicate deliveries.
	acquire, err := s.repo.AcquireIdempotency(ctx, entitlementGrantScope, idempotencyKey, requestHash, entitlementIdempotencyTTL())
	if err != nil {
		return domain.Entitlement{}, err
	}
	switch acquire.State {
	case "replay":
		return decodeCachedGrantResponse(acquire.CachedResponse)
	case "in_progress":
		return domain.Entitlement{}, apperrors.New(409, "entitlement.idempotency_in_progress", "A request with the same idempotency key is still processing.")
	case "conflict":
		return domain.Entitlement{}, apperrors.New(409, "entitlement.idempotency_key_conflict", "Idempotency key was reused with a different request payload.")
	}

	item, stateBefore, err := s.repo.UpsertGrant(ctx, normalized)
	if err != nil {
		_ = s.repo.MarkIdempotencyFailed(ctx, entitlementGrantScope, idempotencyKey, apperrors.New(500, "entitlement.upsert_grant_failed", err.Error()))
		return domain.Entitlement{}, err
	}
	if err := s.repo.InsertAuditLog(ctx, &item.EntitlementID, request.UserID, "grant", request.SourcePurchaseToken, stateBefore, item.State, request.Reason, request.PayloadSnapshot); err != nil {
		_ = s.repo.MarkIdempotencyFailed(ctx, entitlementGrantScope, idempotencyKey, apperrors.New(500, "entitlement.audit_log_failed", err.Error()))
		return domain.Entitlement{}, err
	}
	if err := s.repo.MarkIdempotencySucceeded(ctx, entitlementGrantScope, idempotencyKey, item); err != nil {
		return domain.Entitlement{}, err
	}
	return item, nil
}

func (s Service) Revoke(ctx context.Context, request domain.RevokeRequest, providedIdempotencyKey string) ([]domain.Entitlement, error) {
	idempotencyKey := buildRevokeIdempotencyKey(request, providedIdempotencyKey)
	requestHash := buildRevokeRequestHash(request)
	acquire, err := s.repo.AcquireIdempotency(ctx, entitlementRevokeScope, idempotencyKey, requestHash, entitlementIdempotencyTTL())
	if err != nil {
		return nil, err
	}
	switch acquire.State {
	case "replay":
		return decodeCachedRevokeResponse(acquire.CachedResponse)
	case "in_progress":
		return nil, apperrors.New(409, "entitlement.idempotency_in_progress", "A request with the same idempotency key is still processing.")
	case "conflict":
		return nil, apperrors.New(409, "entitlement.idempotency_key_conflict", "Idempotency key was reused with a different request payload.")
	}

	items, err := s.repo.RevokeByPurchaseToken(ctx, request)
	if err != nil {
		_ = s.repo.MarkIdempotencyFailed(ctx, entitlementRevokeScope, idempotencyKey, apperrors.New(500, "entitlement.revoke_failed", err.Error()))
		return nil, err
	}
	for _, item := range items {
		stateBefore := item.State
		if err := s.repo.InsertAuditLog(ctx, &item.EntitlementID, request.UserID, "revoke", request.SourcePurchaseToken, &stateBefore, request.State, request.Reason, request.PayloadSnapshot); err != nil {
			_ = s.repo.MarkIdempotencyFailed(ctx, entitlementRevokeScope, idempotencyKey, apperrors.New(500, "entitlement.audit_log_failed", err.Error()))
			return nil, err
		}
	}
	if err := s.repo.MarkIdempotencySucceeded(ctx, entitlementRevokeScope, idempotencyKey, items); err != nil {
		return nil, err
	}
	return items, nil
}

func (s Service) Recompute(ctx context.Context, userID string) (domain.RecomputeResponse, error) {
	if userID == "" {
		return domain.RecomputeResponse{}, apperrors.ErrValidation
	}
	return s.projector.Recompute(ctx, userID)
}

func (s Service) NormalizeGrant(request domain.GrantRequest) domain.GrantRequest {
	if request.EntitlementType == "" {
		request.EntitlementType = "subscription"
	}
	if request.ScopeType == "" {
		request.ScopeType = "global"
	}
	if request.State == "" {
		request.State = "active"
	}
	if request.StartsAt == "" {
		request.StartsAt = time.Now().UTC().Format(time.RFC3339)
	}
	return request
}
