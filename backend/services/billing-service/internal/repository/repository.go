package repository

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"dramaflow/backend/services/billing-service/internal/domain"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) UpsertPurchaseRecord(ctx context.Context, verified domain.VerifiedSubscription, source string) (domain.PurchaseRecord, error) {
	lineItems, _ := json.Marshal(verified.LineItemsRaw)
	rawPayload, _ := json.Marshal(verified.RawPayload)
	externalIdentifiers, _ := json.Marshal(verified.ExternalAccountIdentifiers)
	const query = `
		INSERT INTO billing_purchase_records (
			purchase_token, linked_purchase_token, order_id, user_id, package_name, product_id, base_plan_id, offer_id,
			purchase_state, acknowledgement_state, external_account_identifiers, obfuscated_account_id, obfuscated_profile_id,
			start_time, expiry_time, auto_renew_enabled, cancel_reason, line_items_snapshot, raw_payload_snapshot, raw_api_version,
			source, created_at, updated_at
		) VALUES (
			$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,NOW(),NOW()
		)
		ON CONFLICT (purchase_token) DO UPDATE SET
			linked_purchase_token = EXCLUDED.linked_purchase_token,
			order_id = EXCLUDED.order_id,
			user_id = EXCLUDED.user_id,
			package_name = EXCLUDED.package_name,
			product_id = EXCLUDED.product_id,
			base_plan_id = EXCLUDED.base_plan_id,
			offer_id = EXCLUDED.offer_id,
			purchase_state = EXCLUDED.purchase_state,
			acknowledgement_state = EXCLUDED.acknowledgement_state,
			external_account_identifiers = EXCLUDED.external_account_identifiers,
			obfuscated_account_id = EXCLUDED.obfuscated_account_id,
			obfuscated_profile_id = EXCLUDED.obfuscated_profile_id,
			start_time = EXCLUDED.start_time,
			expiry_time = EXCLUDED.expiry_time,
			auto_renew_enabled = EXCLUDED.auto_renew_enabled,
			cancel_reason = EXCLUDED.cancel_reason,
			line_items_snapshot = EXCLUDED.line_items_snapshot,
			raw_payload_snapshot = EXCLUDED.raw_payload_snapshot,
			raw_api_version = EXCLUDED.raw_api_version,
			source = EXCLUDED.source,
			updated_at = NOW()
		RETURNING purchase_token, linked_purchase_token, order_id, user_id, package_name, product_id, base_plan_id, offer_id,
		          purchase_state, acknowledgement_state, start_time, expiry_time, auto_renew_enabled, cancel_reason, source
	`
	var record domain.PurchaseRecord
	var startTime *time.Time
	var expiryTime *time.Time
	if err := r.pool.QueryRow(
		ctx,
		query,
		verified.PurchaseToken,
		verified.LinkedPurchaseToken,
		verified.OrderID,
		verified.UserID,
		verified.PackageName,
		verified.ProductID,
		verified.BasePlanID,
		verified.OfferID,
		verified.PurchaseState,
		verified.AcknowledgementState,
		externalIdentifiers,
		verified.ObfuscatedAccountID,
		verified.ObfuscatedProfileID,
		verified.StartTime,
		verified.ExpiryTime,
		verified.AutoRenewEnabled,
		verified.CancelReason,
		lineItems,
		rawPayload,
		verified.RawAPIVersion,
		source,
	).Scan(
		&record.PurchaseToken,
		&record.LinkedPurchaseToken,
		&record.OrderID,
		&record.UserID,
		&record.PackageName,
		&record.ProductID,
		&record.BasePlanID,
		&record.OfferID,
		&record.PurchaseState,
		&record.AcknowledgementState,
		&startTime,
		&expiryTime,
		&record.AutoRenewEnabled,
		&record.CancelReason,
		&record.Source,
	); err != nil {
		return domain.PurchaseRecord{}, fmt.Errorf("upsert purchase record: %w", err)
	}
	record.StartTime = formatTimePtr(startTime)
	record.ExpiryTime = formatTimePtr(expiryTime)
	return record, nil
}

func (r Repository) UpsertOrder(ctx context.Context, verified domain.VerifiedSubscription, state string) error {
	const query = `
		INSERT INTO billing_orders (
			billing_order_id, user_id, purchase_token, product_id, amount_micros, currency_code, country_code, platform, state, created_at, updated_at
		) VALUES ($1,$2,$3,$4,$5,$6,$7,'google_play',$8,NOW(),NOW())
		ON CONFLICT (purchase_token, state) DO UPDATE SET
			amount_micros = EXCLUDED.amount_micros,
			currency_code = EXCLUDED.currency_code,
			country_code = EXCLUDED.country_code,
			updated_at = NOW()
	`
	_, err := r.pool.Exec(ctx, query, uuid.NewString(), verified.UserID, verified.PurchaseToken, verified.ProductID, verified.AmountMicros, verified.CurrencyCode, verified.CountryCode, state)
	if err != nil {
		return fmt.Errorf("upsert order: %w", err)
	}
	return nil
}

