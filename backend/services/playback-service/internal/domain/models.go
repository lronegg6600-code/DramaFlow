package domain

type PlaybackMode string

const (
	PlaybackModePreview PlaybackMode = "preview"
	PlaybackModeFull    PlaybackMode = "full"
)

type EntitlementAccess string

const (
	EntitlementNone        EntitlementAccess = "none"
	EntitlementPreviewOnly EntitlementAccess = "preview_only"
	EntitlementFullAccess  EntitlementAccess = "full_access"
)

type SessionStatus string

const (
	SessionStatusActive    SessionStatus = "active"
	SessionStatusExpired   SessionStatus = "expired"
	SessionStatusCompleted SessionStatus = "completed"
)

type DeviceContext struct {
	Platform    string `json:"platform"`
	AppVersion  string `json:"appVersion"`
	NetworkType string `json:"networkType"`
}

type CreatePlaybackSessionRequest struct {
	EpisodeID        string        `json:"episodeId" binding:"required"`
	SourcePage       string        `json:"sourcePage" binding:"required"`
	AutoNext         bool          `json:"autoNext"`
	PreferredQuality *string       `json:"preferredQuality,omitempty"`
	DeviceContext    DeviceContext `json:"deviceContext"`
}

type HeaderKV struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type NextEpisodeHint struct {
	EpisodeID string `json:"episodeId"`
	Title     string `json:"title"`
}

type AnalyticsContext struct {
	SessionID    string `json:"sessionId"`
	DramaID      string `json:"dramaId"`
	EpisodeID    string `json:"episodeId"`
	PlaybackMode string `json:"playbackMode"`
	SourcePage   string `json:"sourcePage"`
	SignerMode   string `json:"signerMode"`
}

type PlaybackDescriptor struct {
	SessionID                string           `json:"sessionId"`
	PlaybackMode             PlaybackMode     `json:"playbackMode"`
	MediaURL                 string           `json:"mediaUrl"`
	RequestHeaders           []HeaderKV       `json:"requestHeaders"`
	ExpiresAt                string           `json:"expiresAt"`
	PreviewSeconds           int              `json:"previewSeconds"`
	HeartbeatIntervalSeconds int              `json:"heartbeatIntervalSeconds"`
	RefreshAfterSeconds      int              `json:"refreshAfterSeconds"`
	NextEpisodeHint          *NextEpisodeHint `json:"nextEpisodeHint,omitempty"`
	AnalyticsContext         AnalyticsContext `json:"analyticsContext"`
}

type SessionDetail struct {
	SessionID     string        `json:"sessionId"`
	UserID        string        `json:"userId"`
	DramaID       string        `json:"dramaId"`
	EpisodeID     string        `json:"episodeId"`
	PlaybackMode  PlaybackMode  `json:"playbackMode"`
	SessionStatus SessionStatus `json:"sessionStatus"`
	MediaPath     string        `json:"mediaPath"`
	IssuedAt      string        `json:"issuedAt"`
	ExpiresAt     string        `json:"expiresAt"`
	CompletedAt   *string       `json:"completedAt,omitempty"`
}

type HeartbeatRequest struct {
	PositionSeconds         int     `json:"positionSeconds" binding:"required,min=0"`
	BufferedPositionSeconds int     `json:"bufferedPositionSeconds" binding:"required,min=0"`
	IsPlaying               bool    `json:"isPlaying"`
	NetworkType             *string `json:"networkType,omitempty"`
	PlayerState             string  `json:"playerState" binding:"required"`
	ClientTime              string  `json:"clientTime" binding:"required"`
}

type HeartbeatResponse struct {
	KeepAlive               bool    `json:"keepAlive"`
	ExpiresAt               string  `json:"expiresAt"`
	ShouldRefreshURL        bool    `json:"shouldRefreshUrl"`
	PreviewRemainingSeconds *int    `json:"previewRemainingSeconds,omitempty"`
	NextAction              *string `json:"nextAction,omitempty"`
}

type CompleteRequest struct {
	FinalPositionSeconds int    `json:"finalPositionSeconds" binding:"required,min=0"`
	Completed            bool   `json:"completed"`
	WatchedSeconds       int    `json:"watchedSeconds" binding:"required,min=0"`
	ClientTime           string `json:"clientTime" binding:"required"`
}

type CompleteResponse struct {
	Accepted      bool    `json:"accepted"`
	NextEpisodeID *string `json:"nextEpisodeId,omitempty"`
}

type SessionRefreshResponse struct {
	Descriptor PlaybackDescriptor `json:"descriptor"`
}

type EpisodeRecord struct {
	DramaID          string
	EpisodeID        string
	Title            string
	EpisodeNo        int
	DurationSeconds  int
	PreviewSeconds   int
	IsPremium        bool
	PublishStatus    string
	MediaPath        string
	NextEpisodeID    *string
	NextEpisodeTitle *string
}

type DevEntitlementGrantRequest struct {
	UserID          string  `json:"userId" binding:"required"`
	EntitlementType string  `json:"entitlementType" binding:"required"`
	DramaID         *string `json:"dramaId,omitempty"`
	EpisodeID       *string `json:"episodeId,omitempty"`
	ExpiresAt       *string `json:"expiresAt,omitempty"`
}

type DevEntitlementRevokeRequest struct {
	UserID string `json:"userId" binding:"required"`
}

type SessionCacheRecord struct {
	SessionID                string       `json:"sessionId"`
	UserID                   string       `json:"userId"`
	DramaID                  string       `json:"dramaId"`
	EpisodeID                string       `json:"episodeId"`
	PlaybackMode             PlaybackMode `json:"playbackMode"`
	MediaPath                string       `json:"mediaPath"`
	ExpiresAt                string       `json:"expiresAt"`
	PreviewSeconds           int          `json:"previewSeconds"`
	HeartbeatIntervalSeconds int          `json:"heartbeatIntervalSeconds"`
	RefreshAfterSeconds      int          `json:"refreshAfterSeconds"`
	SourcePage               string       `json:"sourcePage"`
	SignerMode               string       `json:"signerMode"`
	NextEpisodeID            *string      `json:"nextEpisodeId,omitempty"`
	NextEpisodeTitle         *string      `json:"nextEpisodeTitle,omitempty"`
}
