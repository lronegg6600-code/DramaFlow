package telemetry

import (
	"context"

	"dramaflow/backend/shared/config"
	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/contrib/instrumentation/github.com/gin-gonic/gin/otelgin"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.28.0"
)

type Metrics struct {
	RequestCount       *prometheus.CounterVec
	RequestLatency     *prometheus.HistogramVec
	ErrorCount         *prometheus.CounterVec
	ProgressWriteCount prometheus.Counter
	FeedHitCount       prometheus.Counter
	PlaybackSessionCreateTotal      prometheus.Counter
	PlaybackSessionCreateErrorTotal prometheus.Counter
	PlaybackHeartbeatTotal          prometheus.Counter
	PlaybackRefreshTotal            prometheus.Counter
	PlaybackCompleteTotal           prometheus.Counter
	PlaybackPreviewModeTotal        prometheus.Counter
	PlaybackFullModeTotal           prometheus.Counter
	PlaybackSignErrorTotal          prometheus.Counter
	BillingPurchaseSyncTotal        prometheus.Counter
	BillingPurchaseSyncErrorTotal   prometheus.Counter
	BillingPurchaseVerifyTotal      prometheus.Counter
	BillingPurchaseVerifyErrorTotal prometheus.Counter
	BillingRTDNReceivedTotal        prometheus.Counter
	BillingRTDNProcessedTotal       prometheus.Counter
	BillingRTDNDuplicateTotal       prometheus.Counter
	EntitlementGrantTotal          prometheus.Counter
	EntitlementRevokeTotal         prometheus.Counter
	EntitlementRecomputeTotal      prometheus.Counter
	PlaybackAccessCheckTotal       prometheus.Counter
	PlaybackAccessFullTotal        prometheus.Counter
	PlaybackAccessPreviewTotal     prometheus.Counter
	PlaybackAccessNoneTotal        prometheus.Counter
	AdminLoginTotal                prometheus.Counter
	AdminLoginErrorTotal           prometheus.Counter
	AdminAuditLogWriteTotal        prometheus.Counter
	AdminDramaUpdateTotal          prometheus.Counter
	AdminEpisodeUpdateTotal        prometheus.Counter
	AdminFeedConfigPublishTotal    prometheus.Counter
	AdminPurchaseResyncTotal       prometheus.Counter
	AdminEntitlementRecomputeTotal prometheus.Counter
	AdminEntitlementGrantTempTotal prometheus.Counter
	AdminEntitlementRevokeTotal    prometheus.Counter
	AdminRTDNReplayTotal           prometheus.Counter
	AdminPlaybackLookupTotal       prometheus.Counter
}

func InstallTracing(ctx context.Context, cfg config.Config) (func(context.Context) error, error) {
	exporter, err := otlptracehttp.New(ctx, otlptracehttp.WithEndpointURL(cfg.OTLPEndpoint))
	if err != nil {
		return nil, err
	}

	provider := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceName(cfg.ServiceName),
		)),
	)
	otel.SetTracerProvider(provider)
	return provider.Shutdown, nil
}

