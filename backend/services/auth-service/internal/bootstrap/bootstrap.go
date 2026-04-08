package bootstrap

import (
	"context"
	"net/http"

	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/auth-service/internal/app"
	"dramaflow/backend/services/auth-service/internal/repository"
	"dramaflow/backend/services/auth-service/internal/service"
	httptransport "dramaflow/backend/services/auth-service/internal/transport/http"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("auth-service", "8081")
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
	svc := service.New(repo, cfg)
	handler := httptransport.New(svc)
	router := app.NewRouter(cfg, log, metrics, svc.TokenManager(), handler)
	server := sharedhttp.NewServer(cfg, router)
	return server, shutdownTelemetry, nil
}
