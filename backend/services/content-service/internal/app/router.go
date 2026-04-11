package app

import (
	"net/http"

	sharedauth "dramaflow/backend/shared/auth"
	"dramaflow/backend/shared/config"
	sharedlogger "dramaflow/backend/shared/logger"
	"dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	"dramaflow/backend/shared/telemetry"
	httptransport "dramaflow/backend/services/content-service/internal/transport/http"
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"
)

func NewRouter(cfg config.Config, baseLogger zerolog.Logger, metrics telemetry.Metrics, tokens sharedauth.TokenManager, handler httptransport.Handler) *gin.Engine {
	gin.SetMode(cfg.GinMode)
	router := gin.New()
	router.Use(middleware.Recovery(), middleware.CORS(), middleware.RequestContext(), telemetry.TraceMiddleware(cfg.ServiceName), middleware.OptionalAuth(tokens), middleware.Logging(baseLogger, metrics, cfg.ServiceName))
	router.Use(middleware.ServiceGuards(cfg)...)
	router.GET("/health/live", func(c *gin.Context) { response.Success(c, http.StatusOK, gin.H{"status": "ok"}) })
	router.GET("/health/ready", func(c *gin.Context) { response.Success(c, http.StatusOK, gin.H{"status": "ready"}) })
	router.GET("/metrics", telemetry.MetricsHandler())

	v1 := router.Group("/v1")
	handler.RegisterRoutes(v1)
	return router
}

func NewLogger(cfg config.Config) zerolog.Logger {
	return sharedlogger.New(cfg.ServiceName, cfg.Environment)
}
