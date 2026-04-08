package logger

import (
	"os"
	"time"

	"github.com/rs/zerolog"
)

func New(service string, env string) zerolog.Logger {
	return zerolog.New(os.Stdout).
		With().
		Timestamp().
		Str("service", service).
		Str("env", env).
		Logger().
		Level(zerolog.InfoLevel)
}

func WithRequest(base zerolog.Logger, requestID string, traceID string) zerolog.Logger {
	return base.With().
		Str("request_id", requestID).
		Str("trace_id", traceID).
		Str("user_id", "").
		Str("admin_user_id", "").
		Str("purchase_token", "").
		Str("session_id", "").
		Str("episode_id", "").
		Str("drama_id", "").
		Str("action", "").
		Str("error_code", "").
		Time("ts", time.Now().UTC()).
		Logger()
}
