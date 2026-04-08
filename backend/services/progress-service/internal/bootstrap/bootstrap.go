package bootstrap

import (
	"context"
	"net/http"

	sharedauth "dramaflow/backend/shared/auth"
	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/progress-service/internal/app"
	"dramaflow/backend/services/progress-service/internal/repository"
	"dramaflow/backend/services/progress-service/internal/service"
	httptransport "dramaflow/backend/services/progress-service/internal/transport/http"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("progress-service", "8084")
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
	svc := service.New(repo)
	handler := httptransport.New(svc, metrics)
	router := app.NewRouter(cfg, log, metrics, sharedauth.NewTokenManager(cfg.JWT), handler)
	server := sharedhttp.NewServer(cfg, router)
	return server, shutdownTelemetry, nil
}
