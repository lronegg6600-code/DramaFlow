package auth

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"time"

	"dramaflow/backend/shared/config"
	"github.com/golang-jwt/jwt/v5"
)

type AccessClaims struct {
	UserID string `json:"userId"`
	jwt.RegisteredClaims
}

type TokenPair struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	ExpiresAt    string `json:"expiresAt"`
}

type TokenManager struct {
	cfg config.JWTConfig
}

func NewTokenManager(cfg config.JWTConfig) TokenManager {
	return TokenManager{cfg: cfg}
}

func (m TokenManager) CreateTokenPair(userID string) (TokenPair, string, error) {
	now := time.Now().UTC()
	expiresAt := now.Add(m.cfg.AccessTokenTTL)
	refreshToken := fmt.Sprintf("%s.%d", userID, now.UnixNano())

	claims := AccessClaims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    m.cfg.Issuer,
			Subject:   userID,
			ExpiresAt: jwt.NewNumericDate(expiresAt),
			IssuedAt:  jwt.NewNumericDate(now),
		},
	}

	signedAccess, err := jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString([]byte(m.cfg.Secret))
	if err != nil {
		return TokenPair{}, "", fmt.Errorf("sign access token: %w", err)
	}

	return TokenPair{
		AccessToken:  signedAccess,
		RefreshToken: refreshToken,
		ExpiresAt:    expiresAt.Format(time.RFC3339),
	}, hashToken(refreshToken), nil
}

func (m TokenManager) ParseAccessToken(tokenString string) (AccessClaims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &AccessClaims{}, func(token *jwt.Token) (any, error) {
		return []byte(m.cfg.Secret), nil
	})
	if err != nil {
		return AccessClaims{}, err
	}

	claims, ok := token.Claims.(*AccessClaims)
	if !ok || !token.Valid {
		return AccessClaims{}, fmt.Errorf("invalid token")
	}

	return *claims, nil
}

func HashRefreshToken(token string) string {
	return hashToken(token)
}

func hashToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}
