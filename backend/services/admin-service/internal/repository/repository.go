package repository

import (
	"context"
	"encoding/json"
	"errors"
	"time"

	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/admin-service/internal/domain"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) EnsureBootstrapAdmin(ctx context.Context, email string, passwordHash string) error {
	const query = `
		INSERT INTO admin_users (id, email, password_hash, display_name, role, status, created_at, updated_at)
		VALUES ($1, $2, $3, 'Bootstrap Admin', 'super_admin', 'active', NOW(), NOW())
		ON CONFLICT (email) DO NOTHING
	`
	_, err := r.pool.Exec(ctx, query, "admin-bootstrap", email, passwordHash)
	return err
}

func (r Repository) FindAdminByEmail(ctx context.Context, email string) (domain.AdminUser, string, error) {
	const query = `
		SELECT id, email, display_name, role, status, last_login_at, password_hash
		FROM admin_users
		WHERE email = $1
	`
	var item domain.AdminUser
	var passwordHash string
	var lastLoginAt *time.Time
	err := r.pool.QueryRow(ctx, query, email).Scan(&item.ID, &item.Email, &item.DisplayName, &item.Role, &item.Status, &lastLoginAt, &passwordHash)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.AdminUser{}, "", apperrors.ErrUnauthorized
		}
		return domain.AdminUser{}, "", err
	}
	if lastLoginAt != nil {
		value := lastLoginAt.UTC().Format(time.RFC3339)
		item.LastLoginAt = &value
	}
	return item, passwordHash, nil
}

func (r Repository) FindAdminBySessionHash(ctx context.Context, sessionHash string) (domain.AdminUser, error) {
	const query = `
		SELECT u.id, u.email, u.display_name, u.role, u.status, u.last_login_at
		FROM admin_sessions s
		JOIN admin_users u ON u.id = s.admin_user_id
		WHERE s.session_token_hash = $1
		  AND s.revoked_at IS NULL
		  AND s.expires_at > NOW()
	`
	var item domain.AdminUser
	var lastLoginAt *time.Time
	err := r.pool.QueryRow(ctx, query, sessionHash).Scan(&item.ID, &item.Email, &item.DisplayName, &item.Role, &item.Status, &lastLoginAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.AdminUser{}, apperrors.ErrUnauthorized
		}
		return domain.AdminUser{}, err
	}
	if lastLoginAt != nil {
		value := lastLoginAt.UTC().Format(time.RFC3339)
		item.LastLoginAt = &value
	}
	return item, nil
}

