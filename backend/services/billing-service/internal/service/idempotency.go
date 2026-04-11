package service

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"strings"
	"time"

	"dramaflow/backend/services/billing-service/internal/domain"
	apperrors "dramaflow/backend/shared/errors"
)

const (
	billingSyncScope = "billing.sync_purchase"
)

func buildSyncIdempotencyKey(userID string, request domain.SyncPurchaseRequest, provided string) string {
	if trimmed := strings.TrimSpace(provided); trimmed != "" {
		return trimmed
	}
	base := strings.Join([]string{
		userID,
		request.PurchaseToken,
		request.ProductID,
		request.PackageName,
		request.Source,
	}, "|")
	return "auto:" + digest(base)
}

func buildSyncRequestHash(userID string, request domain.SyncPurchaseRequest) string {
	// Keep this hash bound to the full semantic request payload.
	// If the same idempotency key arrives with different payload, we reject with 409.
	raw, _ := json.Marshal(struct {
		UserID  string                     `json:"userId"`
		Request domain.SyncPurchaseRequest `json:"request"`
	}{
		UserID:  userID,
		Request: request,
	})
	return digest(string(raw))
}

func decodeCachedSyncResponse(payload []byte) (domain.SyncPurchaseResponse, error) {
	var cached domain.SyncPurchaseResponse
	if err := json.Unmarshal(payload, &cached); err != nil {
		return domain.SyncPurchaseResponse{}, apperrors.New(500, "billing.idempotency_payload_corrupt", "Stored idempotency payload is invalid.")
	}
	return cached, nil
}

func idempotencyTTL() time.Duration {
	// 24h is enough for client retries and replay windows without keeping records forever.
	return 24 * time.Hour
}

func digest(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}
