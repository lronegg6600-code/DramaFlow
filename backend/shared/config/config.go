package config

import (
	"fmt"
	"os"
	"time"
)

type HTTPConfig struct {
	Port         string
	ReadTimeout  time.Duration
	WriteTimeout time.Duration
	HandlerTimeout time.Duration
	MaxBodyBytes int64
	MaxInflight int
}

type JWTConfig struct {
	Secret          string
	Issuer          string
	AccessTokenTTL  time.Duration
	RefreshTokenTTL time.Duration
}

type Config struct {
	ServiceName  string
	Environment  string
	GinMode      string
	PostgresDSN  string
	RedisAddr    string
	OTLPEndpoint string
	HTTP         HTTPConfig
	JWT          JWTConfig
	Playback     PlaybackConfig
	Services     ServicesConfig
	Billing      BillingConfig
	Entitlement  EntitlementConfig
	Admin        AdminConfig
}

type PlaybackConfig struct {
	SignerMode               string
	URLTTL                   time.Duration
	HeartbeatInterval        time.Duration
	RefreshBefore            time.Duration
	CDNBaseURL               string
	CloudFrontKeyGroupID     string
	CloudFrontPrivateKeyPEM  string
	CloudFrontKeyPairID      string
}

type ServicesConfig struct {
	EntitlementServiceBaseURL string
	BillingServiceBaseURL     string
	ContentServiceBaseURL     string
	FeedServiceBaseURL        string
	PlaybackServiceBaseURL    string
	AdminServiceBaseURL       string
}

type BillingConfig struct {
	GooglePlayPackageName        string
	GooglePlayServiceAccountJSON string
	GooglePlayPublisherScope     string
	SyncAckMode                  string
	RTDNEnabled                  bool
	RTDNPushSecret               string
}

type EntitlementConfig struct {
	DefaultPreviewSeconds int
	GracePolicyEnabled    bool
}

type AdminConfig struct {
	SessionSecret      string
	BootstrapEmail     string
	BootstrapPassword  string
	AllowedOrigins     []string
	EnableDevOperations bool
}