func (r Repository) CreateSession(ctx context.Context, adminUserID string, sessionHash string, expiresAt time.Time) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO admin_sessions (id, admin_user_id, session_token_hash, expires_at, created_at)
		VALUES ($1, $2, $3, $4, NOW())
	`, uuid.NewString(), adminUserID, sessionHash, expiresAt)
	return err
}

func (r Repository) RevokeSession(ctx context.Context, sessionHash string) error {
	_, err := r.pool.Exec(ctx, `UPDATE admin_sessions SET revoked_at = NOW() WHERE session_token_hash = $1`, sessionHash)
	return err
}

func (r Repository) TouchAdminLogin(ctx context.Context, adminUserID string) error {
	_, err := r.pool.Exec(ctx, `UPDATE admin_users SET last_login_at = NOW(), updated_at = NOW() WHERE id = $1`, adminUserID)
	return err
}

func (r Repository) ListDramas(ctx context.Context, search string, status string, page int, pageSize int) (domain.DramaListResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured
		FROM dramas
		WHERE ($1 = '' OR title ILIKE '%' || $1 || '%')
		  AND ($2 = '' OR publish_status = $2)
		ORDER BY updated_at DESC
		LIMIT $3 OFFSET $4
	`, search, status, pageSize, offset)
	if err != nil {
		return domain.DramaListResponse{}, err
	}
	defer rows.Close()

	items := make([]domain.DramaRecord, 0)
	for rows.Next() {
		var item domain.DramaRecord
		var rawTags []byte
		if err := rows.Scan(&item.ID, &item.Title, &item.ShortDescription, &item.LongDescription, &item.PosterURL, &item.CoverURL, &rawTags, &item.Region, &item.Language, &item.PublishStatus, &item.IsFeatured); err != nil {
			return domain.DramaListResponse{}, err
		}
		_ = json.Unmarshal(rawTags, &item.Tags)
		items = append(items, item)
	}

	var total int
	if err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM dramas
		WHERE ($1 = '' OR title ILIKE '%' || $1 || '%')
		  AND ($2 = '' OR publish_status = $2)
	`, search, status).Scan(&total); err != nil {
		return domain.DramaListResponse{}, err
	}

	return domain.DramaListResponse{
		Items: items,
		Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total},
	}, nil
}

func (r Repository) CreateDrama(ctx context.Context, req domain.DramaMutationRequest) (domain.DramaRecord, error) {
	item := domain.DramaRecord{ID: uuid.NewString(), DramaMutationRequest: req}
	rawTags, _ := json.Marshal(req.Tags)
	_, err := r.pool.Exec(ctx, `
		INSERT INTO dramas (id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured, created_at, updated_at)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW(),NOW())
	`, item.ID, item.Title, item.ShortDescription, item.LongDescription, item.PosterURL, item.CoverURL, rawTags, item.Region, item.Language, item.PublishStatus, item.IsFeatured)
	return item, err
}

func (r Repository) GetDrama(ctx context.Context, dramaID string) (domain.DramaRecord, error) {
	const query = `
		SELECT id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured
		FROM dramas
		WHERE id = $1
	`
	var item domain.DramaRecord
	var rawTags []byte
	err := r.pool.QueryRow(ctx, query, dramaID).Scan(&item.ID, &item.Title, &item.ShortDescription, &item.LongDescription, &item.PosterURL, &item.CoverURL, &rawTags, &item.Region, &item.Language, &item.PublishStatus, &item.IsFeatured)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.DramaRecord{}, apperrors.ErrNotFound
		}
		return domain.DramaRecord{}, err
	}
	_ = json.Unmarshal(rawTags, &item.Tags)
	return item, nil
}

func (r Repository) UpdateDrama(ctx context.Context, dramaID string, req domain.DramaMutationRequest) (domain.DramaRecord, error) {
	rawTags, _ := json.Marshal(req.Tags)
	result, err := r.pool.Exec(ctx, `
		UPDATE dramas
		SET title = $2, short_description = $3, long_description = $4, poster_url = $5, cover_url = $6, tags = $7, region = $8, language = $9, publish_status = $10, is_featured = $11, updated_at = NOW()
		WHERE id = $1
	`, dramaID, req.Title, req.ShortDescription, req.LongDescription, req.PosterURL, req.CoverURL, rawTags, req.Region, req.Language, req.PublishStatus, req.IsFeatured)
	if err != nil {
		return domain.DramaRecord{}, err
	}
	if result.RowsAffected() == 0 {
		return domain.DramaRecord{}, apperrors.ErrNotFound
	}
	return r.GetDrama(ctx, dramaID)
}

func (r Repository) ListEpisodes(ctx context.Context, dramaID string) ([]domain.EpisodeRecord, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, drama_id, title, description, episode_no, duration_seconds, preview_seconds, is_premium, publish_status, sort_order, stream_key_placeholder
		FROM episodes
		WHERE drama_id = $1
		ORDER BY sort_order ASC, episode_no ASC
	`, dramaID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]domain.EpisodeRecord, 0)
	for rows.Next() {
		var item domain.EpisodeRecord
		if err := rows.Scan(&item.ID, &item.DramaID, &item.Title, &item.Description, &item.EpisodeNo, &item.DurationSeconds, &item.PreviewSeconds, &item.IsPremium, &item.PublishStatus, &item.SortOrder, &item.StreamKeyPlaceholder); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r Repository) CreateEpisode(ctx context.Context, dramaID string, req domain.EpisodeMutationRequest) (domain.EpisodeRecord, error) {
	item := domain.EpisodeRecord{ID: uuid.NewString(), DramaID: dramaID, EpisodeMutationRequest: req}
	_, err := r.pool.Exec(ctx, `
		INSERT INTO episodes (id, drama_id, episode_no, title, description, duration_seconds, preview_seconds, is_premium, stream_key_placeholder, publish_status, sort_order, created_at, updated_at)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW(),NOW())
	`, item.ID, item.DramaID, item.EpisodeNo, item.Title, item.Description, item.DurationSeconds, item.PreviewSeconds, item.IsPremium, item.StreamKeyPlaceholder, item.PublishStatus, item.SortOrder)
	return item, err
}

