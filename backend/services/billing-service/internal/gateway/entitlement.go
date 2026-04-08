package gateway

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type EntitlementServiceClient struct {
	baseURL string
	client  *http.Client
}

func NewEntitlementServiceClient(baseURL string) EntitlementServiceClient {
	return EntitlementServiceClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		client:  &http.Client{Timeout: 10 * time.Second},
	}
}

func (c EntitlementServiceClient) Grant(ctx context.Context, payload map[string]any) error {
	return c.post(ctx, "/v1/entitlements/grants", payload)
}

func (c EntitlementServiceClient) Revoke(ctx context.Context, payload map[string]any) error {
	return c.post(ctx, "/v1/entitlements/revoke", payload)
}

func (c EntitlementServiceClient) Me(ctx context.Context, accessToken string) (map[string]any, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/v1/entitlements/me", nil)
	if err != nil {
		return nil, err
	}
	if accessToken != "" {
		req.Header.Set("Authorization", accessToken)
	}
	resp, err := c.client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var envelope struct {
		Data map[string]any `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		return nil, err
	}
	return envelope.Data, nil
}

func (c EntitlementServiceClient) PlaybackAccess(ctx context.Context, userID string, episodeID string, dramaID *string) (map[string]any, error) {
	query := url.Values{}
	query.Set("episodeId", episodeID)
	if dramaID != nil {
		query.Set("dramaId", *dramaID)
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, fmt.Sprintf("%s/v1/internal/entitlements/users/%s/playback-access?%s", c.baseURL, userID, query.Encode()), nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var envelope struct {
		Data map[string]any `json:"data"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		return nil, err
	}
	return envelope.Data, nil
}

func (c EntitlementServiceClient) post(ctx context.Context, path string, payload map[string]any) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("entitlement service returned %d", resp.StatusCode)
	}
	return nil
}
