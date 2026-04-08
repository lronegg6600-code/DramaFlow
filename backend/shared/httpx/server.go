package httpx

import (
	"context"
	"fmt"
	"net/http"

	"dramaflow/backend/shared/config"
	"github.com/gin-gonic/gin"
)

func NewServer(cfg config.Config, handler *gin.Engine) *http.Server {
	return &http.Server{
		Addr:         fmt.Sprintf(":%s", cfg.HTTP.Port),
		Handler:      handler,
		ReadTimeout:  cfg.HTTP.ReadTimeout,
		WriteTimeout: cfg.HTTP.WriteTimeout,
	}
}

func Shutdown(ctx context.Context, server *http.Server) error {
	return server.Shutdown(ctx)
}
