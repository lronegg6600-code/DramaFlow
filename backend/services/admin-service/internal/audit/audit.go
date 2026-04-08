package audit

import (
	"context"

	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/admin-service/internal/domain"
)

type Writer interface {
	Write(ctx context.Context, item domain.AuditLogRecord) error
}

type MetricsWriter struct {
	repo    Writer
	metrics sharedtelemetry.Metrics
}

func New(repo Writer, metrics sharedtelemetry.Metrics) MetricsWriter {
	return MetricsWriter{repo: repo, metrics: metrics}
}

func (w MetricsWriter) Write(ctx context.Context, item domain.AuditLogRecord) error {
	if err := w.repo.Write(ctx, item); err != nil {
		return err
	}
	w.metrics.AdminAuditLogWriteTotal.Inc()
	return nil
}
