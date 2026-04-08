package http

import (
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/playback-service/internal/domain"
	"dramaflow/backend/services/playback-service/internal/service"
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

func (h Handler) RegisterRoutes(router *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	protected := router.Group("/playback")
	protected.Use(authMiddleware)
	protected.POST("/sessions", h.createSession)
	protected.GET("/sessions/:sessionId", h.getSession)
	protected.POST("/sessions/:sessionId/heartbeat", h.heartbeat)
	protected.POST("/sessions/:sessionId/complete", h.complete)
	protected.POST("/sessions/:sessionId/refresh", h.refresh)

	if h.ginMode != "release" {
		router.POST("/dev/entitlements/grant-premium", h.grantPremium)
		router.POST("/dev/entitlements/revoke-premium", h.revokePremium)
	}
}

func (h Handler) createSession(c *gin.Context) {
	var request domain.CreatePlaybackSessionRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		h.metrics.PlaybackSessionCreateErrorTotal.Inc()
		return
	}
	descriptor, err := h.service.CreateSession(c.Request.Context(), sharedmiddleware.UserID(c), request, requestID(c), traceID(c))
	if err != nil {
		response.Fail(c, err)
		h.metrics.PlaybackSessionCreateErrorTotal.Inc()
		if appErr, ok := err.(apperrors.AppError); ok && appErr.Code == "playback.signer_config_missing" {
			h.metrics.PlaybackSignErrorTotal.Inc()
		}
		return
	}
	h.metrics.PlaybackSessionCreateTotal.Inc()
	if descriptor.PlaybackMode == domain.PlaybackModePreview {
		h.metrics.PlaybackPreviewModeTotal.Inc()
	} else {
		h.metrics.PlaybackFullModeTotal.Inc()
	}
	response.Success(c, http.StatusCreated, descriptor)
}

func (h Handler) getSession(c *gin.Context) {
	data, err := h.service.GetSession(c.Request.Context(), c.Param("sessionId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) heartbeat(c *gin.Context) {
	var request domain.HeartbeatRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.Heartbeat(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("sessionId"), request, requestID(c), traceID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.PlaybackHeartbeatTotal.Inc()
	response.Success(c, http.StatusOK, data)
}

func (h Handler) complete(c *gin.Context) {
	var request domain.CompleteRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	data, err := h.service.Complete(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("sessionId"), request, requestID(c), traceID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.PlaybackCompleteTotal.Inc()
	response.Success(c, http.StatusOK, data)
}

func (h Handler) refresh(c *gin.Context) {
	data, err := h.service.Refresh(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("sessionId"), requestID(c), traceID(c))
	if err != nil {
		response.Fail(c, err)
		if appErr, ok := err.(apperrors.AppError); ok && appErr.Code == "playback.signer_config_missing" {
			h.metrics.PlaybackSignErrorTotal.Inc()
		}
		return
	}
	h.metrics.PlaybackRefreshTotal.Inc()
	response.Success(c, http.StatusOK, data)
}

func (h Handler) grantPremium(c *gin.Context) {
	var request domain.DevEntitlementGrantRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	if request.EntitlementType == "" {
		request.EntitlementType = "premium"
	}
	if err := h.service.GrantDevEntitlement(c.Request.Context(), request); err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}

func (h Handler) revokePremium(c *gin.Context) {
	var request domain.DevEntitlementRevokeRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		return
	}
	if err := h.service.RevokeDevEntitlement(c.Request.Context(), request.UserID); err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusAccepted, gin.H{"accepted": true})
}

func validationError(err error) apperrors.AppError {
	return apperrors.AppError{
		Code:       apperrors.ErrValidation.Code,
		Message:    apperrors.ErrValidation.Message,
		HTTPStatus: apperrors.ErrValidation.HTTPStatus,
		Details:    map[string]any{"reason": err.Error()},
	}
}

func requestID(c *gin.Context) string {
	value, _ := c.Get(sharedmiddleware.ContextRequestIDKey)
	cast, _ := value.(string)
	return cast
}

func traceID(c *gin.Context) string {
	value, _ := c.Get(sharedmiddleware.ContextTraceIDKey)
	cast, _ := value.(string)
	return cast
}
