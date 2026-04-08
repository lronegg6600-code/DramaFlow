package queue

const (
	TopicWatchProgressUpdated = "user.watch_progress.updated"
	TopicWatchHistoryCreated  = "user.watch_history.created"
	TopicDramaUpdated         = "content.drama.updated"
	TopicEntitlementChanged   = "billing.entitlement.changed"
)

type Producer interface {
	Publish(topic string, payload []byte) error
}

type Consumer interface {
	Consume(topic string, handler func(payload []byte) error) error
}
