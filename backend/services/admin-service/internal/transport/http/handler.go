package http

import (
	"crypto/sha256"
	"encoding/hex"
	"net/http"
	"strconv"

	apperrors "dramaflow/backend/shared/errors"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	adminauth "dramaflow/backend/services/admin-service/internal/auth"
	"dramaflow/backend/services/admin-service/internal/domain"
	"dramaflow/backend/services/admin-service/internal/rbac"
	"dramaflow/backend/services/admin-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
	metrics sharedtelemetry.Metrics
}

func New(service service.Service, metrics sharedtelemetry.Metrics) Handler {
	return Handler{service: service, metrics: metrics}
}

func (h Handler) RegisterRoutes(v1 *gin.RouterGroup, requireSession gin.HandlerFunc, requirePermission func(string) gin.HandlerFunc) {
	authGroup := v1.Group("/admin/auth")
	authGroup.POST("/login", h.login)
	authGroup.POST("/logout", requireSession, h.logout)
	authGroup.GET("/me", requireSession, h.me)

	admin := v1.Group("/admin")
	admin.Use(requireSession)
	admin.GET("/dashboard", requirePermission(rbac.PermDashboardView), h.dashboard)
	admin.GET("/dramas", requirePermission(rbac.PermDramaRead), h.listDramas)
	admin.POST("/dramas", requirePermission(rbac.PermDramaWrite), h.createDrama)
	admin.GET("/dramas/:dramaId", requirePermission(rbac.PermDramaRead), h.getDrama)
	admin.PUT("/dramas/:dramaId", requirePermission(rbac.PermDramaWrite), h.updateDrama)
	admin.GET("/dramas/:dramaId/episodes", requirePermission(rbac.PermDramaRead), h.listEpisodes)
	admin.POST("/dramas/:dramaId/episodes", requirePermission(rbac.PermEpisodeWrite), h.createEpisode)
	admin.PUT("/episodes/:episodeId", requirePermission(rbac.PermEpisodeWrite), h.updateEpisode)
	admin.POST("/episodes/:episodeId/publish", requirePermission(rbac.PermEpisodeWrite), h.publishEpisode)
	admin.POST("/episodes/:episodeId/unpublish", requirePermission(rbac.PermEpisodeWrite), h.unpublishEpisode)
	admin.GET("/feed-config/home", requirePermission(rbac.PermFeedConfigRead), h.getFeedConfig)
	admin.PUT("/feed-config/home", requirePermission(rbac.PermFeedConfigWrite), h.saveFeedDraft)
	admin.POST("/feed-config/home/publish", requirePermission(rbac.PermFeedConfigWrite), h.publishFeed)
	admin.POST("/feed-config/home/rollback", requirePermission(rbac.PermFeedConfigWrite), h.rollbackFeed)
	admin.GET("/users", requirePermission(rbac.PermPurchaseRead), h.listUsers)
	admin.GET("/users/:userId", requirePermission(rbac.PermPurchaseRead), h.getUser)
	admin.GET("/purchases", requirePermission(rbac.PermPurchaseRead), h.listPurchases)
	admin.GET("/purchases/:purchaseToken", requirePermission(rbac.PermPurchaseRead), h.getPurchase)
	admin.POST("/purchases/:purchaseToken/resync", requirePermission(rbac.PermPurchaseResync), h.resyncPurchase)
	admin.GET("/entitlements", requirePermission(rbac.PermEntitlementRead), h.listEntitlements)
	admin.GET("/entitlements/:userId", requirePermission(rbac.PermEntitlementRead), h.getUserEntitlements)
	admin.POST("/entitlements/:userId/recompute", requirePermission(rbac.PermEntitlementRecompute), h.recomputeEntitlements)
	admin.POST("/entitlements/:userId/grant-temp", requirePermission(rbac.PermEntitlementGrantTemp), h.grantTempEntitlement)
	admin.POST("/entitlements/:userId/revoke", requirePermission(rbac.PermEntitlementRevoke), h.revokeEntitlement)
	admin.GET("/rtdn-events", requirePermission(rbac.PermRTDNRead), h.listRTDNEvents)
	admin.POST("/rtdn-events/:messageId/replay", requirePermission(rbac.PermRTDNReplay), h.replayRTDN)
	admin.GET("/playback/sessions", requirePermission(rbac.PermPlaybackRead), h.listPlaybackSessions)
	admin.GET("/playback/sessions/:sessionId", requirePermission(rbac.PermPlaybackRead), h.getPlaybackSession)
	admin.GET("/audit-logs", requirePermission(rbac.PermAuditRead), h.listAuditLogs)
}