func NewMetrics(service string) Metrics {
	requestCount := prometheus.NewCounterVec(
		prometheus.CounterOpts{Name: "dramaflow_http_requests_total", Help: "Total HTTP requests."},
		[]string{"service", "method", "route", "status"},
	)
	requestLatency := prometheus.NewHistogramVec(
		prometheus.HistogramOpts{Name: "dramaflow_http_request_duration_seconds", Help: "HTTP request latency."},
		[]string{"service", "method", "route"},
	)
	errorCount := prometheus.NewCounterVec(
		prometheus.CounterOpts{Name: "dramaflow_http_errors_total", Help: "HTTP error count."},
		[]string{"service", "route", "code"},
	)
	progressWriteCount := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_progress_write_total",
		Help: "Progress writes handled by progress-service.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	feedHitCount := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_feed_hit_total",
		Help: "Feed home requests.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackSessionCreateTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_session_create_total",
		Help: "Playback session create requests.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackSessionCreateErrorTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_session_create_error_total",
		Help: "Playback session create failures.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackHeartbeatTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_heartbeat_total",
		Help: "Playback heartbeat requests.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackRefreshTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_refresh_total",
		Help: "Playback refresh requests.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackCompleteTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_complete_total",
		Help: "Playback complete requests.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackPreviewModeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_preview_mode_total",
		Help: "Playback sessions granted in preview mode.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackFullModeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_full_mode_total",
		Help: "Playback sessions granted in full mode.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackSignErrorTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "dramaflow_playback_sign_error_total",
		Help: "Playback signing failures.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingPurchaseSyncTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_purchase_sync_total",
		Help: "Billing sync attempts.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingPurchaseSyncErrorTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_purchase_sync_error_total",
		Help: "Billing sync failures.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingPurchaseVerifyTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_purchase_verify_total",
		Help: "Google Play verification attempts.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingPurchaseVerifyErrorTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_purchase_verify_error_total",
		Help: "Google Play verification failures.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingRTDNReceivedTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_rtdn_received_total",
		Help: "RTDN notifications received.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingRTDNProcessedTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_rtdn_processed_total",
		Help: "RTDN notifications processed.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	billingRTDNDuplicateTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "billing_rtdn_duplicate_total",
		Help: "Duplicate RTDN notifications detected.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	entitlementGrantTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "entitlement_grant_total",
		Help: "Entitlement grants applied.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	entitlementRevokeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "entitlement_revoke_total",
		Help: "Entitlement revokes applied.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	entitlementRecomputeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "entitlement_recompute_total",
		Help: "Entitlement recomputes executed.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackAccessCheckTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "playback_access_check_total",
		Help: "Playback access checks.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackAccessFullTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "playback_access_full_total",
		Help: "Playback full access outcomes.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackAccessPreviewTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "playback_access_preview_total",
		Help: "Playback preview access outcomes.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	playbackAccessNoneTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "playback_access_none_total",
		Help: "Playback denied outcomes.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminLoginTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_login_total",
		Help: "Admin login attempts.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminLoginErrorTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_login_error_total",
		Help: "Admin login failures.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminAuditLogWriteTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_audit_log_write_total",
		Help: "Admin audit log writes.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminDramaUpdateTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_drama_update_total",
		Help: "Drama mutations executed by admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminEpisodeUpdateTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_episode_update_total",
		Help: "Episode mutations executed by admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminFeedConfigPublishTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_feed_config_publish_total",
		Help: "Feed config publish operations.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminPurchaseResyncTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_purchase_resync_total",
		Help: "Purchase resync operations from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminEntitlementRecomputeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_entitlement_recompute_total",
		Help: "Entitlement recompute operations from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminEntitlementGrantTempTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_entitlement_grant_temp_total",
		Help: "Temporary entitlement grants from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminEntitlementRevokeTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_entitlement_revoke_total",
		Help: "Entitlement revokes from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminRTDNReplayTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_rtdn_replay_total",
		Help: "RTDN replay operations from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})
	adminPlaybackLookupTotal := prometheus.NewCounter(prometheus.CounterOpts{
		Name: "admin_playback_lookup_total",
		Help: "Playback lookup operations from admin.",
		ConstLabels: prometheus.Labels{"service": service},
	})

	prometheus.MustRegister(
		requestCount,
		requestLatency,
		errorCount,
		progressWriteCount,
		feedHitCount,
		playbackSessionCreateTotal,
		playbackSessionCreateErrorTotal,
		playbackHeartbeatTotal,
		playbackRefreshTotal,
		playbackCompleteTotal,
		playbackPreviewModeTotal,
		playbackFullModeTotal,
		playbackSignErrorTotal,
		billingPurchaseSyncTotal,
		billingPurchaseSyncErrorTotal,
		billingPurchaseVerifyTotal,
		billingPurchaseVerifyErrorTotal,
		billingRTDNReceivedTotal,
		billingRTDNProcessedTotal,
		billingRTDNDuplicateTotal,
		entitlementGrantTotal,
		entitlementRevokeTotal,
		entitlementRecomputeTotal,
		playbackAccessCheckTotal,
		playbackAccessFullTotal,
		playbackAccessPreviewTotal,
		playbackAccessNoneTotal,
		adminLoginTotal,
		adminLoginErrorTotal,
		adminAuditLogWriteTotal,
		adminDramaUpdateTotal,
		adminEpisodeUpdateTotal,
		adminFeedConfigPublishTotal,
		adminPurchaseResyncTotal,
		adminEntitlementRecomputeTotal,
		adminEntitlementGrantTempTotal,
		adminEntitlementRevokeTotal,
		adminRTDNReplayTotal,
		adminPlaybackLookupTotal,
	)
	return Metrics{
		RequestCount:       requestCount,
		RequestLatency:     requestLatency,
		ErrorCount:         errorCount,
		ProgressWriteCount: progressWriteCount,
		FeedHitCount:       feedHitCount,
		PlaybackSessionCreateTotal:      playbackSessionCreateTotal,
		PlaybackSessionCreateErrorTotal: playbackSessionCreateErrorTotal,
		PlaybackHeartbeatTotal:          playbackHeartbeatTotal,
		PlaybackRefreshTotal:            playbackRefreshTotal,
		PlaybackCompleteTotal:           playbackCompleteTotal,
		PlaybackPreviewModeTotal:        playbackPreviewModeTotal,
		PlaybackFullModeTotal:           playbackFullModeTotal,
		PlaybackSignErrorTotal:          playbackSignErrorTotal,
		BillingPurchaseSyncTotal:        billingPurchaseSyncTotal,
		BillingPurchaseSyncErrorTotal:   billingPurchaseSyncErrorTotal,
		BillingPurchaseVerifyTotal:      billingPurchaseVerifyTotal,
		BillingPurchaseVerifyErrorTotal: billingPurchaseVerifyErrorTotal,
		BillingRTDNReceivedTotal:        billingRTDNReceivedTotal,
		BillingRTDNProcessedTotal:       billingRTDNProcessedTotal,
		BillingRTDNDuplicateTotal:       billingRTDNDuplicateTotal,
		EntitlementGrantTotal:          entitlementGrantTotal,
		EntitlementRevokeTotal:         entitlementRevokeTotal,
		EntitlementRecomputeTotal:      entitlementRecomputeTotal,
		PlaybackAccessCheckTotal:       playbackAccessCheckTotal,
		PlaybackAccessFullTotal:        playbackAccessFullTotal,
		PlaybackAccessPreviewTotal:     playbackAccessPreviewTotal,
		PlaybackAccessNoneTotal:        playbackAccessNoneTotal,
		AdminLoginTotal:                adminLoginTotal,
		AdminLoginErrorTotal:           adminLoginErrorTotal,
		AdminAuditLogWriteTotal:        adminAuditLogWriteTotal,
		AdminDramaUpdateTotal:          adminDramaUpdateTotal,
		AdminEpisodeUpdateTotal:        adminEpisodeUpdateTotal,
		AdminFeedConfigPublishTotal:    adminFeedConfigPublishTotal,
		AdminPurchaseResyncTotal:       adminPurchaseResyncTotal,
		AdminEntitlementRecomputeTotal: adminEntitlementRecomputeTotal,
		AdminEntitlementGrantTempTotal: adminEntitlementGrantTempTotal,
		AdminEntitlementRevokeTotal:    adminEntitlementRevokeTotal,
		AdminRTDNReplayTotal:           adminRTDNReplayTotal,
		AdminPlaybackLookupTotal:       adminPlaybackLookupTotal,
	}
}

func MetricsHandler() gin.HandlerFunc {
	h := promhttp.Handler()
	return func(c *gin.Context) {
		h.ServeHTTP(c.Writer, c.Request)
	}
}

func TraceMiddleware(service string) gin.HandlerFunc {
	return otelgin.Middleware(service)
}