func Load(serviceName string, defaultPort string) (Config, error) {
	readTimeout, err := time.ParseDuration(getEnv("DRAMAFLOW_HTTP_READ_TIMEOUT", "10s"))
	if err != nil {
		return Config{}, fmt.Errorf("parse read timeout: %w", err)
	}

	writeTimeout, err := time.ParseDuration(getEnv("DRAMAFLOW_HTTP_WRITE_TIMEOUT", "10s"))
	if err != nil {
		return Config{}, fmt.Errorf("parse write timeout: %w", err)
	}
	handlerTimeout, err := time.ParseDuration(getEnv("DRAMAFLOW_HTTP_HANDLER_TIMEOUT", "8s"))
	if err != nil {
		return Config{}, fmt.Errorf("parse handler timeout: %w", err)
	}
	maxBodyBytes, err := parseInt64Env("DRAMAFLOW_HTTP_MAX_BODY_BYTES", 1048576)
	if err != nil {
		return Config{}, fmt.Errorf("parse max body bytes: %w", err)
	}
	maxInflight, err := parseIntEnv("DRAMAFLOW_HTTP_MAX_INFLIGHT", 1000)
	if err != nil {
		return Config{}, fmt.Errorf("parse max inflight: %w", err)
	}

	accessTTL, err := time.ParseDuration(getEnv("DRAMAFLOW_ACCESS_TOKEN_TTL", "15m"))
	if err != nil {
		return Config{}, fmt.Errorf("parse access token ttl: %w", err)
	}

	refreshTTL, err := time.ParseDuration(getEnv("DRAMAFLOW_REFRESH_TOKEN_TTL", "720h"))
	if err != nil {
		return Config{}, fmt.Errorf("parse refresh token ttl: %w", err)
	}

	playbackTTL, err := time.ParseDuration(fmt.Sprintf("%ss", getEnv("DRAMAFLOW_PLAYBACK_URL_TTL_SECONDS", "300")))
	if err != nil {
		return Config{}, fmt.Errorf("parse playback url ttl: %w", err)
	}

	heartbeatInterval, err := time.ParseDuration(fmt.Sprintf("%ss", getEnv("DRAMAFLOW_PLAYBACK_HEARTBEAT_INTERVAL_SECONDS", "15")))
	if err != nil {
		return Config{}, fmt.Errorf("parse playback heartbeat interval: %w", err)
	}

	refreshBefore, err := time.ParseDuration(fmt.Sprintf("%ss", getEnv("DRAMAFLOW_PLAYBACK_REFRESH_BEFORE_SECONDS", "45")))
	if err != nil {
		return Config{}, fmt.Errorf("parse playback refresh before: %w", err)
	}

	defaultPreviewSeconds, err := parseIntEnv("DRAMAFLOW_ENTITLEMENT_DEFAULT_PREVIEW_SECONDS", 15)
	if err != nil {
		return Config{}, fmt.Errorf("parse entitlement default preview seconds: %w", err)
	}

	cfg := Config{
		ServiceName:  serviceName,
		Environment:  getEnv("DRAMAFLOW_ENV", "local"),
		GinMode:      getEnv("DRAMAFLOW_GIN_MODE", "debug"),
		PostgresDSN:  getEnv("DRAMAFLOW_POSTGRES_DSN", ""),
		RedisAddr:    getEnv("DRAMAFLOW_REDIS_ADDR", ""),
		OTLPEndpoint: getEnv("DRAMAFLOW_OTEL_ENDPOINT", ""),
		HTTP: HTTPConfig{
			Port:         getEnv(fmt.Sprintf("DRAMAFLOW_%s_PORT", serviceEnvKey(serviceName)), defaultPort),
			ReadTimeout:  readTimeout,
			WriteTimeout: writeTimeout,
			HandlerTimeout: handlerTimeout,
			MaxBodyBytes: maxBodyBytes,
			MaxInflight: maxInflight,
		},
		JWT: JWTConfig{
			Secret:          getEnv("DRAMAFLOW_JWT_SECRET", "replace-with-local-secret"),
			Issuer:          getEnv("DRAMAFLOW_JWT_ISSUER", "dramaflow.local"),
			AccessTokenTTL:  accessTTL,
			RefreshTokenTTL: refreshTTL,
		},
		Playback: PlaybackConfig{
			SignerMode:              getEnv("DRAMAFLOW_PLAYBACK_SIGNER_MODE", "dev_passthrough"),
			URLTTL:                  playbackTTL,
			HeartbeatInterval:       heartbeatInterval,
			RefreshBefore:           refreshBefore,
			CDNBaseURL:              getEnv("DRAMAFLOW_PLAYBACK_CDN_BASE_URL", "https://storage.googleapis.com/exoplayer-test-media-0"),
			CloudFrontKeyGroupID:    getEnv("DRAMAFLOW_PLAYBACK_CLOUDFRONT_KEY_GROUP_ID", ""),
			CloudFrontPrivateKeyPEM: getEnv("DRAMAFLOW_PLAYBACK_CLOUDFRONT_PRIVATE_KEY_PEM", ""),
			CloudFrontKeyPairID:     getEnv("DRAMAFLOW_PLAYBACK_CLOUDFRONT_KEY_PAIR_ID", ""),
		},
		Services: ServicesConfig{
			EntitlementServiceBaseURL: getEnv("DRAMAFLOW_ENTITLEMENT_SERVICE_BASE_URL", "http://entitlement-service:8086"),
			BillingServiceBaseURL:     getEnv("DRAMAFLOW_BILLING_SERVICE_BASE_URL", "http://billing-service:8087"),
			ContentServiceBaseURL:     getEnv("DRAMAFLOW_CONTENT_SERVICE_BASE_URL", "http://content-service:8082"),
			FeedServiceBaseURL:        getEnv("DRAMAFLOW_FEED_SERVICE_BASE_URL", "http://feed-service:8083"),
			PlaybackServiceBaseURL:    getEnv("DRAMAFLOW_PLAYBACK_SERVICE_BASE_URL", "http://playback-service:8085"),
			AdminServiceBaseURL:       getEnv("DRAMAFLOW_ADMIN_SERVICE_BASE_URL", "http://admin-service:8088"),
		},
		Billing: BillingConfig{
			GooglePlayPackageName:        getEnv("GOOGLE_PLAY_PACKAGE_NAME", ""),
			GooglePlayServiceAccountJSON: getEnv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", ""),
			GooglePlayPublisherScope:     getEnv("GOOGLE_PLAY_PUBLISHER_SCOPE", "https://www.googleapis.com/auth/androidpublisher"),
			SyncAckMode:                  getEnv("BILLING_SYNC_ACK_MODE", "record_only"),
			RTDNEnabled:                  getEnv("BILLING_RTDN_ENABLED", "false") == "true",
			RTDNPushSecret:               getEnv("BILLING_RTDN_PUSH_SECRET", ""),
		},
		Entitlement: EntitlementConfig{
			DefaultPreviewSeconds: defaultPreviewSeconds,
			GracePolicyEnabled:    getEnv("ENTITLEMENT_GRACE_POLICY_ENABLED", "true") == "true",
		},
		Admin: AdminConfig{
			SessionSecret:      getEnv("ADMIN_SESSION_SECRET", "replace-with-admin-session-secret"),
			BootstrapEmail:     getEnv("ADMIN_BOOTSTRAP_EMAIL", "admin@dramaflow.local"),
			BootstrapPassword:  getEnv("ADMIN_BOOTSTRAP_PASSWORD", "change-me-now"),
			AllowedOrigins:     splitCSVEnv("ADMIN_ALLOWED_ORIGINS", "http://localhost:3000"),
			EnableDevOperations: getEnv("ADMIN_ENABLE_DEV_OPERATIONS", "false") == "true",
		},
	}

	if cfg.PostgresDSN == "" {
		return Config{}, fmt.Errorf("DRAMAFLOW_POSTGRES_DSN is required")
	}

	return cfg, nil
}

func getEnv(key string, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

func serviceEnvKey(serviceName string) string {
	out := ""
	for _, ch := range serviceName {
		switch ch {
		case '-':
			out += "_"
		default:
			if ch >= 'a' && ch <= 'z' {
				out += string(ch - 32)
			} else {
				out += string(ch)
			}
		}
	}
	return out
}

func parseIntEnv(key string, fallback int) (int, error) {
	value := getEnv(key, fmt.Sprintf("%d", fallback))
	var parsed int
	if _, err := fmt.Sscanf(value, "%d", &parsed); err != nil {
		return 0, err
	}
	return parsed, nil
}

func parseInt64Env(key string, fallback int64) (int64, error) {
	value := getEnv(key, fmt.Sprintf("%d", fallback))
	var parsed int64
	if _, err := fmt.Sscanf(value, "%d", &parsed); err != nil {
		return 0, err
	}
	return parsed, nil
}

func splitCSVEnv(key string, fallback string) []string {
	value := getEnv(key, fallback)
	items := make([]string, 0)
	current := ""
	for _, ch := range value {
		if ch == ',' {
			if current != "" {
				items = append(items, current)
			}
			current = ""
			continue
		}
		if ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t' {
			continue
		}
		current += string(ch)
	}
	if current != "" {
		items = append(items, current)
	}
	if len(items) == 0 {
		return []string{fallback}
	}
	return items
}
