package http

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	sharedresponse "dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/billing-service/internal/rtdn"
	"dramaflow/backend/services/billing-service/internal/service"
	"github.com/gin-gonic/gin"
)

func TestSyncPurchaseReturnsValidationEnvelopeOnBadJSON(t *testing.T) {
	gin.SetMode(gin.TestMode)

	recorder := httptest.NewRecorder()
	ctx, _ := gin.CreateTestContext(recorder)
	ctx.Request = httptest.NewRequest(http.MethodPost, "/v1/billing/google-play/purchases:sync", strings.NewReader("{"))
	ctx.Request.Header.Set("Content-Type", "application/json")

	handler := New(service.Service{}, rtdn.Parser{}, sharedtelemetry.NewMetrics("billing-service-test"), "", true, gin.TestMode)
	handler.syncPurchase(ctx)

	if recorder.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", recorder.Code)
	}

	var envelope sharedresponse.Envelope
	if err := json.Unmarshal(recorder.Body.Bytes(), &envelope); err != nil {
		t.Fatalf("unmarshal response: %v", err)
	}
	if envelope.Error == nil || envelope.Error.Code != "request.invalid" {
		t.Fatalf("unexpected error envelope: %#v", envelope.Error)
	}
}