func (r Repository) InsertRTDNEvent(ctx context.Context, event domain.RtdnDomainEvent) (bool, error) {
	rawPayload, _ := json.Marshal(event.PayloadSnapshot)
	const query = `
		INSERT INTO billing_rtdn_events (
			message_id, package_name, event_type, purchase_token, subscription_notification_type, one_time_product_notification_type,
			event_time, payload_snapshot, processed_state, created_at
		) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,'pending',NOW())
	`
	_, err := r.pool.Exec(ctx, query, event.MessageID, event.PackageName, event.EventType, event.PurchaseToken, event.SubscriptionNotificationType, event.OneTimeProductNotificationType, event.EventTime, rawPayload)
	if err == nil {
		return true, nil
	}
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" {
		return false, nil
	}
	return false, fmt.Errorf("insert rtdn event: %w", err)
}

func (r Repository) MarkRTDNProcessed(ctx context.Context, messageID string) error {
	_, err := r.pool.Exec(ctx, `UPDATE billing_rtdn_events SET processed_state = 'processed', processed_at = NOW(), error_message = NULL WHERE message_id = $1`, messageID)
	if err != nil {
		return fmt.Errorf("mark rtdn processed: %w", err)
	}
	return nil
}

func (r Repository) MarkRTDNError(ctx context.Context, messageID string, errorMessage string) error {
	_, err := r.pool.Exec(ctx, `UPDATE billing_rtdn_events SET processed_state = 'error', processed_at = NOW(), error_message = $2 WHERE message_id = $1`, messageID, errorMessage)
	if err != nil {
		return fmt.Errorf("mark rtdn error: %w", err)
	}
	return nil
}

func (r Repository) GetPurchaseRecord(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	const query = `
		SELECT purchase_token, linked_purchase_token, order_id, user_id, package_name, product_id, base_plan_id, offer_id,
		       purchase_state, acknowledgement_state, start_time, expiry_time, auto_renew_enabled, cancel_reason, source
		FROM billing_purchase_records
		WHERE purchase_token = $1
	`
	var record domain.PurchaseRecord
	var startTime *time.Time
	var expiryTime *time.Time
	err := r.pool.QueryRow(ctx, query, purchaseToken).Scan(
		&record.PurchaseToken,
		&record.LinkedPurchaseToken,
		&record.OrderID,
		&record.UserID,
		&record.PackageName,
		&record.ProductID,
		&record.BasePlanID,
		&record.OfferID,
		&record.PurchaseState,
		&record.AcknowledgementState,
		&startTime,
		&expiryTime,
		&record.AutoRenewEnabled,
		&record.CancelReason,
		&record.Source,
	)
	record.StartTime = formatTimePtr(startTime)
	record.ExpiryTime = formatTimePtr(expiryTime)
	return record, err
}

func (r Repository) GetLatestPurchaseByUser(ctx context.Context, userID string) (domain.PurchaseRecord, error) {
	const query = `
		SELECT purchase_token, linked_purchase_token, order_id, user_id, package_name, product_id, base_plan_id, offer_id,
		       purchase_state, acknowledgement_state, start_time, expiry_time, auto_renew_enabled, cancel_reason, source
		FROM billing_purchase_records
		WHERE user_id = $1
		ORDER BY updated_at DESC
		LIMIT 1
	`
	var record domain.PurchaseRecord
	var startTime *time.Time
	var expiryTime *time.Time
	err := r.pool.QueryRow(ctx, query, userID).Scan(
		&record.PurchaseToken,
		&record.LinkedPurchaseToken,
		&record.OrderID,
		&record.UserID,
		&record.PackageName,
		&record.ProductID,
		&record.BasePlanID,
		&record.OfferID,
		&record.PurchaseState,
		&record.AcknowledgementState,
		&startTime,
		&expiryTime,
		&record.AutoRenewEnabled,
		&record.CancelReason,
		&record.Source,
	)
	record.StartTime = formatTimePtr(startTime)
	record.ExpiryTime = formatTimePtr(expiryTime)
	return record, err
}

func (r Repository) GetCatalogProducts(ctx context.Context) ([]map[string]any, error) {
	rows, err := r.pool.Query(ctx, `SELECT product_id, display_name, benefit_snapshot, active FROM billing_catalog_products WHERE active = TRUE ORDER BY product_id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var items []map[string]any
	for rows.Next() {
		var productID, displayName string
		var benefits []byte
		var active bool
		if err := rows.Scan(&productID, &displayName, &benefits, &active); err != nil {
			return nil, err
		}
		var parsed any
		_ = json.Unmarshal(benefits, &parsed)
		items = append(items, map[string]any{
			"productId":       productID,
			"displayName":     displayName,
			"benefitSnapshot": parsed,
			"active":          active,
		})
	}
	return items, rows.Err()
}

func formatTimePtr(value *time.Time) *string {
	if value == nil {
		return nil
	}
	formatted := value.UTC().Format(time.RFC3339)
	return &formatted
}
