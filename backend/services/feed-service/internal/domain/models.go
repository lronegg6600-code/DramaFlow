package domain

type FeedItem struct {
	DramaID          string   `json:"dramaId"`
	Title            string   `json:"title"`
	ShortDescription string   `json:"shortDescription"`
	PosterURL        string   `json:"posterUrl"`
	CoverURL         string   `json:"coverUrl"`
	Tags             []string `json:"tags"`
	IsFeatured       bool     `json:"isFeatured"`
	IsPremium        bool     `json:"isPremium"`
}

type ContinueWatchingItem struct {
	DramaID          string `json:"dramaId"`
	EpisodeID        string `json:"episodeId"`
	Title            string `json:"title"`
	ProgressPercent  int    `json:"progressPercent"`
	UpdatedAtISO8601 string `json:"updatedAt"`
}

type HomeFeed struct {
	Featured         []FeedItem             `json:"featured"`
	Trending         []FeedItem             `json:"trending"`
	Recommended      []FeedItem             `json:"recommended"`
	ContinueWatching []ContinueWatchingItem `json:"continueWatching"`
}
