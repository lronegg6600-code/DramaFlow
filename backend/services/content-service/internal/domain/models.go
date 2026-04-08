package domain

type Drama struct {
	ID               string   `json:"id"`
	Title            string   `json:"title"`
	ShortDescription string   `json:"shortDescription"`
	LongDescription  string   `json:"longDescription"`
	PosterURL        string   `json:"posterUrl"`
	CoverURL         string   `json:"coverUrl"`
	Tags             []string `json:"tags"`
	Region           string   `json:"region"`
	Language         string   `json:"language"`
	PublishStatus    string   `json:"publishStatus"`
	IsFeatured       bool     `json:"isFeatured"`
}

type Episode struct {
	ID                   string `json:"id"`
	DramaID              string `json:"dramaId"`
	EpisodeNo            int    `json:"episodeNo"`
	Title                string `json:"title"`
	Description          string `json:"description"`
	DurationSeconds      int    `json:"durationSeconds"`
	PreviewSeconds       int    `json:"previewSeconds"`
	IsPremium            bool   `json:"isPremium"`
	StreamKeyPlaceholder string `json:"streamKeyPlaceholder"`
	PublishStatus        string `json:"publishStatus"`
	SortOrder            int    `json:"sortOrder"`
}
