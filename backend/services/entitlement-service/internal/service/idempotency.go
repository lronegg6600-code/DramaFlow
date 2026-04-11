package service

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"strings"
	"time"

	"dramaflow/backend/services/entitlement-service/internal/domain"
	apperrors "dramaflow/backend/shared/errors"
)

const (
	entitlementGrantScope  = "entitlement.grant"
	entitlementRevokeScope = "entitlement.revoke"
)

func buildGrantIdempotencyKey(request domain.GrantRequest, provided string) string {
	if trimmed := strings.TrimSpace(provided); trimmed != "" {
		return trimmed
	}
	scopeRef := ""
	if request.ScopeRef != nil {
		scopeRef = *request.ScopeRef
	}
	endsAt := ""
	if request.EndsAt != nil {
		endsAt = *request.EndsAt
	}
	base := strings.Join([]string{
		request.UserID,
		request.SourcePurchaseToken,
		request.EntitlementType,
		request.ScopeType,
		scopeRef,
		request.State,
		request.StartsAt,
		endsAt,
	}, "|")
	return "auto:" + digest(base)
}

func buildRevokeIdempotencyKey(request domain.RevokeRequest, provided string) string {
	if trimmed := strings.TrimSpace(provided); trimmed != "" {
		return trimmed
	}
	base := strings.Join([]string{
		request.UserID,
		request.SourcePurchaseToken,
		request.State,
		request.Reason,
	}, "|")
	return "auto:" + digest(base)
}

func buildGrantRequestHash(request domain.GrantRequest) string {
	raw, _ := json.Marshal(request)
	return digest(string(raw))
}

func buildRevokeRequestHash(request domain.RevokeRequest) string {
	raw, _ := json.Marshal(request)
	return digest(string(raw))
}

func decodeCachedGrantResponse(payload []byte) (domain.Entitlement, error) {
	var cached domain.Entitlement
	if err := json.Unmarshal(payload, &cached); err != nil {
		return domain.Entitlement{}, apperrors.New(500, "entitlement.idempotency_payload_corrupt", "Stored idempotency payload is invalid.")
	}
	return cached, nil
}

func decodeCachedRevokeResponse(payload []byte) ([]domain.Entitlement, error) {
	var cached []domain.Entitlement
	if err := json.Unmarshal(payload, &cached); err != nil {
		return nil, apperrors.New(500, "entitlement.idempotency_payload_corrupt", "Stored idempotency payload is invalid.")
	}
	return cached, nil
}

func entitlementIdempotencyTTL() time.Duration {
	return 24 * time.Hour
}

func digest(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}
