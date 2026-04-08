package http

import (
	"io"
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/billing-service/internal/domain"
	"dramaflow/backend/services/billing-service/internal/rtdn"
	"dramaflow/backend/services/billing-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service        service.Service
	parser         rtdn.Parser
	metrics        sharedtelemetry.Metrics
	pushSecret     string
	rtdnEnabled    bool
	ginMode        string
}

func New(service service.Service, parser rtdn.Parser, metrics sharedtelemetry.Metrics, pushSecret string, rtdnEnabled bool, ginMode string) Handler {
	return Handler{
		service:     service,
		parser:      parser,
		metrics:     metrics,
		pushSecret:  pushSecret,
		rtdnEnabled: rtdnEnabled,
		ginMode:     ginMode,
	}
}

func (h Handler) RegisterRoutes(v1 *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	billing := v1.Group("/billing")
	billing.POST("/google-play/purchases:sync", authMiddleware, h.syncPurchase)
	billing.POST("/google-play/rtdn", h.googlePlayRTDN)
	billing.POST("/google-play/purchases/:purchaseToken/resync", authMiddleware, h.resyncPurchase)
	billing.GET("/me/subscription-status", authMiddleware, h.subscriptionStatus)
	billing.GET("/google-play/catalog", authMiddleware, h.catalog)
	v1.POST("/internal/billing/purchases/:purchaseToken/resync", h.internalResyncPurchase)
	if h.ginMode != "release" {
		v1.POST("/dev/billing/mock-rtdn", h.mockRTDN)
	}
}

func (h Handler) syncPurchase(c *gin.Context) {
	var request domain.SyncPurchaseRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		response.Fail(c, validationError(err))
		h.metrics.BillingPurchaseSyncErrorTotal.Inc()
		return
	}
	h.metrics.BillingPurchaseSyncTotal.Inc()
	h.metrics.BillingPurchaseVerifyTotal.Inc()
	data, err := h.service.SyncPurchase(c.Request.Context(), sharedmiddleware.UserID(c), request, traceID(c))
	if err != nil {
		response.Fail(c, err)
		h.metrics.BillingPurchaseSyncErrorTotal.Inc()
		h.metrics.BillingPurchaseVerifyErrorTotal.Inc()
		return
	}
	response.Success(c, http.StatusAccepted, data)
}

func (h Handler) resyncPurchase(c *gin.Context) {
	h.metrics.BillingPurchaseSyncTotal.Inc()
	h.metrics.BillingPurchaseVerifyTotal.Inc()
	data, err := h.service.ResyncPurchase(c.Request.Context(), sharedmiddleware.UserID(c), c.Param("purchaseToken"), traceID(c))
	if err != nil {
		response.Fail(c, err)
		h.metrics.BillingPurchaseSyncErrorTotal.Inc()
		h.metrics.BillingPurchaseVerifyErrorTotal.Inc()
		return
	}
	response.Success(c, http.StatusAccepted, data)
}

func (h Handler) internalResyncPurchase(c *gin.Context) {
	h.metrics.BillingPurchaseSyncTotal.Inc()
	h.metrics.BillingPurchaseVerifyTotal.Inc()
	data, err := h.service.ResyncPurchase(c.Request.Context(), "", c.Param("purchaseToken"), traceID(c))
	if err != nil {
		response.Fail(c, err)
		h.metrics.BillingPurchaseSyncErrorTotal.Inc()
		h.metrics.BillingPurchaseVerifyErrorTotal.Inc()
		return
	}
	response.Success(c, http.StatusAccepted, data)
}

func (h Handler) subscriptionStatus(c *gin.Context) {
	data, err := h.service.SubscriptionStatus(c.Request.Context(), sharedmiddleware.UserID(c), c.GetHeader("Authorization"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) catalog(c *gin.Context) {
	data, err := h.service.Catalog(c.Request.Context())
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, gin.H{"products": data})
}

func (h Handler) googlePlayRTDN(c *gin.Context) {
	if err := service.EnsureRTDNEnabled(h.rtdnEnabled); err != nil {
		response.Fail(c, err)
		return
	}
	h.metrics.BillingRTDNReceivedTotal.Inc()
	if err := service.RequireSecret(h.rtdnEnabled, h.pushSecret, c.GetHeader("X-RTDN-Secret")); err != nil {
		response.Fail(c, err)
		return
	}
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		response.Fail(c, apperrors.ErrInternal)
		return
	}
	var envelope domain.RtdnEnvelope
	_ = c.ShouldBindJSON(&envelope)
	event, err := h.parser.Parse(c.Request.Context(), envelope, body)
	if err != nil {
		response.Fail(c, err)
		return
	}
	data, duplicate, err := h.service.HandleRTDNEvent(c.Request.Context(), event, traceID(c))
	if err != nil {
		response.Fail(c, err)
		h.metrics.BillingPurchaseVerifyErrorTotal.Inc()
		return
	}
	if duplicate {
		h.metrics.BillingRTDNDuplicateTotal.Inc()
	} else {
		h.metrics.BillingRTDNProcessedTotal.Inc()
	}
	response.Success(c, http.StatusAccepted, data)
}

func (h Handler) mockRTDN(c *gin.Context) {
	h.googlePlayRTDN(c)
}

func validationError(err error) apperrors.AppError {
	return apperrors.AppError{
		Code:       apperrors.ErrValidation.Code,
		Message:    apperrors.ErrValidation.Message,
		HTTPStatus: apperrors.ErrValidation.HTTPStatus,
		Details:    map[string]any{"reason": err.Error()},
	}
}

func traceID(c *gin.Context) string {
	value, _ := c.Get(sharedmiddleware.ContextTraceIDKey)
	cast, _ := value.(string)
	return cast
}
