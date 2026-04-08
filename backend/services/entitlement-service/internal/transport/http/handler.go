package http

import (
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/entitlement-service/internal/domain"
	"dramaflow/backend/services/entitlement-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
	metrics sharedtelemetry.Metrics
	ginMode string
}

func New(service service.Service, metrics sharedtelemetry.Metrics, ginMode string) Handler {
	return Handler{service: service, metrics: metrics, ginMode: ginMode}
}

func (h Handler) RegisterRoutes(v1 *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	protected := v1.Group("/entitlements")
	protected.Use(authMiddleware)
	protected.GET("/me", h.me)
	protected.GET("/me/playback-access", h.mePlaybackAccess)

	v1.POST("/entitlements/grants", h.grant)
	v1.POST("/entitlements/revoke", h.revoke)
	v1.GET("/internal/entitlements/users/:userId/playback-access", h.internalPlaybackAccess)
	v1.POST("/internal/entitlements/users/:userId/recompute", h.recompute)
	if h.ginMode != "release" {
		v1.POST("/entitlements/recompute/:userId", h.recompute)
	}
}

func (h Handler) me(c *gin.Context) {
	data, err := h.service.Me(c.Request.Context(), sharedmiddleware.UserID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) mePlaybackAccess(c *gin.Context) {
	var query domain.PlaybackAccessQuery
	if err := c.ShouldBindQuery(&query); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.PlaybackAccess(c.Request.Context(), sharedmiddleware.UserID(c), query)
	if err != nil {
		response.Fail(c, err)
		return
	}
	recordPlaybackMetric(h.metrics, data.AccessLevel)
	response.Success(c, http.StatusOK, data)
}

func (h Handler) internalPlaybackAccess(c *gin.Context) {
	var query domain.PlaybackAccessQuery
	if err := c.ShouldBindQuery(&query); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.PlaybackAccess(c.Request.Context(), c.Param("userId"), query)
	if err != nil {
		response.Fail(c, err)
		return
	}
	recordPlaybackMetric(h.metrics, data.AccessLevel)
	response.Success(c, http.StatusOK, data)
}

func (h Handler) grant(c *gin.Context) {
	var request domain.GrantRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.Grant(c.Request.Context(), request)
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.EntitlementGrantTotal.Inc()
	response.Success(c, http.StatusAccepted, data)
}

func (h Handler) revoke(c *gin.Context) {
	var request domain.RevokeRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.Revoke(c.Request.Context(), request)
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.EntitlementRevokeTotal.Inc()
	response.Success(c, http.StatusAccepted, gin.H{"updated": len(data)})
}

func (h Handler) recompute(c *gin.Context) {
	data, err := h.service.Recompute(c.Request.Context(), c.Param("userId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.EntitlementRecomputeTotal.Inc()
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

func recordPlaybackMetric(metrics sharedtelemetry.Metrics, accessLevel string) {
	metrics.PlaybackAccessCheckTotal.Inc()
	switch accessLevel {
	case "full_access":
		metrics.PlaybackAccessFullTotal.Inc()
	case "preview_only":
		metrics.PlaybackAccessPreviewTotal.Inc()
	default:
		metrics.PlaybackAccessNoneTotal.Inc()
	}
}
