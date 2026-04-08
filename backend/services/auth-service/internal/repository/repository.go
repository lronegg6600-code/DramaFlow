package repository

import (
	"context"
	"fmt"
	"time"

	"dramaflow/backend/services/auth-service/internal/domain"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) Repository {
	return Repository{pool: pool}
}

func (r Repository) FindOrCreateGuestUser(ctx context.Context, anonymousDeviceID string) (domain.User, error) {
	const query = `
		INSERT INTO users (id, anonymous_device_id, status, created_at, updated_at)
		VALUES ($1, $2, 'active', NOW(), NOW())
		ON CONFLICT (anonymous_device_id) DO UPDATE SET updated_at = NOW()
		RETURNING id, anonymous_device_id, status
	`
	var user domain.User
	err := r.pool.QueryRow(ctx, query, uuid.NewString(), anonymousDeviceID).Scan(
		&user.ID,
		&user.AnonymousDeviceID,
		&user.Status,
	)
	if err != nil {
		return domain.User{}, fmt.Errorf("upsert guest user: %w", err)
	}
	return user, nil
}

func (r Repository) StoreRefreshToken(ctx context.Context, userID string, tokenHash string, expiresAt time.Time) error {
	const query = `
		INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at)
		VALUES ($1, $2, $3, $4, NOW())
	`
	_, err := r.pool.Exec(ctx, query, uuid.NewString(), userID, tokenHash, expiresAt)
	if err != nil {
		return fmt.Errorf("store refresh token: %w", err)
	}
	return nil
}

func (r Repository) FindUserByRefreshToken(ctx context.Context, tokenHash string) (domain.User, error) {
	const query = `
		SELECT u.id, u.anonymous_device_id, u.status
		FROM refresh_tokens rt
		JOIN users u ON u.id = rt.user_id
		WHERE rt.token_hash = $1
		  AND rt.revoked_at IS NULL
		  AND rt.expires_at > NOW()
		ORDER BY rt.created_at DESC
		LIMIT 1
	`
	var user domain.User
	err := r.pool.QueryRow(ctx, query, tokenHash).Scan(&user.ID, &user.AnonymousDeviceID, &user.Status)
	if err != nil {
		return domain.User{}, fmt.Errorf("find refresh token user: %w", err)
	}
	return user, nil
}

func (r Repository) RevokeRefreshToken(ctx context.Context, tokenHash string) error {
	const query = `UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = $1`
	_, err := r.pool.Exec(ctx, query, tokenHash)
	if err != nil {
		return fmt.Errorf("revoke refresh token: %w", err)
	}
	return nil
}

func (r Repository) FindUserByID(ctx context.Context, userID string) (domain.User, error) {
	const query = `SELECT id, anonymous_device_id, status FROM users WHERE id = $1`
	var user domain.User
	err := r.pool.QueryRow(ctx, query, userID).Scan(&user.ID, &user.AnonymousDeviceID, &user.Status)
	if err != nil {
		return domain.User{}, fmt.Errorf("find user by id: %w", err)
	}
	return user, nil
}
