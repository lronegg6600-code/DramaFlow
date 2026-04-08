package domain

type Entitlement struct {
	EntitlementID       string  `json:"entitlementId"`
	UserID              string  `json:"userId"`
	EntitlementType     string  `json:"entitlementType"`
	ProductID           string  `json:"productId"`
	ScopeType           string  `json:"scopeType"`
	ScopeRef            *string `json:"scopeRef,omitempty"`
	State               string  `json:"state"`
	StartsAt            string  `json:"startsAt"`
	EndsAt              *string `json:"endsAt,omitempty"`
	SourcePurchaseToken string  `json:"sourcePurchaseToken"`
	LastSyncedAt        string  `json:"lastSyncedAt"`
}

type MeResponse struct {
	UserID        string        `json:"userId"`
	Entitlements  []Entitlement `json:"entitlements"`
	IsPremium     bool          `json:"isPremium"`
	ActiveProduct *string       `json:"activeProductId,omitempty"`
	Source        string        `json:"source"`
}

type PlaybackAccessResponse struct {
	AccessLevel       string  `json:"accessLevel"`
	EntitlementSource string  `json:"entitlementSource"`
	PreviewSeconds    *int    `json:"previewSeconds,omitempty"`
	ExpiresAt         *string `json:"expiresAt,omitempty"`
	Reason            string  `json:"reason"`
}

type PlaybackAccessQuery struct {
	EpisodeID string  `json:"episodeId" form:"episodeId" binding:"required"`
	DramaID   *string `json:"dramaId" form:"dramaId"`
}

type GrantRequest struct {
	UserID              string         `json:"userId" binding:"required"`
	EntitlementType     string         `json:"entitlementType" binding:"required"`
	ProductID           string         `json:"productId" binding:"required"`
	ScopeType           string         `json:"scopeType" binding:"required"`
	ScopeRef            *string        `json:"scopeRef,omitempty"`
	State               string         `json:"state" binding:"required"`
	StartsAt            string         `json:"startsAt" binding:"required"`
	EndsAt              *string        `json:"endsAt,omitempty"`
	SourcePurchaseToken string         `json:"sourcePurchaseToken" binding:"required"`
	Reason              string         `json:"reason" binding:"required"`
	PayloadSnapshot     map[string]any `json:"payloadSnapshot,omitempty"`
}

type RevokeRequest struct {
	UserID              string         `json:"userId" binding:"required"`
	SourcePurchaseToken string         `json:"sourcePurchaseToken" binding:"required"`
	State               string         `json:"state" binding:"required"`
	Reason              string         `json:"reason" binding:"required"`
	PayloadSnapshot     map[string]any `json:"payloadSnapshot,omitempty"`
}

type RecomputeResponse struct {
	UserID           string `json:"userId"`
	ProcessedRecords int    `json:"processedRecords"`
	UpdatedRecords   int    `json:"updatedRecords"`
}
