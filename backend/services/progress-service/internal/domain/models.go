package domain

type ProgressUpsertRequest struct {
	DramaID         string `json:"dramaId" binding:"required"`
	PositionSeconds int    `json:"positionSeconds" binding:"required,min=0"`
	DurationSeconds int    `json:"durationSeconds" binding:"required,min=1"`
	Completed       bool   `json:"completed"`
}

type EpisodeProgress struct {
	UserID          string `json:"userId"`
	DramaID         string `json:"dramaId"`
	EpisodeID       string `json:"episodeId"`
	PositionSeconds int    `json:"positionSeconds"`
	DurationSeconds int    `json:"durationSeconds"`
	Completed       bool   `json:"completed"`
	UpdatedAt       string `json:"updatedAt"`
}

type RecentHistoryItem struct {
	DramaID   string `json:"dramaId"`
	EpisodeID string `json:"episodeId"`
	WatchedAt string `json:"watchedAt"`
}
