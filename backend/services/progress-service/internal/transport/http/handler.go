package http

import (
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/progress-service/internal/domain"
	"dramaflow/backend/services/progress-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
	metrics sharedtelemetry.Metrics
}

func New(service service.Service, metrics sharedtelemetry.Metrics) Handler {
	return Handler{service: service, metrics: metrics}
}

func (h Handler) RegisterRoutes(router *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	protected := router.Group("/")
	protected.Use(authMiddleware)
	protected.GET("/progress/episodes/:episodeId", h.getEpisodeProgress)
	protected.PUT("/progress/episodes/:episodeId", h.putEpisodeProgress)
	protected.GET("/history/recent", h.recentHistory)
}

func (h Handler) getEpisodeProgress(c *gin.Context) {
	data, err := h.service.GetEpisodeProgress(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("episodeId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) putEpisodeProgress(c *gin.Context) {
	var req domain.ProgressUpsertRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Fail(c, apperrors.AppError{
			Code:       apperrors.ErrValidation.Code,
			Message:    apperrors.ErrValidation.Message,
			HTTPStatus: apperrors.ErrValidation.HTTPStatus,
			Details:    map[string]any{"reason": err.Error()},
		})
		return
	}
	data, err := h.service.UpsertEpisodeProgress(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("episodeId"), req)
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.ProgressWriteCount.Inc()
	response.Success(c, http.StatusOK, data)
}

func (h Handler) recentHistory(c *gin.Context) {
	data, err := h.service.RecentHistory(c.Request.Context(), sharedmiddleware.UserID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}