func (r Repository) GetEpisode(ctx context.Context, episodeID string) (domain.EpisodeRecord, error) {
	var item domain.EpisodeRecord
	err := r.pool.QueryRow(ctx, `
		SELECT id, drama_id, title, description, episode_no, duration_seconds, preview_seconds, is_premium, publish_status, sort_order, stream_key_placeholder
		FROM episodes WHERE id = $1
	`, episodeID).Scan(&item.ID, &item.DramaID, &item.Title, &item.Description, &item.EpisodeNo, &item.DurationSeconds, &item.PreviewSeconds, &item.IsPremium, &item.PublishStatus, &item.SortOrder, &item.StreamKeyPlaceholder)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.EpisodeRecord{}, apperrors.ErrNotFound
		}
		return domain.EpisodeRecord{}, err
	}
	return item, nil
}

func (r Repository) UpdateEpisode(ctx context.Context, episodeID string, req domain.EpisodeMutationRequest) (domain.EpisodeRecord, error) {
	result, err := r.pool.Exec(ctx, `
		UPDATE episodes
		SET title=$2, description=$3, episode_no=$4, duration_seconds=$5, preview_seconds=$6, is_premium=$7, publish_status=$8, sort_order=$9, stream_key_placeholder=$10, updated_at=NOW()
		WHERE id = $1
	`, episodeID, req.Title, req.Description, req.EpisodeNo, req.DurationSeconds, req.PreviewSeconds, req.IsPremium, req.PublishStatus, req.SortOrder, req.StreamKeyPlaceholder)
	if err != nil {
		return domain.EpisodeRecord{}, err
	}
	if result.RowsAffected() == 0 {
		return domain.EpisodeRecord{}, apperrors.ErrNotFound
	}
	return r.GetEpisode(ctx, episodeID)
}

func (r Repository) SetEpisodePublishStatus(ctx context.Context, episodeID string, publishStatus string) error {
	result, err := r.pool.Exec(ctx, `UPDATE episodes SET publish_status = $2, updated_at = NOW() WHERE id = $1`, episodeID, publishStatus)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return apperrors.ErrNotFound
	}
	return nil
}

func (r Repository) GetFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error) {
	var item domain.FeedHomeConfig
	var draftRaw []byte
	var publishedRaw []byte
	var publishedAt *time.Time
	var publishedBy *string
	err := r.pool.QueryRow(ctx, `
		SELECT id, region, language, config_version, draft_payload, published_payload, published_at, published_by
		FROM feed_home_configs
		WHERE region = $1 AND language = $2
	`, region, language).Scan(&item.ID, &item.Region, &item.Language, &item.ConfigVersion, &draftRaw, &publishedRaw, &publishedAt, &publishedBy)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.FeedHomeConfig{}, apperrors.ErrNotFound
		}
		return domain.FeedHomeConfig{}, err
	}
	_ = json.Unmarshal(draftRaw, &item.DraftPayload)
	_ = json.Unmarshal(publishedRaw, &item.PublishedPayload)
	if publishedAt != nil {
		value := publishedAt.UTC().Format(time.RFC3339)
		item.PublishedAt = &value
	}
	item.PublishedBy = publishedBy
	return item, nil
}

