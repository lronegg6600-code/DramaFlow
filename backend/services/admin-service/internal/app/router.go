package app

import (
	"net/http"

	"dramaflow/backend/shared/config"
	sharedlogger "dramaflow/backend/shared/logger"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	adminauth "dramaflow/backend/services/admin-service/internal/auth"
	httptransport "dramaflow/backend/services/admin-service/internal/transport/http"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"
)

func NewRouter(cfg config.Config, baseLogger zerolog.Logger, metrics sharedtelemetry.Metrics, resolver adminauth.SessionResolver, handler httptransport.Handler) *gin.Engine {
	gin.SetMode(cfg.GinMode)
	router := gin.New()
	router.Use(
		sharedmiddleware.Recovery(),
		cors.New(cors.Config{
			AllowOrigins:     cfg.Admin.AllowedOrigins,
			AllowMethods:     []string{"GET", "POST", "PUT", "OPTIONS"},
			AllowHeaders:     []string{"Content-Type", "X-Request-Id"},
			AllowCredentials: true,
		}),
		sharedmiddleware.RequestContext(),
		sharedtelemetry.TraceMiddleware(cfg.ServiceName),
		sharedmiddleware.Logging(baseLogger, metrics, cfg.ServiceName),
	)
	router.GET("/health/live", func(c *gin.Context) { response.Success(c, http.StatusOK, gin.H{"status": "ok"}) })
	router.GET("/health/ready", func(c *gin.Context) { response.Success(c, http.StatusOK, gin.H{"status": "ready"}) })
	router.GET("/metrics", sharedtelemetry.MetricsHandler())

	v1 := router.Group("/v1")
	handler.RegisterRoutes(v1, adminauth.RequireSession(resolver), adminauth.RequirePermission)
	return router
}

func NewLogger(cfg config.Config) zerolog.Logger {
	return sharedlogger.New(cfg.ServiceName, cfg.Environment)
}
