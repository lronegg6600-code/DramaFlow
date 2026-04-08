package bootstrap

import (
	"context"
	"net/http"

	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	adminauth "dramaflow/backend/services/admin-service/internal/auth"
	"dramaflow/backend/services/admin-service/internal/app"
	"dramaflow/backend/services/admin-service/internal/audit"
	"dramaflow/backend/services/admin-service/internal/domain"
	"dramaflow/backend/services/admin-service/internal/gateway"
	"dramaflow/backend/services/admin-service/internal/repository"
	"dramaflow/backend/services/admin-service/internal/service"
	httptransport "dramaflow/backend/services/admin-service/internal/transport/http"
	"github.com/gin-gonic/gin"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("admin-service", "8088")
	if err != nil {
		return nil, nil, err
	}
	shutdownTelemetry, err := sharedtelemetry.InstallTracing(ctx, cfg)
	if err != nil {
		return nil, nil, err
	}
	pool, err := shareddb.NewPostgresPool(ctx, cfg)
	if err != nil {
		return nil, nil, err
	}
	log := app.NewLogger(cfg)
	metrics := sharedtelemetry.NewMetrics(cfg.ServiceName)
	repo := repository.New(pool)
	auditWriter := audit.New(repo, metrics)
	billingClient := gateway.NewBillingClient(cfg.Services.BillingServiceBaseURL, cfg.Billing.RTDNPushSecret)
	entitlementClient := gateway.NewEntitlementClient(cfg.Services.EntitlementServiceBaseURL)
	svc := service.New(repo, auditWriter, billingClient, entitlementClient, cfg)
	if err := svc.Bootstrap(ctx); err != nil {
		return nil, nil, err
	}
	handler := httptransport.New(svc, metrics)
	router := app.NewRouter(cfg, log, metrics, sessionResolver{service: svc}, handler)
	return sharedhttp.NewServer(cfg, router), shutdownTelemetry, nil
}

type sessionResolver struct {
	service service.Service
}

func (s sessionResolver) ResolveSessionHash(c *gin.Context, sessionHash string) (domain.AdminUser, error) {
	return s.service.ResolveSessionHash(c.Request.Context(), sessionHash)
}

var _ adminauth.SessionResolver = sessionResolver{}