func (r Repository) SaveFeedDraft(ctx context.Context, req domain.FeedConfigMutationRequest) (domain.FeedHomeConfig, error) {
	payload, _ := json.Marshal(req.DraftPayload)
	_, err := r.GetFeedConfig(ctx, req.Region, req.Language)
	if err != nil {
		if appErr, ok := err.(apperrors.AppError); ok && appErr.Code == apperrors.ErrNotFound.Code {
			_, err = r.pool.Exec(ctx, `
				INSERT INTO feed_home_configs (id, region, language, config_version, draft_payload, published_payload, created_at, updated_at)
				VALUES ($1, $2, $3, 1, $4, '{}'::jsonb, NOW(), NOW())
			`, uuid.NewString(), req.Region, req.Language, payload)
			if err != nil {
				return domain.FeedHomeConfig{}, err
			}
			return r.GetFeedConfig(ctx, req.Region, req.Language)
		}
		return domain.FeedHomeConfig{}, err
	}
	_, err = r.pool.Exec(ctx, `
		UPDATE feed_home_configs
		SET draft_payload = $3, updated_at = NOW()
		WHERE region = $1 AND language = $2
	`, req.Region, req.Language, payload)
	if err != nil {
		return domain.FeedHomeConfig{}, err
	}
	return r.GetFeedConfig(ctx, req.Region, req.Language)
}

func (r Repository) PublishFeedConfig(ctx context.Context, region string, language string, adminUserID string) (domain.FeedHomeConfig, error) {
	result, err := r.pool.Exec(ctx, `
		UPDATE feed_home_configs
		SET published_payload = draft_payload, config_version = config_version + 1, published_at = NOW(), published_by = $3, updated_at = NOW()
		WHERE region = $1 AND language = $2
	`, region, language, adminUserID)
	if err != nil {
		return domain.FeedHomeConfig{}, err
	}
	if result.RowsAffected() == 0 {
		return domain.FeedHomeConfig{}, apperrors.ErrNotFound
	}
	return r.GetFeedConfig(ctx, region, language)
}

func (r Repository) RollbackFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error) {
	result, err := r.pool.Exec(ctx, `
		UPDATE feed_home_configs
		SET draft_payload = published_payload, updated_at = NOW()
		WHERE region = $1 AND language = $2
	`, region, language)
	if err != nil {
		return domain.FeedHomeConfig{}, err
	}
	if result.RowsAffected() == 0 {
		return domain.FeedHomeConfig{}, apperrors.ErrNotFound
	}
	return r.GetFeedConfig(ctx, region, language)
}

