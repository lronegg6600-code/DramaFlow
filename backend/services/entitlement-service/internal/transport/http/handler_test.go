package http

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	sharedresponse "dramaflow/backend/shared/response"
	sharedtelemetry "dramaflow/backend/shared/telemetry"
	"dramaflow/backend/services/entitlement-service/internal/service"
	"github.com/gin-gonic/gin"
)

func TestGrantReturnsValidationEnvelopeOnBadJSON(t *testing.T) {
	gin.SetMode(gin.TestMode)

	recorder := httptest.NewRecorder()
	ctx, _ := gin.CreateTestContext(recorder)
	ctx.Request = httptest.NewRequest(http.MethodPost, "/v1/entitlements/grants", strings.NewReader("{"))
	ctx.Request.Header.Set("Content-Type", "application/json")

	handler := New(service.Service{}, sharedtelemetry.NewMetrics("entitlement-service-test"), gin.TestMode)
	handler.grant(ctx)

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
