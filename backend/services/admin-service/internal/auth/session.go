package auth

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

const SessionCookieName = "dramaflow_admin_session"

func HashPassword(password string) (string, error) {
	value, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}
	return string(value), nil
}

func VerifyPassword(hash string, password string) error {
	return bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
}

func NewSessionToken(adminUserID string) (string, string, time.Time) {
	raw := fmt.Sprintf("%s.%s.%d", adminUserID, uuid.NewString(), time.Now().UTC().UnixNano())
	sum := sha256.Sum256([]byte(raw))
	return raw, hex.EncodeToString(sum[:]), time.Now().UTC().Add(12 * time.Hour)
}
