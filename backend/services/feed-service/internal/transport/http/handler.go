package http

import (
	"net/http"

	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/feed-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
	metrics sharedtelemetry.Metrics
}

func New(service service.Service, metrics sharedtelemetry.Metrics) Handler {
	return Handler{service: service, metrics: metrics}
}

func (h Handler) RegisterRoutes(router *gin.RouterGroup) {
	router.GET("/home", h.home)
	router.GET("/continue-watching", h.continueWatching)
}

func (h Handler) home(c *gin.Context) {
	data, err := h.service.Home(c.Request.Context(), sharedmiddleware.UserID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.FeedHitCount.Inc()
	response.Success(c, http.StatusOK, data)
}

func (h Handler) continueWatching(c *gin.Context) {
	data, err := h.service.ContinueWatching(c.Request.Context(), sharedmiddleware.UserID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}
