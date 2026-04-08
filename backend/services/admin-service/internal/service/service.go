package service

import (
	"context"
	"log"
	"time"

	"dramaflow/backend/shared/config"
	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/services/admin-service/internal/audit"
	adminauth "dramaflow/backend/services/admin-service/internal/auth"
	"dramaflow/backend/services/admin-service/internal/domain"
	"dramaflow/backend/services/admin-service/internal/rbac"
)

type Service struct {
	repo              adminRepository
	auditWriter       audit.MetricsWriter
	billingClient     billingClient
	entitlementClient entitlementClient
	cfg               config.Config
}

type billingClient interface {
	ResyncPurchase(ctx context.Context, purchaseToken string) error
	ReplayRTDN(ctx context.Context, payload map[string]any) error
}

type entitlementClient interface {
	Recompute(ctx context.Context, userID string) error
	Grant(ctx context.Context, payload map[string]any) error
	Revoke(ctx context.Context, payload map[string]any) error
}

type adminRepository interface {
	EnsureBootstrapAdmin(ctx context.Context, email string, passwordHash string) error
	FindAdminByEmail(ctx context.Context, email string) (domain.AdminUser, string, error)
	CreateSession(ctx context.Context, adminUserID string, sessionHash string, expiresAt time.Time) error
	TouchAdminLogin(ctx context.Context, adminUserID string) error
	FindAdminBySessionHash(ctx context.Context, sessionHash string) (domain.AdminUser, error)
	RevokeSession(ctx context.Context, sessionHash string) error
	DashboardStats(ctx context.Context) (domain.DashboardStats, error)
	ListDramas(ctx context.Context, search string, status string, page int, pageSize int) (domain.DramaListResponse, error)
	CreateDrama(ctx context.Context, request domain.DramaMutationRequest) (domain.DramaRecord, error)
	GetDrama(ctx context.Context, dramaID string) (domain.DramaRecord, error)
	UpdateDrama(ctx context.Context, dramaID string, request domain.DramaMutationRequest) (domain.DramaRecord, error)
	ListEpisodes(ctx context.Context, dramaID string) ([]domain.EpisodeRecord, error)
	CreateEpisode(ctx context.Context, dramaID string, request domain.EpisodeMutationRequest) (domain.EpisodeRecord, error)
	GetEpisode(ctx context.Context, episodeID string) (domain.EpisodeRecord, error)
	UpdateEpisode(ctx context.Context, episodeID string, request domain.EpisodeMutationRequest) (domain.EpisodeRecord, error)
	SetEpisodePublishStatus(ctx context.Context, episodeID string, status string) error
	GetFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error)
	SaveFeedDraft(ctx context.Context, request domain.FeedConfigMutationRequest) (domain.FeedHomeConfig, error)
	PublishFeedConfig(ctx context.Context, region string, language string, actorID string) (domain.FeedHomeConfig, error)
	RollbackFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error)
	ListUsers(ctx context.Context, query string, page int, pageSize int) (domain.UserSummaryResponse, error)
	ListPurchases(ctx context.Context, query string, page int, pageSize int) (domain.PurchaseSearchResponse, error)
	GetPurchase(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error)
	ListEntitlements(ctx context.Context, userID string, page int, pageSize int) (domain.EntitlementSearchResponse, error)
	ListRTDNEvents(ctx context.Context, query string, page int, pageSize int) (domain.RTDNEventResponse, error)
	GetRTDNEvent(ctx context.Context, messageID string) (domain.RTDNEventRecord, error)
	ListPlaybackSessions(ctx context.Context, query string, page int, pageSize int) (domain.PlaybackSessionResponse, error)
	GetPlaybackSession(ctx context.Context, sessionID string) (domain.PlaybackSessionRecord, error)
	ListAuditLogs(ctx context.Context, query string, page int, pageSize int) (domain.AuditLogResponse, error)
}