func (r Repository) ListUsers(ctx context.Context, query string, page int, pageSize int) (domain.UserSummaryResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT id, anonymous_device_id, status, created_at
		FROM users
		WHERE ($1 = '' OR id ILIKE '%' || $1 || '%' OR anonymous_device_id ILIKE '%' || $1 || '%')
		ORDER BY created_at DESC
		LIMIT $2 OFFSET $3
	`, query, pageSize, offset)
	if err != nil {
		return domain.UserSummaryResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.UserSummary, 0)
	for rows.Next() {
		var item domain.UserSummary
		var createdAt time.Time
		if err := rows.Scan(&item.ID, &item.AnonymousDeviceID, &item.Status, &createdAt); err != nil {
			return domain.UserSummaryResponse{}, err
		}
		item.CreatedAt = createdAt.UTC().Format(time.RFC3339)
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM users WHERE ($1 = '' OR id ILIKE '%' || $1 || '%' OR anonymous_device_id ILIKE '%' || $1 || '%')`, query).Scan(&total); err != nil {
		return domain.UserSummaryResponse{}, err
	}
	return domain.UserSummaryResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) ListPurchases(ctx context.Context, query string, page int, pageSize int) (domain.PurchaseSearchResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT purchase_token, user_id, product_id, base_plan_id, offer_id, order_id, package_name, purchase_state, acknowledgement_state, start_time, expiry_time, source, raw_payload_snapshot
		FROM billing_purchase_records
		WHERE ($1 = '' OR purchase_token ILIKE '%' || $1 || '%' OR COALESCE(order_id, '') ILIKE '%' || $1 || '%' OR product_id ILIKE '%' || $1 || '%' OR user_id ILIKE '%' || $1 || '%')
		ORDER BY updated_at DESC
		LIMIT $2 OFFSET $3
	`, query, pageSize, offset)
	if err != nil {
		return domain.PurchaseSearchResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.PurchaseRecord, 0)
	for rows.Next() {
		var item domain.PurchaseRecord
		var rawPayload []byte
		var startTime *time.Time
		var expiryTime *time.Time
		if err := rows.Scan(&item.PurchaseToken, &item.UserID, &item.ProductID, &item.BasePlanID, &item.OfferID, &item.OrderID, &item.PackageName, &item.PurchaseState, &item.AcknowledgementState, &startTime, &expiryTime, &item.Source, &rawPayload); err != nil {
			return domain.PurchaseSearchResponse{}, err
		}
		item.StartTime = formatTimePtr(startTime)
		item.ExpiryTime = formatTimePtr(expiryTime)
		_ = json.Unmarshal(rawPayload, &item.RawPayload)
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM billing_purchase_records WHERE ($1 = '' OR purchase_token ILIKE '%' || $1 || '%' OR COALESCE(order_id, '') ILIKE '%' || $1 || '%' OR product_id ILIKE '%' || $1 || '%' OR user_id ILIKE '%' || $1 || '%')`, query).Scan(&total); err != nil {
		return domain.PurchaseSearchResponse{}, err
	}
	return domain.PurchaseSearchResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) GetPurchase(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	items, err := r.ListPurchases(ctx, purchaseToken, 1, 1)
	if err != nil {
		return domain.PurchaseRecord{}, err
	}
	if len(items.Items) == 0 {
		return domain.PurchaseRecord{}, apperrors.ErrNotFound
	}
	return items.Items[0], nil
}

func (r Repository) ListEntitlements(ctx context.Context, userID string, page int, pageSize int) (domain.EntitlementSearchResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT entitlement_id, user_id, entitlement_type, product_id, scope_type, scope_ref, state, starts_at, ends_at, source_purchase_token, last_synced_at
		FROM entitlements
		WHERE ($1 = '' OR user_id = $1)
		ORDER BY last_synced_at DESC
		LIMIT $2 OFFSET $3
	`, userID, pageSize, offset)
	if err != nil {
		return domain.EntitlementSearchResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.EntitlementRecord, 0)
	for rows.Next() {
		var item domain.EntitlementRecord
		var startsAt time.Time
		var endsAt *time.Time
		var lastSyncedAt time.Time
		if err := rows.Scan(&item.EntitlementID, &item.UserID, &item.EntitlementType, &item.ProductID, &item.ScopeType, &item.ScopeRef, &item.State, &startsAt, &endsAt, &item.SourcePurchaseToken, &lastSyncedAt); err != nil {
			return domain.EntitlementSearchResponse{}, err
		}
		item.StartsAt = startsAt.UTC().Format(time.RFC3339)
		item.EndsAt = formatTimePtr(endsAt)
		item.LastSyncedAt = lastSyncedAt.UTC().Format(time.RFC3339)
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM entitlements WHERE ($1 = '' OR user_id = $1)`, userID).Scan(&total); err != nil {
		return domain.EntitlementSearchResponse{}, err
	}
	return domain.EntitlementSearchResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) ListRTDNEvents(ctx context.Context, query string, page int, pageSize int) (domain.RTDNEventResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT message_id, package_name, event_type, purchase_token, event_time, processed_state, processed_at, error_message, payload_snapshot
		FROM billing_rtdn_events
		WHERE ($1 = '' OR message_id ILIKE '%' || $1 || '%' OR purchase_token ILIKE '%' || $1 || '%')
		ORDER BY event_time DESC
		LIMIT $2 OFFSET $3
	`, query, pageSize, offset)
	if err != nil {
		return domain.RTDNEventResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.RTDNEventRecord, 0)
	for rows.Next() {
		var item domain.RTDNEventRecord
		var processedAt *time.Time
		var rawPayload []byte
		if err := rows.Scan(&item.MessageID, &item.PackageName, &item.EventType, &item.PurchaseToken, &item.EventTime, &item.ProcessedState, &processedAt, &item.ErrorMessage, &rawPayload); err != nil {
			return domain.RTDNEventResponse{}, err
		}
		if processedAt != nil {
			value := processedAt.UTC().Format(time.RFC3339)
			item.ProcessedAt = &value
		}
		_ = json.Unmarshal(rawPayload, &item.PayloadSnapshot)
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM billing_rtdn_events WHERE ($1 = '' OR message_id ILIKE '%' || $1 || '%' OR purchase_token ILIKE '%' || $1 || '%')`, query).Scan(&total); err != nil {
		return domain.RTDNEventResponse{}, err
	}
	return domain.RTDNEventResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) GetRTDNEvent(ctx context.Context, messageID string) (domain.RTDNEventRecord, error) {
	items, err := r.ListRTDNEvents(ctx, messageID, 1, 1)
	if err != nil {
		return domain.RTDNEventRecord{}, err
	}
	if len(items.Items) == 0 {
		return domain.RTDNEventRecord{}, apperrors.ErrNotFound
	}
	return items.Items[0], nil
}

