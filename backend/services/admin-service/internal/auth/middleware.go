package auth

import (
	"crypto/sha256"
	"encoding/hex"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	"dramaflow/backend/services/admin-service/internal/domain"
	"dramaflow/backend/services/admin-service/internal/rbac"
	"github.com/gin-gonic/gin"
)

const ContextAdminUserKey = "admin_user"

type SessionResolver interface {
	ResolveSessionHash(c *gin.Context, sessionHash string) (domain.AdminUser, error)
}

func RequireSession(resolver SessionResolver) gin.HandlerFunc {
	return func(c *gin.Context) {
		cookie, err := c.Cookie(SessionCookieName)
		if err != nil || cookie == "" {
			response.Fail(c, apperrors.ErrUnauthorized)
			c.Abort()
			return
		}
		sum := sha256.Sum256([]byte(cookie))
		user, err := resolver.ResolveSessionHash(c, hex.EncodeToString(sum[:]))
		if err != nil {
			response.Fail(c, err)
			c.Abort()
			return
		}
		c.Set(ContextAdminUserKey, user)
		c.Set(sharedmiddleware.ContextAdminUserIDKey, user.ID)
		c.Next()
	}
}

func RequirePermission(permission string) gin.HandlerFunc {
	return func(c *gin.Context) {
		if !rbac.Allowed(CurrentAdminUser(c).Role, permission) {
			response.Fail(c, apperrors.ErrForbidden)
			c.Abort()
			return
		}
		c.Next()
	}
}

func CurrentAdminUser(c *gin.Context) domain.AdminUser {
	value, _ := c.Get(ContextAdminUserKey)
	item, _ := value.(domain.AdminUser)
	return item
}

func SetSessionCookie(c *gin.Context, token string, expiresAt string) {
	c.SetCookie(SessionCookieName, token, 60*60*12, "/", "", false, true)
	c.Header("X-Admin-Session-Expires-At", expiresAt)
}

func ClearSessionCookie(c *gin.Context) {
	c.SetCookie(SessionCookieName, "", -1, "/", "", false, true)
}

func RequestID(c *gin.Context) string {
	value, _ := c.Get(sharedmiddleware.ContextRequestIDKey)
	cast, _ := value.(string)
	return cast
}

func TraceID(c *gin.Context) string {
	value, _ := c.Get(sharedmiddleware.ContextTraceIDKey)
	cast, _ := value.(string)
	return cast
}