func New(repo adminRepository, auditWriter audit.MetricsWriter, billingClient billingClient, entitlementClient entitlementClient, cfg config.Config) Service {
	return Service{
		repo:              repo,
		auditWriter:       auditWriter,
		billingClient:     billingClient,
		entitlementClient: entitlementClient,
		cfg:               cfg,
	}
}

func (s Service) Bootstrap(ctx context.Context) error {
	hash, err := adminauth.HashPassword(s.cfg.Admin.BootstrapPassword)
	if err != nil {
		return err
	}
	return s.repo.EnsureBootstrapAdmin(ctx, s.cfg.Admin.BootstrapEmail, hash)
}

func (s Service) Login(ctx context.Context, request domain.AuthLoginRequest) (domain.AuthLoginResponse, string, error) {
	user, passwordHash, err := s.repo.FindAdminByEmail(ctx, request.Email)
	if err != nil {
		return domain.AuthLoginResponse{}, "", err
	}
	if user.Status != "active" {
		return domain.AuthLoginResponse{}, "", apperrors.ErrForbidden
	}
	if err := adminauth.VerifyPassword(passwordHash, request.Password); err != nil {
		return domain.AuthLoginResponse{}, "", apperrors.ErrUnauthorized
	}
	rawToken, sessionHash, expiresAt := adminauth.NewSessionToken(user.ID)
	if err := s.repo.CreateSession(ctx, user.ID, sessionHash, expiresAt); err != nil {
		return domain.AuthLoginResponse{}, "", err
	}
	if err := s.repo.TouchAdminLogin(ctx, user.ID); err != nil {
		return domain.AuthLoginResponse{}, "", err
	}
	return domain.AuthLoginResponse{
		AdminUser:   user,
		Permissions: rbac.Permissions(user.Role),
		ExpiresAt:   expiresAt.UTC().Format(time.RFC3339),
	}, rawToken, nil
}

func (s Service) ResolveSessionHash(ctx context.Context, sessionHash string) (domain.AdminUser, error) {
	return s.repo.FindAdminBySessionHash(ctx, sessionHash)
}

func (s Service) RevokeSessionHash(ctx context.Context, sessionHash string) error {
	return s.repo.RevokeSession(ctx, sessionHash)
}

func (s Service) Dashboard(ctx context.Context) (domain.DashboardStats, error) {
	return s.repo.DashboardStats(ctx)
}