func (r Repository) ListPlaybackSessions(ctx context.Context, query string, page int, pageSize int) (domain.PlaybackSessionResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT id, user_id, drama_id, episode_id, playback_mode, session_status, issued_at, expires_at, completed_at, client_platform, client_version
		FROM playback_sessions
		WHERE ($1 = '' OR id ILIKE '%' || $1 || '%' OR user_id ILIKE '%' || $1 || '%' OR episode_id ILIKE '%' || $1 || '%')
		ORDER BY created_at DESC
		LIMIT $2 OFFSET $3
	`, query, pageSize, offset)
	if err != nil {
		return domain.PlaybackSessionResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.PlaybackSessionRecord, 0)
	for rows.Next() {
		var item domain.PlaybackSessionRecord
		var issuedAt time.Time
		var expiresAt time.Time
		var completedAt *time.Time
		if err := rows.Scan(&item.SessionID, &item.UserID, &item.DramaID, &item.EpisodeID, &item.PlaybackMode, &item.SessionStatus, &issuedAt, &expiresAt, &completedAt, &item.ClientPlatform, &item.ClientVersion); err != nil {
			return domain.PlaybackSessionResponse{}, err
		}
		item.IssuedAt = issuedAt.UTC().Format(time.RFC3339)
		item.ExpiresAt = expiresAt.UTC().Format(time.RFC3339)
		if completedAt != nil {
			value := completedAt.UTC().Format(time.RFC3339)
			item.CompletedAt = &value
		}
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM playback_sessions WHERE ($1 = '' OR id ILIKE '%' || $1 || '%' OR user_id ILIKE '%' || $1 || '%' OR episode_id ILIKE '%' || $1 || '%')`, query).Scan(&total); err != nil {
		return domain.PlaybackSessionResponse{}, err
	}
	return domain.PlaybackSessionResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) GetPlaybackSession(ctx context.Context, sessionID string) (domain.PlaybackSessionRecord, error) {
	items, err := r.ListPlaybackSessions(ctx, sessionID, 1, 1)
	if err != nil {
		return domain.PlaybackSessionRecord{}, err
	}
	if len(items.Items) == 0 {
		return domain.PlaybackSessionRecord{}, apperrors.ErrNotFound
	}
	return items.Items[0], nil
}

