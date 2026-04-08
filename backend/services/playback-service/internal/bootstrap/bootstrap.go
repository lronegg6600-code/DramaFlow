package bootstrap

import (
	"context"
	"net/http"

	sharedauth "dramaflow/backend/shared/auth"
	sharedcache "dramaflow/backend/shared/cache"
	sharedconfig "dramaflow/backend/shared/config"
	shareddb "dramaflow/backend/shared/db"
	sharedhttp "dramaflow/backend/shared/httpx"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/playback-service/internal/app"
	"dramaflow/backend/services/playback-service/internal/gateway"
	"dramaflow/backend/services/playback-service/internal/repository"
	"dramaflow/backend/services/playback-service/internal/service"
	"dramaflow/backend/services/playback-service/internal/signer"
	httptransport "dramaflow/backend/services/playback-service/internal/transport/http"
)

func Build(ctx context.Context) (*http.Server, func(context.Context) error, error) {
	cfg, err := sharedconfig.Load("playback-service", "8085")
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
	redisClient, err := sharedcache.NewRedisClient(ctx, cfg)
	if err != nil {
		return nil, nil, err
	}
	log := app.NewLogger(cfg)
	metrics := sharedtelemetry.NewMetrics(cfg.ServiceName)
	repo := repository.New(pool, redisClient)
	entitlementGateway := gateway.NewRemoteEntitlementGateway(cfg.Services.EntitlementServiceBaseURL)

	var playbackSigner signer.PlaybackUrlSigner
	if cfg.Playback.SignerMode == "cloudfront_signed_url" {
		playbackSigner = signer.NewCloudFrontSignedUrlSigner(cfg)
	} else {
		playbackSigner = signer.NewDevPassthroughSigner(cfg)
	}
	assetLocator := signer.NewDefaultPlaybackAssetLocator(cfg)

	svc := service.New(repo, entitlementGateway, assetLocator, playbackSigner, cfg, log)
	handler := httptransport.New(svc, metrics, cfg.GinMode)
	router := app.NewRouter(cfg, log, metrics, sharedauth.NewTokenManager(cfg.JWT), handler)
	server := sharedhttp.NewServer(cfg, router)
	return server, shutdownTelemetry, nil
}
