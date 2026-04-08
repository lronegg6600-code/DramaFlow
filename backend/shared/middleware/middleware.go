package middleware

import (
	"net/http"
	"strings"
	"time"

	"dramaflow/backend/shared/auth"
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
