package main

import (
	"context"
	"log"

	"dramaflow/backend/services/content-service/internal/bootstrap"
)

func main() {
	ctx := context.Background()
	server, shutdownTelemetry, err := bootstrap.Build(ctx)
	if err != nil {
		log.Fatal(err)
	}
	defer func() { _ = shutdownTelemetry(ctx) }()
	if err := server.ListenAndServe(); err != nil && err.Error() != "http: Server closed" {
		log.Fatal(err)
	}
}
