package gateway

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
)

type BillingClient struct {
	baseURL    string
	httpClient *http.Client
	pushSecret string
}

func NewBillingClient(baseURL string, pushSecret string) BillingClient {
	return BillingClient{
		baseURL:    strings.TrimRight(baseURL, "/"),
		httpClient: &http.Client{},
		pushSecret: pushSecret,
	}
}

func (c BillingClient) ResyncPurchase(ctx context.Context, purchaseToken string) error {
	request, _ := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+"/v1/internal/billing/purchases/"+purchaseToken+"/resync", nil)
	response, err := c.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode >= http.StatusBadRequest {
		raw, _ := io.ReadAll(response.Body)
		return fmt.Errorf("billing resync failed: %s", string(raw))
	}
	return nil
}

func (c BillingClient) ReplayRTDN(ctx context.Context, payload map[string]any) error {
	purchaseToken, _ := payload["purchaseToken"].(string)
	if purchaseToken == "" {
		return fmt.Errorf("billing replay rtdn failed: missing purchaseToken")
	}
	return c.ResyncPurchase(ctx, purchaseToken)
}

type EntitlementClient struct {
	baseURL    string
	httpClient *http.Client
}

func NewEntitlementClient(baseURL string) EntitlementClient {
	return EntitlementClient{
		baseURL:    strings.TrimRight(baseURL, "/"),
		httpClient: &http.Client{},
	}
}

func (c EntitlementClient) Recompute(ctx context.Context, userID string) error {
	request, _ := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+"/v1/internal/entitlements/users/"+userID+"/recompute", nil)
	response, err := c.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode >= http.StatusBadRequest {
		raw, _ := io.ReadAll(response.Body)
		return fmt.Errorf("entitlement recompute failed: %s", string(raw))
	}
	return nil
}

func (c EntitlementClient) Grant(ctx context.Context, payload map[string]any) error {
	return c.postJSON(ctx, "/v1/entitlements/grants", payload)
}

func (c EntitlementClient) Revoke(ctx context.Context, payload map[string]any) error {
	return c.postJSON(ctx, "/v1/entitlements/revoke", payload)
}

func (c EntitlementClient) postJSON(ctx context.Context, path string, payload map[string]any) error {
	body, _ := json.Marshal(payload)
	request, _ := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	request.Header.Set("Content-Type", "application/json")
	response, err := c.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode >= http.StatusBadRequest {
		raw, _ := io.ReadAll(response.Body)
		return fmt.Errorf("entitlement call failed: %s", string(raw))
	}
	return nil
}