func (s Service) ListDramas(ctx context.Context, search string, status string, page int, pageSize int) (domain.DramaListResponse, error) {
	return s.repo.ListDramas(ctx, search, status, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) CreateDrama(ctx context.Context, request domain.DramaMutationRequest, actor domain.AdminUser, requestID string, traceID string) (domain.DramaRecord, error) {
	item, err := s.repo.CreateDrama(ctx, request)
	s.writeAudit(ctx, actor, "drama.create", "drama", item.ID, requestID, traceID, nil, map[string]any{"drama": item}, err)
	return item, err
}

func (s Service) GetDrama(ctx context.Context, dramaID string) (domain.DramaRecord, error) {
	return s.repo.GetDrama(ctx, dramaID)
}

func (s Service) UpdateDrama(ctx context.Context, dramaID string, request domain.DramaMutationRequest, actor domain.AdminUser, requestID string, traceID string) (domain.DramaRecord, error) {
	before, _ := s.repo.GetDrama(ctx, dramaID)
	item, err := s.repo.UpdateDrama(ctx, dramaID, request)
	s.writeAudit(ctx, actor, "drama.update", "drama", dramaID, requestID, traceID, map[string]any{"drama": before}, map[string]any{"drama": item}, err)
	return item, err
}

func (s Service) ListEpisodes(ctx context.Context, dramaID string) ([]domain.EpisodeRecord, error) {
	return s.repo.ListEpisodes(ctx, dramaID)
}

func (s Service) CreateEpisode(ctx context.Context, dramaID string, request domain.EpisodeMutationRequest, actor domain.AdminUser, requestID string, traceID string) (domain.EpisodeRecord, error) {
	item, err := s.repo.CreateEpisode(ctx, dramaID, request)
	s.writeAudit(ctx, actor, "episode.create", "episode", item.ID, requestID, traceID, nil, map[string]any{"episode": item}, err)
	return item, err
}

func (s Service) UpdateEpisode(ctx context.Context, episodeID string, request domain.EpisodeMutationRequest, actor domain.AdminUser, requestID string, traceID string) (domain.EpisodeRecord, error) {
	before, _ := s.repo.GetEpisode(ctx, episodeID)
	item, err := s.repo.UpdateEpisode(ctx, episodeID, request)
	s.writeAudit(ctx, actor, "episode.update", "episode", episodeID, requestID, traceID, map[string]any{"episode": before}, map[string]any{"episode": item}, err)
	return item, err
}

func (s Service) PublishEpisode(ctx context.Context, episodeID string, actor domain.AdminUser, requestID string, traceID string) error {
	before, _ := s.repo.GetEpisode(ctx, episodeID)
	err := s.repo.SetEpisodePublishStatus(ctx, episodeID, "published")
	after, _ := s.repo.GetEpisode(ctx, episodeID)
	s.writeAudit(ctx, actor, "episode.publish", "episode", episodeID, requestID, traceID, map[string]any{"episode": before}, map[string]any{"episode": after}, err)
	return err
}

func (s Service) UnpublishEpisode(ctx context.Context, episodeID string, actor domain.AdminUser, requestID string, traceID string) error {
	before, _ := s.repo.GetEpisode(ctx, episodeID)
	err := s.repo.SetEpisodePublishStatus(ctx, episodeID, "draft")
	after, _ := s.repo.GetEpisode(ctx, episodeID)
	s.writeAudit(ctx, actor, "episode.unpublish", "episode", episodeID, requestID, traceID, map[string]any{"episode": before}, map[string]any{"episode": after}, err)
	return err
}

func (s Service) GetFeedConfig(ctx context.Context, region string, language string) (domain.FeedHomeConfig, error) {
	item, err := s.repo.GetFeedConfig(ctx, region, language)
	if err == nil {
		return item, nil
	}
	if appErr, ok := err.(apperrors.AppError); ok && appErr.Code == apperrors.ErrNotFound.Code {
		// 2026-04-08:
		// 这里故意不再把“配置不存在”直接往上抛 404。
		//
		// 这个调整解决的是一个非常常见、也很影响运营体验的问题：
		// 1. 新环境第一次打开“首页推荐配置”时，数据库里大概率还没有 feed_home_configs 记录。
		// 2. 老逻辑直接返回 404，前端页面会误以为接口坏了。
		// 3. 但真实业务诉求不是报错，而是先给运营一个可编辑的空草稿，让她保存第一版配置。
		//
		// 所以这里改成更适合生产的兜底策略：
		// - 读不到配置时，先返回一个内存态默认空配置
		// - 等用户点击“保存草稿”时，再由 SaveFeedDraft 真正落库
		//
		// 这套做法在大流量项目里更稳，原因也很实际：
		// - 首次初始化不依赖人工提前灌库
		// - 页面首次进入不报错，运营和测试同学更容易确认链路
		// - 真正写库只发生在显式保存时，边界清楚，审计也清楚
		return domain.FeedHomeConfig{
			Region:           region,
			Language:         language,
			ConfigVersion:    0,
			DraftPayload:     map[string]any{"featured": []any{}, "trending": []any{}, "recommended": []any{}, "banners": []any{}},
			PublishedPayload: map[string]any{},
		}, nil
	}
	return domain.FeedHomeConfig{}, err
}

func (s Service) SaveFeedDraft(ctx context.Context, request domain.FeedConfigMutationRequest, actor domain.AdminUser, requestID string, traceID string) (domain.FeedHomeConfig, error) {
	before, _ := s.repo.GetFeedConfig(ctx, request.Region, request.Language)
	item, err := s.repo.SaveFeedDraft(ctx, request)
	s.writeAudit(ctx, actor, "feed_config.save_draft", "feed_home_config", request.Region+":"+request.Language, requestID, traceID, map[string]any{"config": before}, map[string]any{"config": item}, err)
	return item, err
}

func (s Service) PublishFeed(ctx context.Context, region string, language string, actor domain.AdminUser, requestID string, traceID string) (domain.FeedHomeConfig, error) {
	before, _ := s.repo.GetFeedConfig(ctx, region, language)
	item, err := s.repo.PublishFeedConfig(ctx, region, language, actor.ID)
	s.writeAudit(ctx, actor, "feed_config.publish", "feed_home_config", region+":"+language, requestID, traceID, map[string]any{"config": before}, map[string]any{"config": item}, err)
	return item, err
}

func (s Service) RollbackFeed(ctx context.Context, region string, language string, actor domain.AdminUser, requestID string, traceID string) (domain.FeedHomeConfig, error) {
	before, _ := s.repo.GetFeedConfig(ctx, region, language)
	item, err := s.repo.RollbackFeedConfig(ctx, region, language)
	s.writeAudit(ctx, actor, "feed_config.rollback", "feed_home_config", region+":"+language, requestID, traceID, map[string]any{"config": before}, map[string]any{"config": item}, err)
	return item, err
}

func (s Service) ListUsers(ctx context.Context, query string, page int, pageSize int) (domain.UserSummaryResponse, error) {
	return s.repo.ListUsers(ctx, query, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) GetUser(ctx context.Context, userID string) (domain.UserSummaryResponse, error) {
	return s.repo.ListUsers(ctx, userID, 1, 10)
}

func (s Service) ListPurchases(ctx context.Context, query string, page int, pageSize int) (domain.PurchaseSearchResponse, error) {
	return s.repo.ListPurchases(ctx, query, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) GetPurchase(ctx context.Context, purchaseToken string) (domain.PurchaseRecord, error) {
	return s.repo.GetPurchase(ctx, purchaseToken)
}

func (s Service) ResyncPurchase(ctx context.Context, purchaseToken string, actor domain.AdminUser, requestID string, traceID string) error {
	before, _ := s.repo.GetPurchase(ctx, purchaseToken)
	err := s.billingClient.ResyncPurchase(ctx, purchaseToken)
	after, _ := s.repo.GetPurchase(ctx, purchaseToken)
	s.writeAudit(ctx, actor, "purchase.resync", "purchase", purchaseToken, requestID, traceID, map[string]any{"purchase": before}, map[string]any{"purchase": after}, err)
	return err
}

func (s Service) ListEntitlements(ctx context.Context, userID string, page int, pageSize int) (domain.EntitlementSearchResponse, error) {
	return s.repo.ListEntitlements(ctx, userID, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) GetUserEntitlements(ctx context.Context, userID string) (domain.EntitlementSearchResponse, error) {
	return s.repo.ListEntitlements(ctx, userID, 1, 50)
}

func (s Service) RecomputeEntitlements(ctx context.Context, userID string, actor domain.AdminUser, requestID string, traceID string) error {
	before, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	err := s.entitlementClient.Recompute(ctx, userID)
	after, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	s.writeAudit(ctx, actor, "entitlement.recompute", "entitlement", userID, requestID, traceID, map[string]any{"entitlements": before.Items}, map[string]any{"entitlements": after.Items}, err)
	return err
}

func (s Service) GrantTempEntitlement(ctx context.Context, userID string, request domain.GrantTempRequest, actor domain.AdminUser, requestID string, traceID string) error {
	if !rbac.Allowed(actor.Role, rbac.PermEntitlementGrantTemp) {
		return apperrors.ErrForbidden
	}
	before, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	err := s.entitlementClient.Grant(ctx, map[string]any{
		"userId":              userID,
		"entitlementType":     "subscription",
		"productId":           request.ProductID,
		"scopeType":           "global",
		"state":               "active",
		"startsAt":            time.Now().UTC().Format(time.RFC3339),
		"endsAt":              request.EndsAt,
		"sourcePurchaseToken": "admin-temp-" + userID,
		"reason":              request.Reason,
		"payloadSnapshot":     map[string]any{"issuedBy": actor.Email, "mode": "temporary_admin_grant"},
	})
	after, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	s.writeAudit(ctx, actor, "entitlement.grant_temp", "entitlement", userID, requestID, traceID, map[string]any{"entitlements": before.Items}, map[string]any{"entitlements": after.Items}, err)
	return err
}

func (s Service) RevokeEntitlement(ctx context.Context, userID string, request domain.RevokeEntitlementRequest, actor domain.AdminUser, requestID string, traceID string) error {
	if !rbac.Allowed(actor.Role, rbac.PermEntitlementRevoke) {
		return apperrors.ErrForbidden
	}
	before, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	err := s.entitlementClient.Revoke(ctx, map[string]any{
		"userId":              userID,
		"sourcePurchaseToken": request.SourcePurchaseToken,
		"state":               "revoked",
		"reason":              request.Reason,
		"payloadSnapshot":     map[string]any{"issuedBy": actor.Email},
	})
	after, _ := s.repo.ListEntitlements(ctx, userID, 1, 50)
	s.writeAudit(ctx, actor, "entitlement.revoke", "entitlement", userID, requestID, traceID, map[string]any{"entitlements": before.Items}, map[string]any{"entitlements": after.Items}, err)
	return err
}

func (s Service) ListRTDNEvents(ctx context.Context, query string, page int, pageSize int) (domain.RTDNEventResponse, error) {
	return s.repo.ListRTDNEvents(ctx, query, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) ReplayRTDN(ctx context.Context, messageID string, actor domain.AdminUser, requestID string, traceID string) error {
	event, err := s.repo.GetRTDNEvent(ctx, messageID)
	if err != nil {
		return err
	}
	err = s.billingClient.ReplayRTDN(ctx, event.PayloadSnapshot)
	s.writeAudit(ctx, actor, "rtdn.replay", "rtdn_event", messageID, requestID, traceID, nil, map[string]any{"event": event}, err)
	return err
}

func (s Service) ListPlaybackSessions(ctx context.Context, query string, page int, pageSize int) (domain.PlaybackSessionResponse, error) {
	return s.repo.ListPlaybackSessions(ctx, query, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) GetPlaybackSession(ctx context.Context, sessionID string) (domain.PlaybackSessionRecord, error) {
	return s.repo.GetPlaybackSession(ctx, sessionID)
}

func (s Service) ListAuditLogs(ctx context.Context, query string, page int, pageSize int) (domain.AuditLogResponse, error) {
	return s.repo.ListAuditLogs(ctx, query, normalizePage(page), normalizePageSize(pageSize))
}

func (s Service) writeAudit(ctx context.Context, actor domain.AdminUser, action string, resourceType string, resourceID string, requestID string, traceID string, before map[string]any, after map[string]any, err error) {
	success := err == nil
	log.Printf("admin action admin_user_id=%s role=%s action=%s resource_type=%s resource_id=%s trace_id=%s request_id=%s success=%t", actor.ID, actor.Role, action, resourceType, resourceID, traceID, requestID, success)
	_ = s.auditWriter.Write(ctx, domain.AuditLogRecord{
		AdminUserID:    actor.ID,
		Action:         action,
		ResourceType:   resourceType,
		ResourceID:     resourceID,
		RequestID:      requestID,
		TraceID:        traceID,
		Success:        success,
		BeforeSnapshot: before,
		AfterSnapshot:  after,
	})
}

func normalizePage(page int) int {
	if page <= 0 {
		return 1
	}
	return page
}

func normalizePageSize(pageSize int) int {
	if pageSize <= 0 || pageSize > 100 {
		return 20
	}
	return pageSize
}