func (r Repository) ListAuditLogs(ctx context.Context, query string, page int, pageSize int) (domain.AuditLogResponse, error) {
	offset := (page - 1) * pageSize
	rows, err := r.pool.Query(ctx, `
		SELECT id, admin_user_id, action, resource_type, resource_id, request_id, trace_id, success, before_snapshot, after_snapshot, metadata, created_at
		FROM admin_audit_logs
		WHERE ($1 = '' OR action ILIKE '%' || $1 || '%' OR resource_type ILIKE '%' || $1 || '%' OR resource_id ILIKE '%' || $1 || '%')
		ORDER BY created_at DESC
		LIMIT $2 OFFSET $3
	`, query, pageSize, offset)
	if err != nil {
		return domain.AuditLogResponse{}, err
	}
	defer rows.Close()
	items := make([]domain.AuditLogRecord, 0)
	for rows.Next() {
		var item domain.AuditLogRecord
		var beforeRaw []byte
		var afterRaw []byte
		var metadataRaw []byte
		var createdAt time.Time
		if err := rows.Scan(&item.ID, &item.AdminUserID, &item.Action, &item.ResourceType, &item.ResourceID, &item.RequestID, &item.TraceID, &item.Success, &beforeRaw, &afterRaw, &metadataRaw, &createdAt); err != nil {
			return domain.AuditLogResponse{}, err
		}
		item.CreatedAt = createdAt.UTC().Format(time.RFC3339)
		_ = json.Unmarshal(beforeRaw, &item.BeforeSnapshot)
		_ = json.Unmarshal(afterRaw, &item.AfterSnapshot)
		_ = json.Unmarshal(metadataRaw, &item.Metadata)
		items = append(items, item)
	}
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM admin_audit_logs WHERE ($1 = '' OR action ILIKE '%' || $1 || '%' OR resource_type ILIKE '%' || $1 || '%' OR resource_id ILIKE '%' || $1 || '%')`, query).Scan(&total); err != nil {
		return domain.AuditLogResponse{}, err
	}
	return domain.AuditLogResponse{Items: items, Pagination: domain.Pagination{Page: page, PageSize: pageSize, Total: total}}, nil
}

func (r Repository) Write(ctx context.Context, item domain.AuditLogRecord) error {
	beforeRaw, _ := json.Marshal(item.BeforeSnapshot)
	afterRaw, _ := json.Marshal(item.AfterSnapshot)
	metadataRaw, _ := json.Marshal(item.Metadata)
	_, err := r.pool.Exec(ctx, `
		INSERT INTO admin_audit_logs (id, admin_user_id, action, resource_type, resource_id, request_id, trace_id, success, before_snapshot, after_snapshot, metadata, created_at)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW())
	`, uuid.NewString(), item.AdminUserID, item.Action, item.ResourceType, item.ResourceID, item.RequestID, item.TraceID, item.Success, beforeRaw, afterRaw, metadataRaw)
	return err
}

func (r Repository) DashboardStats(ctx context.Context) (domain.DashboardStats, error) {
	stats := domain.DashboardStats{}
	checks := []struct {
		target *int
		query  string
	}{
		{&stats.DramaTotal, `SELECT COUNT(*) FROM dramas`},
		{&stats.PublishedDramaTotal, `SELECT COUNT(*) FROM dramas WHERE publish_status = 'published'`},
		{&stats.TodayPurchaseSyncTotal, `SELECT COUNT(*) FROM billing_purchase_records WHERE updated_at >= CURRENT_DATE`},
		{&stats.TodayRTDNEventTotal, `SELECT COUNT(*) FROM billing_rtdn_events WHERE event_time::timestamptz >= CURRENT_DATE`},
		{&stats.ActiveEntitlementTotal, `SELECT COUNT(*) FROM entitlements WHERE state IN ('active', 'grace')`},
		{&stats.Playback24hTotal, `SELECT COUNT(*) FROM playback_sessions WHERE created_at >= NOW() - INTERVAL '24 hours'`},
	}
	for _, check := range checks {
		if err := r.pool.QueryRow(ctx, check.query).Scan(check.target); err != nil {
			return domain.DashboardStats{}, err
		}
	}
	return stats, nil
}

func formatTimePtr(value *time.Time) *string {
	if value == nil {
		return nil
	}
	formatted := value.UTC().Format(time.RFC3339)
	return &formatted
}
