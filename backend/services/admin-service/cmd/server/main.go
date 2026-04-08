package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	sharedhttp "dramaflow/backend/shared/httpx"
	"dramaflow/backend/services/admin-service/internal/bootstrap"
)

func main() {
	ctx := context.Background()
	server, shutdownTelemetry, err := bootstrap.Build(ctx)
	if err != nil {
		log.Fatalf("bootstrap admin-service: %v", err)
	}
	defer func() {
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = shutdownTelemetry(shutdownCtx)
	}()

	go func() {
		if err := server.ListenAndServe(); err != nil && err.Error() != "http: Server closed" {
			log.Fatalf("listen admin-service: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := sharedhttp.Shutdown(shutdownCtx, server); err != nil {
		log.Fatalf("shutdown admin-service: %v", err)
	}
}
