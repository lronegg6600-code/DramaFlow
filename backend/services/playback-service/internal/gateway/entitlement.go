package gateway

import (
	"encoding/json"
	"context"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	"dramaflow/backend/services/playback-service/internal/domain"
	"dramaflow/backend/services/playback-service/internal/repository"
)

type EntitlementGateway interface {
	ResolveAccess(ctx context.Context, userID string, record domain.EpisodeRecord) (domain.EntitlementAccess, error)
}

type DevEntitlementGateway struct {
	repo repository.Repository
}

func NewDevEntitlementGateway(repo repository.Repository) DevEntitlementGateway {
	return DevEntitlementGateway{repo: repo}
}

func (g DevEntitlementGateway) ResolveAccess(ctx context.Context, userID string, record domain.EpisodeRecord) (domain.EntitlementAccess, error) {
	if !record.IsPremium {
		return domain.EntitlementFullAccess, nil
	}

	entitlements, err := g.repo.FindDevEntitlements(ctx, userID, record.DramaID, record.EpisodeID)
	if err != nil {
		return domain.EntitlementNone, err
	}
	for _, entitlementType := range entitlements {
		if entitlementType == "premium" || entitlementType == "full_access" {
			return domain.EntitlementFullAccess, nil
		}
	}
	if record.PreviewSeconds > 0 {
		return domain.EntitlementPreviewOnly, nil
	}
	return domain.EntitlementNone, nil
}

type RemoteEntitlementGateway struct {
	baseURL string
	client  *http.Client
}

func NewRemoteEntitlementGateway(baseURL string) RemoteEntitlementGateway {
	return RemoteEntitlementGateway{
		baseURL: strings.TrimRight(baseURL, "/"),
		client:  &http.Client{Timeout: 10 * time.Second},
	}
}

func (g RemoteEntitlementGateway) ResolveAccess(ctx context.Context, userID string, record domain.EpisodeRecord) (domain.EntitlementAccess, error) {
	if !record.IsPremium {
		return domain.EntitlementFullAccess, nil
	}
	query := url.Values{}
	query.Set("episodeId", record.EpisodeID)
	query.Set("dramaId", record.DramaID)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, fmt.Sprintf("%s/v1/internal/entitlements/users/%s/playback-access?%s", g.baseURL, userID, query.Encode()), nil)
	if err != nil {
		return domain.EntitlementNone, err
	}
	resp, err := g.client.Do(req)
	if err != nil {
		return domain.EntitlementNone, err
	}
	defer resp.Body.Close()
	var envelope struct {
		Data struct {
			AccessLevel string `json:"accessLevel"`
		} `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		return domain.EntitlementNone, err
	}
	switch envelope.Data.AccessLevel {
	case "full_access":
		return domain.EntitlementFullAccess, nil
	case "preview_only":
		return domain.EntitlementPreviewOnly, nil
	default:
		if record.PreviewSeconds > 0 {
			return domain.EntitlementPreviewOnly, nil
		}
		return domain.EntitlementNone, nil
	}
}
