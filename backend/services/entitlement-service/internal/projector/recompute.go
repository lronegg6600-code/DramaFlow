package projector

import (
	"context"
	"time"

	"dramaflow/backend/services/entitlement-service/internal/domain"
	"dramaflow/backend/services/entitlement-service/internal/repository"
)

type RecomputeProjector struct {
	repo repository.Repository
}

func New(repo repository.Repository) RecomputeProjector {
	return RecomputeProjector{repo: repo}
}

func (p RecomputeProjector) Recompute(ctx context.Context, userID string) (domain.RecomputeResponse, error) {
	records, err := p.repo.ListPurchaseSnapshotsByUser(ctx, userID)
	if err != nil {
		return domain.RecomputeResponse{}, err
	}
	updated := 0
	for _, record := range records {
		state := "expired"
		switch record.PurchaseState {
		case "active", "grace", "hold", "paused":
			state = record.PurchaseState
		}
		startsAt := time.Now().UTC()
		if record.StartTime != nil {
			startsAt = *record.StartTime
		}
		request := domain.GrantRequest{
			UserID:              userID,
			EntitlementType:     "subscription",
			ProductID:           record.ProductID,
			ScopeType:           "global",
			State:               state,
			StartsAt:            startsAt.UTC().Format(time.RFC3339),
			SourcePurchaseToken: record.PurchaseToken,
			Reason:              "recompute",
			PayloadSnapshot:     map[string]any{"purchaseState": record.PurchaseState},
		}
		if record.ExpiryTime != nil {
			value := record.ExpiryTime.UTC().Format(time.RFC3339)
			request.EndsAt = &value
		}
		if _, _, err := p.repo.UpsertGrant(ctx, request); err != nil {
			return domain.RecomputeResponse{}, err
		}
		updated++
	}
	return domain.RecomputeResponse{
		UserID:           userID,
		ProcessedRecords: len(records),
		UpdatedRecords:   updated,
	}, nil
}
