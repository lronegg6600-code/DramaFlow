package cache

import (
	"context"
	"fmt"

	"dramaflow/backend/shared/config"
	"github.com/go-redis/redis/v8"
)

func NewRedisClient(ctx context.Context, cfg config.Config) (*redis.Client, error) {
	client := redis.NewClient(&redis.Options{Addr: cfg.RedisAddr})
	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("ping redis: %w", err)
	}
	return client, nil
}
