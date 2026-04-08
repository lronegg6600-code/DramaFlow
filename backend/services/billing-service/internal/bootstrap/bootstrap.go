package bootstrap

import (
	"context"
	"net/http"

	sharedauth "dramaflow/backend/shared/auth"
	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/billing-service/internal/app"
	"dramaflow/backend/services/billing-service/internal/gateway"
	"dramaflow/backend/services/billing-service/internal/repository"
	"dramaflow/backend/services/billing-service/internal/rtdn"
	"dramaflow/backend/services/billing-service/internal/service"
	httptransport "dramaflow/backend/services/billing-service/internal/transport/http"
	"dramaflow/backend/services/billing-service/internal/verifier"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("billing-service", "8087")
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
	entitlementClient := gateway.NewEntitlementServiceClient(cfg.Services.EntitlementServiceBaseURL)
	publisherGateway := verifier.NewGooglePlayPublisherGateway(cfg)
	parser := rtdn.NewParser()
	svc := service.New(repo, entitlementClient, publisherGateway, cfg)
	handler := httptransport.New(svc, parser, metrics, cfg.Billing.RTDNPushSecret, cfg.Billing.RTDNEnabled, cfg.GinMode)
	router := app.NewRouter(cfg, log, metrics, sharedauth.NewTokenManager(cfg.JWT), handler)
	server := sharedhttp.NewServer(cfg, router)
	return server, shutdownTelemetry, nil
}
