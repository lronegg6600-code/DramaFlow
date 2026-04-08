package bootstrap

import (
	"context"
	"net/http"

	sharedauth "dramaflow/backend/shared/auth"
	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/entitlement-service/internal/app"
	"dramaflow/backend/services/entitlement-service/internal/projector"
	"dramaflow/backend/services/entitlement-service/internal/repository"
	"dramaflow/backend/services/entitlement-service/internal/service"
	httptransport "dramaflow/backend/services/entitlement-service/internal/transport/http"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("entitlement-service", "8086")
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
	projector := projector.New(repo)
	svc := service.New(repo, projector, cfg)
	handler := httptransport.New(svc, metrics, cfg.GinMode)
	router := app.NewRouter(cfg, log, metrics, sharedauth.NewTokenManager(cfg.JWT), handler)
	server := sharedhttp.NewServer(cfg, router)
	return server, shutdownTelemetry, nil
}
