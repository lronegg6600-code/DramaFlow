package domain

type AdminUser struct {
	ID          string  `json:"id"`
	Email       string  `json:"email"`
	DisplayName string  `json:"displayName"`
	Role        string  `json:"role"`
	Status      string  `json:"status"`
	LastLoginAt *string `json:"lastLoginAt,omitempty"`
}

type AuthLoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=8"`
}

type AuthLoginResponse struct {
	AdminUser   AdminUser `json:"adminUser"`
	Permissions []string  `json:"permissions"`
	ExpiresAt   string    `json:"expiresAt"`
}

type Pagination struct {
	Page     int `json:"page"`
	PageSize int `json:"pageSize"`
	Total    int `json:"total"`
}

type DramaMutationRequest struct {
	Title            string   `json:"title" binding:"required"`
	ShortDescription string   `json:"shortDescription" binding:"required"`
	LongDescription  string   `json:"longDescription" binding:"required"`
	PosterURL        string   `json:"posterUrl" binding:"required"`
	CoverURL         string   `json:"coverUrl" binding:"required"`
	Tags             []string `json:"tags"`
	Region           string   `json:"region" binding:"required"`
	Language         string   `json:"language" binding:"required"`
	PublishStatus    string   `json:"publishStatus" binding:"required"`
	IsFeatured       bool     `json:"isFeatured"`
}

type DramaRecord struct {
	ID string `json:"id"`
	DramaMutationRequest
}

type DramaListResponse struct {
	Items      []DramaRecord `json:"items"`
	Pagination Pagination    `json:"pagination"`
}

type EpisodeMutationRequest struct {
	Title                string `json:"title" binding:"required"`
	Description          string `json:"description" binding:"required"`
	EpisodeNo            int    `json:"episodeNo" binding:"required"`
	DurationSeconds      int    `json:"durationSeconds" binding:"required"`
	PreviewSeconds       int    `json:"previewSeconds" binding:"required"`
	IsPremium            bool   `json:"isPremium"`
	PublishStatus        string `json:"publishStatus" binding:"required"`
	SortOrder            int    `json:"sortOrder" binding:"required"`
	StreamKeyPlaceholder string `json:"streamKeyPlaceholder" binding:"required"`
}

type EpisodeRecord struct {
	ID      string `json:"id"`
	DramaID string `json:"dramaId"`
	EpisodeMutationRequest
}

type FeedHomeConfig struct {
	ID               string         `json:"id"`
	Region           string         `json:"region"`
	Language         string         `json:"language"`
	ConfigVersion    int            `json:"configVersion"`
	DraftPayload     map[string]any `json:"draftPayload"`
	PublishedPayload map[string]any `json:"publishedPayload"`
	PublishedAt      *string        `json:"publishedAt,omitempty"`
	PublishedBy      *string        `json:"publishedBy,omitempty"`
}

type FeedConfigMutationRequest struct {
	Region       string         `json:"region" binding:"required"`
	Language     string         `json:"language" binding:"required"`
	DraftPayload map[string]any `json:"draftPayload" binding:"required"`
}

type UserSummary struct {
	ID                string `json:"id"`
	AnonymousDeviceID string `json:"anonymousDeviceId"`
	Status            string `json:"status"`
	CreatedAt         string `json:"createdAt"`
}

type UserSummaryResponse struct {
	Items      []UserSummary `json:"items"`
	Pagination Pagination    `json:"pagination"`
}

type PurchaseRecord struct {
	PurchaseToken        string         `json:"purchaseToken"`
	UserID               string         `json:"userId"`
	ProductID            string         `json:"productId"`
	BasePlanID           *string        `json:"basePlanId,omitempty"`
	OfferID              *string        `json:"offerId,omitempty"`
	OrderID              *string        `json:"orderId,omitempty"`
	PackageName          string         `json:"packageName"`
	PurchaseState        string         `json:"purchaseState"`
	AcknowledgementState string         `json:"acknowledgementState"`
	StartTime            *string        `json:"startTime,omitempty"`
	ExpiryTime           *string        `json:"expiryTime,omitempty"`
	Source               string         `json:"source"`
	RawPayload           map[string]any `json:"rawPayload,omitempty"`
}

type PurchaseSearchResponse struct {
	Items      []PurchaseRecord `json:"items"`
	Pagination Pagination       `json:"pagination"`
}

type EntitlementRecord struct {
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

type EntitlementSearchResponse struct {
	Items      []EntitlementRecord `json:"items"`
	Pagination Pagination          `json:"pagination"`
}

type RTDNEventRecord struct {
	MessageID       string         `json:"messageId"`
	PackageName     string         `json:"packageName"`
	EventType       string         `json:"eventType"`
	PurchaseToken   string         `json:"purchaseToken"`
	EventTime       string         `json:"eventTime"`
	ProcessedState  string         `json:"processedState"`
	ProcessedAt     *string        `json:"processedAt,omitempty"`
	ErrorMessage    *string        `json:"errorMessage,omitempty"`
	PayloadSnapshot map[string]any `json:"payloadSnapshot"`
}

type RTDNEventResponse struct {
	Items      []RTDNEventRecord `json:"items"`
	Pagination Pagination        `json:"pagination"`
}

type PlaybackSessionRecord struct {
	SessionID      string  `json:"sessionId"`
	UserID         string  `json:"userId"`
	DramaID        string  `json:"dramaId"`
	EpisodeID      string  `json:"episodeId"`
	PlaybackMode   string  `json:"playbackMode"`
	SessionStatus  string  `json:"sessionStatus"`
	IssuedAt       string  `json:"issuedAt"`
	ExpiresAt      string  `json:"expiresAt"`
	CompletedAt    *string `json:"completedAt,omitempty"`
	ClientPlatform string  `json:"clientPlatform"`
	ClientVersion  string  `json:"clientVersion"`
}

type PlaybackSessionResponse struct {
	Items      []PlaybackSessionRecord `json:"items"`
	Pagination Pagination              `json:"pagination"`
}

type AuditLogRecord struct {
	ID             string         `json:"id"`
	AdminUserID    string         `json:"adminUserId"`
	Action         string         `json:"action"`
	ResourceType   string         `json:"resourceType"`
	ResourceID     string         `json:"resourceId"`
	RequestID      string         `json:"requestId"`
	TraceID        string         `json:"traceId"`
	Success        bool           `json:"success"`
	BeforeSnapshot map[string]any `json:"beforeSnapshot,omitempty"`
	AfterSnapshot  map[string]any `json:"afterSnapshot,omitempty"`
	Metadata       map[string]any `json:"metadata,omitempty"`
	CreatedAt      string         `json:"createdAt"`
}

type AuditLogResponse struct {
	Items      []AuditLogRecord `json:"items"`
	Pagination Pagination       `json:"pagination"`
}

type DashboardStats struct {
	DramaTotal             int `json:"dramaTotal"`
	PublishedDramaTotal    int `json:"publishedDramaTotal"`
	TodayPurchaseSyncTotal int `json:"todayPurchaseSyncTotal"`
	TodayRTDNEventTotal    int `json:"todayRtdnEventTotal"`
	ActiveEntitlementTotal int `json:"activeEntitlementTotal"`
	Playback24hTotal       int `json:"playback24hTotal"`
}

type GrantTempRequest struct {
	ProductID string  `json:"productId" binding:"required"`
	EndsAt    *string `json:"endsAt,omitempty"`
	Reason    string  `json:"reason" binding:"required"`
}

type RevokeEntitlementRequest struct {
	SourcePurchaseToken string `json:"sourcePurchaseToken" binding:"required"`
	Reason              string `json:"reason" binding:"required"`
}
