package middleware

import (
	"context"
	"net/http"
	"strings"
	"time"

	"dramaflow/backend/shared/auth"
	"dramaflow/backend/shared/config"
	"dramaflow/backend/shared/errors"
	"dramaflow/backend/shared/logger"
	"dramaflow/backend/shared/response"
	"dramaflow/backend/shared/telemetry"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rs/zerolog"
)

const (
	ContextRequestIDKey   = "request_id"
	ContextTraceIDKey     = "trace_id"
	ContextUserIDKey      = "user_id"
	ContextAdminUserIDKey = "admin_user_id"
	ContextActionKey      = "action"
	ContextErrorCodeKey   = "error_code"
)

func Recovery() gin.HandlerFunc {
	return gin.CustomRecovery(func(c *gin.Context, recovered any) {
		response.Fail(c, errors.ErrInternal)
	})
}

func CORS() gin.HandlerFunc {
	return cors.New(cors.Config{
		AllowOrigins: []string{"*"},
		AllowMethods: []string{"GET", "POST", "PUT", "OPTIONS"},
		AllowHeaders: []string{"Authorization", "Content-Type", "X-Request-Id"},
	})
}

func RequestContext() gin.HandlerFunc {
	return func(c *gin.Context) {
		requestID := c.GetHeader("X-Request-Id")
		if requestID == "" {
			requestID = uuid.NewString()
		}

		traceID := c.Request.Header.Get("Traceparent")
		c.Set(ContextRequestIDKey, requestID)
		c.Set(ContextTraceIDKey, traceID)
		c.Writer.Header().Set("X-Request-Id", requestID)
		c.Next()
	}
}

func BodySizeLimit(maxBodyBytes int64) gin.HandlerFunc {
	return func(c *gin.Context) {
		limit := maxBodyBytes
		if limit <= 0 {
			limit = 1048576
		}
		c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, limit)
		c.Next()
	}
}

func InflightLimit(maxInflight int) gin.HandlerFunc {
	limit := maxInflight
	if limit <= 0 {
		limit = 1000
	}
	semaphore := make(chan struct{}, limit)
	return func(c *gin.Context) {
		select {
		case semaphore <- struct{}{}:
			defer func() { <-semaphore }()
			c.Next()
		default:
			c.Header("Retry-After", "1")
			response.Fail(c, errors.New(http.StatusTooManyRequests, "server.too_many_requests", "Server is busy, please retry shortly."))
			c.Abort()
		}
	}
}

func HandlerTimeout(timeout time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		t := timeout
		if t <= 0 {
			t = 8 * time.Second
		}
		ctx, cancel := context.WithTimeout(c.Request.Context(), t)
		defer cancel()
		c.Request = c.Request.WithContext(ctx)
		c.Next()
		if ctx.Err() == context.DeadlineExceeded && !c.Writer.Written() {
			response.Fail(c, errors.New(http.StatusGatewayTimeout, "server.request_timeout", "Request processing exceeded timeout."))
			c.Abort()
		}
	}
}

func ServiceGuards(cfg config.Config) []gin.HandlerFunc {
	return []gin.HandlerFunc{
		BodySizeLimit(cfg.HTTP.MaxBodyBytes),
		InflightLimit(cfg.HTTP.MaxInflight),
		HandlerTimeout(cfg.HTTP.HandlerTimeout),
	}
}

func Logging(base zerolog.Logger, metrics telemetry.Metrics, service string) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()

		requestID, _ := c.Get(ContextRequestIDKey)
		traceID, _ := c.Get(ContextTraceIDKey)
		action, _ := c.Get(ContextActionKey)
		entry := logger.WithRequest(base, toString(requestID), toString(traceID)).With().
			Str("user_id", UserID(c)).
			Str("admin_user_id", AdminUserID(c)).
			Str("action", toString(action)).
			Str("error_code", ErrorCode(c)).
			Logger()
		entry.Info().
			Str("method", c.Request.Method).
			Str("path", c.FullPath()).
			Int("status", c.Writer.Status()).
			Dur("latency", time.Since(start)).
			Msg("request completed")

		route := c.FullPath()
		if route == "" {
			route = c.Request.URL.Path
		}
		status := http.StatusText(c.Writer.Status())
		metrics.RequestCount.WithLabelValues(service, c.Request.Method, route, status).Inc()
		metrics.RequestLatency.WithLabelValues(service, c.Request.Method, route).Observe(time.Since(start).Seconds())
		if ErrorCode(c) != "" {
			metrics.ErrorCount.WithLabelValues(service, route, ErrorCode(c)).Inc()
		}
	}
}

func OptionalAuth(tokens auth.TokenManager) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if strings.HasPrefix(header, "Bearer ") {
			claims, err := tokens.ParseAccessToken(strings.TrimPrefix(header, "Bearer "))
			if err == nil {
				c.Set(ContextUserIDKey, claims.UserID)
			}
		}
		c.Next()
	}
}

func RequireAuth(tokens auth.TokenManager) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			response.Fail(c, errors.ErrUnauthorized)
			c.Abort()
			return
		}

		claims, err := tokens.ParseAccessToken(strings.TrimPrefix(header, "Bearer "))
		if err != nil {
			response.Fail(c, errors.ErrUnauthorized)
			c.Abort()
			return
		}

		c.Set(ContextUserIDKey, claims.UserID)
		c.Next()
	}
}

func UserID(c *gin.Context) string {
	value, _ := c.Get(ContextUserIDKey)
	return toString(value)
}

func AdminUserID(c *gin.Context) string {
	value, _ := c.Get(ContextAdminUserIDKey)
	return toString(value)
}

func ErrorCode(c *gin.Context) string {
	value, _ := c.Get(ContextErrorCodeKey)
	return toString(value)
}

func toString(value any) string {
	if value == nil {
		return ""
	}
	if cast, ok := value.(string); ok {
		return cast
	}
	return ""
}
