package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"dramaflow/backend/services/entitlement-service/internal/domain"
	apperrors "dramaflow/backend/shared/errors"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PurchaseRecordSnapshot struct {
	UserID        string
	ProductID     string
	PurchaseToken string
	PurchaseState string
	StartTime     *time.Time
	ExpiryTime    *time.Time
}

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) ListByUser(ctx context.Context, userID string) ([]domain.Entitlement, error) {
	const query = `
		SELECT entitlement_id, user_id, entitlement_type, product_id, scope_type, scope_ref, state, starts_at, ends_at, source_purchase_token, last_synced_at
		FROM entitlements
		WHERE user_id = $1
		ORDER BY updated_at DESC
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("list entitlements: %w", err)
	}
	defer rows.Close()

	var results []domain.Entitlement
	for rows.Next() {
		item, err := scanEntitlement(rows)
		if err != nil {
			return nil, err
		}
		results = append(results, item)
	}
	return results, rows.Err()
}

func (r Repository) UpsertGrant(ctx context.Context, request domain.GrantRequest) (domain.Entitlement, *string, error) {
	const selectQuery = `
		SELECT state
		FROM entitlements
		WHERE source_purchase_token = $1
		  AND scope_type = $2
		  AND COALESCE(scope_ref, '') = COALESCE($3, '')
		  AND entitlement_type = $4
	`
	var stateBefore *string
	var existingState string
	if err := r.pool.QueryRow(ctx, selectQuery, request.SourcePurchaseToken, request.ScopeType, request.ScopeRef, request.EntitlementType).Scan(&existingState); err == nil {
		stateBefore = &existingState
	}

	const query = `
		INSERT INTO entitlements (
			entitlement_id, user_id, entitlement_type, product_id, scope_type, scope_ref, state, starts_at, ends_at, source_purchase_token, last_synced_at, created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, NOW(), NOW(), NOW())
		ON CONFLICT (source_purchase_token, scope_type, COALESCE(scope_ref, ''), entitlement_type)
		DO UPDATE SET
			user_id = EXCLUDED.user_id,
			product_id = EXCLUDED.product_id,
			state = EXCLUDED.state,
			starts_at = EXCLUDED.starts_at,
			ends_at = EXCLUDED.ends_at,
			last_synced_at = NOW(),
			updated_at = NOW()
		RETURNING entitlement_id, user_id, entitlement_type, product_id, scope_type, scope_ref, state, starts_at, ends_at, source_purchase_token, last_synced_at
	`
	row := r.pool.QueryRow(
		ctx,
		query,
		uuid.NewString(),
		request.UserID,
		request.EntitlementType,
		request.ProductID,
		request.ScopeType,
		request.ScopeRef,
		request.State,
		request.StartsAt,
		request.EndsAt,
		request.SourcePurchaseToken,
	)
	item, err := scanEntitlement(row)
	if err != nil {
		return domain.Entitlement{}, nil, fmt.Errorf("upsert grant: %w", err)
	}
	return item, stateBefore, nil
}

func (r Repository) RevokeByPurchaseToken(ctx context.Context, request domain.RevokeRequest) ([]domain.Entitlement, error) {
	const query = `
		UPDATE entitlements
		SET state = $2, last_synced_at = NOW(), updated_at = NOW()
		WHERE user_id = $1
		  AND source_purchase_token = $3
		RETURNING entitlement_id, user_id, entitlement_type, product_id, scope_type, scope_ref, state, starts_at, ends_at, source_purchase_token, last_synced_at
	`
	rows, err := r.pool.Query(ctx, query, request.UserID, request.State, request.SourcePurchaseToken)
	if err != nil {
		return nil, fmt.Errorf("revoke entitlements: %w", err)
	}
	defer rows.Close()

	var items []domain.Entitlement
	for rows.Next() {
		item, err := scanEntitlement(rows)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r Repository) InsertAuditLog(ctx context.Context, entitlementID *string, userID string, action string, purchaseToken string, stateBefore *string, stateAfter string, reason string, payload map[string]any) error {
	raw, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("marshal audit snapshot: %w", err)
	}
	const query = `
		INSERT INTO entitlement_audit_logs (
			audit_id, entitlement_id, user_id, action, source_purchase_token, state_before, state_after, reason, payload_snapshot, created_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, NOW())
	`
	_, err = r.pool.Exec(ctx, query, uuid.NewString(), entitlementID, userID, action, purchaseToken, stateBefore, stateAfter, reason, raw)
	if err != nil {
		return fmt.Errorf("insert entitlement audit log: %w", err)
	}
	return nil
}

func (r Repository) ListPurchaseSnapshotsByUser(ctx context.Context, userID string) ([]PurchaseRecordSnapshot, error) {
	const query = `
		SELECT user_id, product_id, purchase_token, purchase_state, start_time, expiry_time
		FROM billing_purchase_records
		WHERE user_id = $1
		ORDER BY updated_at DESC
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("list purchase snapshots: %w", err)
	}
	defer rows.Close()

	var items []PurchaseRecordSnapshot
	for rows.Next() {
		var item PurchaseRecordSnapshot
		if err := rows.Scan(&item.UserID, &item.ProductID, &item.PurchaseToken, &item.PurchaseState, &item.StartTime, &item.ExpiryTime); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

type scanner interface {
	Scan(dest ...any) error
}

func scanEntitlement(row scanner) (domain.Entitlement, error) {
	var item domain.Entitlement
	var startsAt time.Time
	var endsAt *time.Time
	var lastSyncedAt time.Time
	var scopeRef *string
	if err := row.Scan(
		&item.EntitlementID,
		&item.UserID,
		&item.EntitlementType,
		&item.ProductID,
		&item.ScopeType,
		&scopeRef,
		&item.State,
		&startsAt,
		&endsAt,
		&item.SourcePurchaseToken,
		&lastSyncedAt,
	); err != nil {
		return domain.Entitlement{}, err
	}
	item.StartsAt = startsAt.UTC().Format(time.RFC3339)
	item.LastSyncedAt = lastSyncedAt.UTC().Format(time.RFC3339)
	item.ScopeRef = scopeRef
	if endsAt != nil {
		value := endsAt.UTC().Format(time.RFC3339)
		item.EndsAt = &value
	}
	return item, nil
}

func (r Repository) AcquireIdempotency(ctx context.Context, scope string, idempotencyKey string, requestHash string, ttl time.Duration) (domain.IdempotencyAcquireResult, error) {
	// The same operation scope + idempotency key becomes a durable dedupe slot.
	// We only allow replay when request hash matches.
	const insertQuery = `
		INSERT INTO operation_idempotency_keys (operation_scope, idempotency_key, request_hash, status, expires_at, created_at, updated_at)
		VALUES ($1, $2, $3, 'processing', NOW() + $4::interval, NOW(), NOW())
		ON CONFLICT (operation_scope, idempotency_key) DO NOTHING
	`
	ttlValue := fmt.Sprintf("%f seconds", ttl.Seconds())
	tag, err := r.pool.Exec(ctx, insertQuery, scope, idempotencyKey, requestHash, ttlValue)
	if err != nil {
		return domain.IdempotencyAcquireResult{}, fmt.Errorf("acquire idempotency: %w", err)
	}
	if tag.RowsAffected() == 1 {
		return domain.IdempotencyAcquireResult{State: "acquired"}, nil
	}

	const selectQuery = `
		SELECT request_hash, status, response_payload
		FROM operation_idempotency_keys
		WHERE operation_scope = $1 AND idempotency_key = $2
	`
	var existingHash string
	var status string
	var responsePayload []byte
	if err := r.pool.QueryRow(ctx, selectQuery, scope, idempotencyKey).Scan(&existingHash, &status, &responsePayload); err != nil {
		return domain.IdempotencyAcquireResult{}, fmt.Errorf("load idempotency record: %w", err)
	}
	if existingHash != requestHash {
		return domain.IdempotencyAcquireResult{State: "conflict"}, nil
	}
	switch status {
	case "succeeded":
		return domain.IdempotencyAcquireResult{State: "replay", CachedResponse: responsePayload}, nil
	case "failed":
		const retryQuery = `
			UPDATE operation_idempotency_keys
			SET status = 'processing', error_payload = NULL, updated_at = NOW(), expires_at = NOW() + $4::interval
			WHERE operation_scope = $1 AND idempotency_key = $2 AND request_hash = $3 AND status = 'failed'
		`
		retryTag, err := r.pool.Exec(ctx, retryQuery, scope, idempotencyKey, requestHash, ttlValue)
		if err != nil {
			return domain.IdempotencyAcquireResult{}, fmt.Errorf("retry failed idempotency: %w", err)
		}
		if retryTag.RowsAffected() == 1 {
			return domain.IdempotencyAcquireResult{State: "acquired"}, nil
		}
	}
	return domain.IdempotencyAcquireResult{State: "in_progress"}, nil
}

func (r Repository) MarkIdempotencySucceeded(ctx context.Context, scope string, idempotencyKey string, response any) error {
	payload, err := json.Marshal(response)
	if err != nil {
		return fmt.Errorf("marshal idempotency response: %w", err)
	}
	const query = `
		UPDATE operation_idempotency_keys
		SET status = 'succeeded', response_payload = $3, error_payload = NULL, updated_at = NOW()
		WHERE operation_scope = $1 AND idempotency_key = $2
	`
	_, err = r.pool.Exec(ctx, query, scope, idempotencyKey, payload)
	if err != nil {
		return fmt.Errorf("mark idempotency succeeded: %w", err)
	}
	return nil
}

func (r Repository) MarkIdempotencyFailed(ctx context.Context, scope string, idempotencyKey string, appErr apperrors.AppError) error {
	payload, err := json.Marshal(map[string]any{
		"code":    appErr.Code,
		"message": appErr.Message,
		"details": appErr.Details,
	})
	if err != nil {
		return fmt.Errorf("marshal idempotency error payload: %w", err)
	}
	const query = `
		UPDATE operation_idempotency_keys
		SET status = 'failed', error_payload = $3, updated_at = NOW()
		WHERE operation_scope = $1 AND idempotency_key = $2
	`
	_, err = r.pool.Exec(ctx, query, scope, idempotencyKey, payload)
	if err != nil {
		return fmt.Errorf("mark idempotency failed: %w", err)
	}
	return nil
}
