package domain

type User struct {
	ID                string `json:"id"`
	AnonymousDeviceID string `json:"anonymousDeviceId"`
	Status            string `json:"status"`
}

type UserSummary struct {
	ID     string `json:"id"`
	Status string `json:"status"`
}

type SessionResponse struct {
	AccessToken  string      `json:"accessToken"`
	RefreshToken string      `json:"refreshToken"`
	ExpiresAt    string      `json:"expiresAt"`
	User         UserSummary `json:"user"`
}

type GuestSessionRequest struct {
	AnonymousDeviceID string `json:"anonymousDeviceId" binding:"required"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refreshToken" binding:"required"`
}
