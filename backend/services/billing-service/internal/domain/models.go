package domain

type ClientContext struct {
	AppVersion string         `json:"appVersion,omitempty"`
	Country    string         `json:"country,omitempty"`
	SourcePage string         `json:"sourcePage,omitempty"`
	DebugInfo  map[string]any `json:"debugInfo,omitempty"`
}

type SyncPurchaseRequest struct {
	PurchaseToken string         `json:"purchaseToken" binding:"required"`
	ProductID     string         `json:"productId" binding:"required"`
	BasePlanID    *string        `json:"basePlanId,omitempty"`
	OfferID       *string        `json:"offerId,omitempty"`
	PackageName   string         `json:"packageName" binding:"required"`
	Source        string         `json:"source" binding:"required"`
	ClientContext *ClientContext `json:"clientContext,omitempty"`
}

type SyncPurchaseResponse struct {
	SyncAccepted           bool    `json:"syncAccepted"`
	PurchaseState          string  `json:"purchaseState"`
	AcknowledgementState   string  `json:"acknowledgementState"`
	EntitlementState       string  `json:"entitlementState"`
	EntitlementEffectiveAt *string `json:"entitlementEffectiveAt,omitempty"`
	NextAction             string  `json:"nextAction"`
	TraceID                string  `json:"traceId"`
}

type SubscriptionStatusResponse struct {
	UserID               string           `json:"userId"`
	PurchaseToken        *string          `json:"purchaseToken,omitempty"`
	ProductID            *string          `json:"productId,omitempty"`
	PurchaseState        string           `json:"purchaseState"`
	AcknowledgementState string           `json:"acknowledgementState"`
	EntitlementState     string           `json:"entitlementState"`
	Entitlements         []map[string]any `json:"entitlements"`
}

type VerifiedSubscription struct {
	PurchaseToken              string
	LinkedPurchaseToken        *string
	OrderID                    *string
	PackageName                string
	ProductID                  string
	BasePlanID                 *string
	OfferID                    *string
	PurchaseState              string
	AcknowledgementState       string
	ExternalAccountIdentifiers map[string]any
	ObfuscatedAccountID        *string
	ObfuscatedProfileID        *string
	StartTime                  *string
	ExpiryTime                 *string
	AutoRenewEnabled           *bool
	CancelReason               *string
	LineItemsRaw               []map[string]any
	RawPayload                 map[string]any
	RawAPIVersion              string
	UserID                     string
	AmountMicros               *int64
	CurrencyCode               *string
	CountryCode                *string
}

type PurchaseRecord struct {
	PurchaseToken        string
	LinkedPurchaseToken  *string
	OrderID              *string
	UserID               string
	PackageName          string
	ProductID            string
	BasePlanID           *string
	OfferID              *string
	PurchaseState        string
	AcknowledgementState string
	StartTime            *string
	ExpiryTime           *string
	AutoRenewEnabled     *bool
	CancelReason         *string
	Source               string
}

type RtdnEnvelope struct {
	Message struct {
		MessageID  string            `json:"messageId"`
		Data       string            `json:"data"`
		Attributes map[string]string `json:"attributes,omitempty"`
	} `json:"message"`
	Subscription string `json:"subscription,omitempty"`
}

type RtdnDomainEvent struct {
	MessageID                      string
	PackageName                    string
	EventType                      string
	PurchaseToken                  string
	SubscriptionNotificationType   *int
	OneTimeProductNotificationType *int
	EventTime                      string
	PayloadSnapshot                map[string]any
}
