package rtdn

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"time"

	"dramaflow/backend/services/billing-service/internal/domain"
)

type Parser struct{}

func NewParser() Parser {
	return Parser{}
}

func (p Parser) Parse(_ context.Context, envelope domain.RtdnEnvelope, rawBody []byte) (domain.RtdnDomainEvent, error) {
	messageID := envelope.Message.MessageID
	payload := map[string]any{}
	if envelope.Message.Data != "" {
		decoded, err := base64.StdEncoding.DecodeString(envelope.Message.Data)
		if err != nil {
			return domain.RtdnDomainEvent{}, fmt.Errorf("decode rtdn payload: %w", err)
		}
		if err := json.Unmarshal(decoded, &payload); err != nil {
			return domain.RtdnDomainEvent{}, fmt.Errorf("unmarshal rtdn payload: %w", err)
		}
	} else if len(rawBody) > 0 {
		if err := json.Unmarshal(rawBody, &payload); err != nil {
			return domain.RtdnDomainEvent{}, fmt.Errorf("unmarshal raw rtdn payload: %w", err)
		}
	}

	event := domain.RtdnDomainEvent{
		MessageID:       messageID,
		PackageName:     readString(payload, "packageName"),
		PurchaseToken:   readStringFromNested(payload, "subscriptionNotification", "purchaseToken"),
		EventTime:       time.Now().UTC().Format(time.RFC3339),
		PayloadSnapshot: payload,
		EventType:       "subscription_notification",
	}
	if value := readIntFromNested(payload, "subscriptionNotification", "notificationType"); value != nil {
		event.SubscriptionNotificationType = value
	}
	if oneTime := readIntFromNested(payload, "oneTimeProductNotification", "notificationType"); oneTime != nil {
		event.OneTimeProductNotificationType = oneTime
		event.EventType = "one_time_product_notification"
		event.PurchaseToken = readStringFromNested(payload, "oneTimeProductNotification", "purchaseToken")
	}
	if event.PurchaseToken == "" {
		event.PurchaseToken = readString(payload, "purchaseToken")
	}
	return event, nil
}

func readString(source map[string]any, key string) string {
	value, _ := source[key].(string)
	return value
}

func readStringFromNested(source map[string]any, parent string, key string) string {
	nested, _ := source[parent].(map[string]any)
	if nested == nil {
		return ""
	}
	value, _ := nested[key].(string)
	return value
}

func readIntFromNested(source map[string]any, parent string, key string) *int {
	nested, _ := source[parent].(map[string]any)
	if nested == nil {
		return nil
	}
	switch value := nested[key].(type) {
	case float64:
		cast := int(value)
		return &cast
	case int:
		return &value
	default:
		return nil
	}
}