func (h Handler) login(c *gin.Context) {
	var request domain.AuthLoginRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, sessionToken, err := h.service.Login(c.Request.Context(), request)
	if err != nil {
		h.metrics.AdminLoginErrorTotal.Inc()
		response.Fail(c, err)
		return
	}
	h.metrics.AdminLoginTotal.Inc()
	adminauth.SetSessionCookie(c, sessionToken, data.ExpiresAt)
	response.Success(c, http.StatusOK, data)
}

func (h Handler) logout(c *gin.Context) {
	cookie, _ := c.Cookie(adminauth.SessionCookieName)
	if cookie != "" {
		sum := sha256.Sum256([]byte(cookie))
		_ = h.service.RevokeSessionHash(c.Request.Context(), hex.EncodeToString(sum[:]))
	}
	adminauth.ClearSessionCookie(c)
	response.Success(c, http.StatusOK, gin.H{"loggedOut": true})
}

func (h Handler) me(c *gin.Context) {
	user := adminauth.CurrentAdminUser(c)
	response.Success(c, http.StatusOK, gin.H{
		"adminUser":   user,
		"permissions": rbac.Permissions(user.Role),
	})
}

func (h Handler) dashboard(c *gin.Context) {
	data, err := h.service.Dashboard(c.Request.Context())
	respond(c, data, err)
}
func (h Handler) listDramas(c *gin.Context) {
	data, err := h.service.ListDramas(c.Request.Context(), c.Query("q"), c.Query("status"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}

func (h Handler) createDrama(c *gin.Context) {
	var request domain.DramaMutationRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminDramaUpdateTotal.Inc()
	data, err := h.service.CreateDrama(c.Request.Context(), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respondCreated(c, data, err)
}

func (h Handler) getDrama(c *gin.Context) {
	data, err := h.service.GetDrama(c.Request.Context(), c.Param("dramaId"))
	respond(c, data, err)
}

func (h Handler) updateDrama(c *gin.Context) {
	var request domain.DramaMutationRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminDramaUpdateTotal.Inc()
	data, err := h.service.UpdateDrama(c.Request.Context(), c.Param("dramaId"), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respond(c, data, err)
}

func (h Handler) listEpisodes(c *gin.Context) {
	items, err := h.service.ListEpisodes(c.Request.Context(), c.Param("dramaId"))
	if err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusOK, gin.H{"items": items})
}

func (h Handler) createEpisode(c *gin.Context) {
	var request domain.EpisodeMutationRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminEpisodeUpdateTotal.Inc()
	data, err := h.service.CreateEpisode(c.Request.Context(), c.Param("dramaId"), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respondCreated(c, data, err)
}

func (h Handler) updateEpisode(c *gin.Context) {
	var request domain.EpisodeMutationRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminEpisodeUpdateTotal.Inc()
	data, err := h.service.UpdateEpisode(c.Request.Context(), c.Param("episodeId"), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respond(c, data, err)
}

func (h Handler) publishEpisode(c *gin.Context) {
	h.metrics.AdminEpisodeUpdateTotal.Inc()
	if err := h.service.PublishEpisode(c.Request.Context(), c.Param("episodeId"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}

func (h Handler) unpublishEpisode(c *gin.Context) {
	h.metrics.AdminEpisodeUpdateTotal.Inc()
	if err := h.service.UnpublishEpisode(c.Request.Context(), c.Param("episodeId"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}

func (h Handler) getFeedConfig(c *gin.Context) {
	data, err := h.service.GetFeedConfig(c.Request.Context(), c.DefaultQuery("region", "US"), c.DefaultQuery("language", "en"))
	respond(c, data, err)
}

func (h Handler) saveFeedDraft(c *gin.Context) {
	var request domain.FeedConfigMutationRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	data, err := h.service.SaveFeedDraft(c.Request.Context(), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respond(c, data, err)
}

func (h Handler) publishFeed(c *gin.Context) {
	h.metrics.AdminFeedConfigPublishTotal.Inc()
	data, err := h.service.PublishFeed(c.Request.Context(), c.DefaultQuery("region", "US"), c.DefaultQuery("language", "en"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respondAccepted(c, data, err)
}

func (h Handler) rollbackFeed(c *gin.Context) {
	data, err := h.service.RollbackFeed(c.Request.Context(), c.DefaultQuery("region", "US"), c.DefaultQuery("language", "en"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c))
	respondAccepted(c, data, err)
}

func (h Handler) listUsers(c *gin.Context) {
	data, err := h.service.ListUsers(c.Request.Context(), c.Query("q"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}

func (h Handler) getUser(c *gin.Context) {
	data, err := h.service.GetUser(c.Request.Context(), c.Param("userId"))
	respond(c, data, err)
}
func (h Handler) listPurchases(c *gin.Context) {
	data, err := h.service.ListPurchases(c.Request.Context(), c.Query("q"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}
func (h Handler) getPurchase(c *gin.Context) {
	data, err := h.service.GetPurchase(c.Request.Context(), c.Param("purchaseToken"))
	respond(c, data, err)
}
func (h Handler) resyncPurchase(c *gin.Context) {
	h.metrics.AdminPurchaseResyncTotal.Inc()
	if err := h.service.ResyncPurchase(c.Request.Context(), c.Param("purchaseToken"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}
func (h Handler) listEntitlements(c *gin.Context) {
	data, err := h.service.ListEntitlements(c.Request.Context(), c.Query("userId"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}
func (h Handler) getUserEntitlements(c *gin.Context) {
	data, err := h.service.GetUserEntitlements(c.Request.Context(), c.Param("userId"))
	respond(c, data, err)
}
func (h Handler) recomputeEntitlements(c *gin.Context) {
	h.metrics.AdminEntitlementRecomputeTotal.Inc()
	if err := h.service.RecomputeEntitlements(c.Request.Context(), c.Param("userId"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}
func (h Handler) grantTempEntitlement(c *gin.Context) {
	var request domain.GrantTempRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminEntitlementGrantTempTotal.Inc()
	if err := h.service.GrantTempEntitlement(c.Request.Context(), c.Param("userId"), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}
func (h Handler) revokeEntitlement(c *gin.Context) {
	var request domain.RevokeEntitlementRequest
	if err := c.ShouldBindJSON(&request); err != nil { response.Fail(c, validationError(err)); return }
	h.metrics.AdminEntitlementRevokeTotal.Inc()
	if err := h.service.RevokeEntitlement(c.Request.Context(), c.Param("userId"), request, adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}
func (h Handler) listRTDNEvents(c *gin.Context) {
	data, err := h.service.ListRTDNEvents(c.Request.Context(), c.Query("q"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}
func (h Handler) replayRTDN(c *gin.Context) {
	h.metrics.AdminRTDNReplayTotal.Inc()
	if err := h.service.ReplayRTDN(c.Request.Context(), c.Param("messageId"), adminauth.CurrentAdminUser(c), adminauth.RequestID(c), adminauth.TraceID(c)); err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}
func (h Handler) listPlaybackSessions(c *gin.Context) {
	h.metrics.AdminPlaybackLookupTotal.Inc()
	data, err := h.service.ListPlaybackSessions(c.Request.Context(), c.Query("q"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}
func (h Handler) getPlaybackSession(c *gin.Context) {
	h.metrics.AdminPlaybackLookupTotal.Inc()
	data, err := h.service.GetPlaybackSession(c.Request.Context(), c.Param("sessionId"))
	respond(c, data, err)
}
func (h Handler) listAuditLogs(c *gin.Context) {
	data, err := h.service.ListAuditLogs(c.Request.Context(), c.Query("q"), intQuery(c, "page", 1), intQuery(c, "pageSize", 20))
	respond(c, data, err)
}

func respond[T any](c *gin.Context, data T, err error) {
	if err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusOK, data)
}

func respondCreated[T any](c *gin.Context, data T, err error) {
	if err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusCreated, data)
}

func respondAccepted[T any](c *gin.Context, data T, err error) {
	if err != nil { response.Fail(c, err); return }
	response.Success(c, http.StatusAccepted, data)
}

func validationError(err error) apperrors.AppError {
	return apperrors.AppError{
		Code:       apperrors.ErrValidation.Code,
		Message:    apperrors.ErrValidation.Message,
		HTTPStatus: apperrors.ErrValidation.HTTPStatus,
		Details:    map[string]any{"reason": err.Error()},
	}
}

func intQuery(c *gin.Context, key string, fallback int) int {
	value := c.Query(key)
	if value == "" { return fallback }
	parsed, err := strconv.Atoi(value)
	if err != nil { return fallback }
	return parsed
}
