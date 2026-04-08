package http

import (
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	sharedmiddleware "dramaflow/backend/shared/middleware"
	"dramaflow/backend/shared/response"
	"dramaflow/backend/services/auth-service/internal/domain"
	"dramaflow/backend/services/auth-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
}

func New(service service.Service) Handler {
	return Handler{service: service}
}

func (h Handler) RegisterRoutes(router *gin.RouterGroup) {
	router.POST("/guest-session", h.guestSession)
	router.POST("/refresh", h.refresh)
	router.GET("/me", sharedmiddleware.RequireAuth(h.service.TokenManager()), h.me)
}

func (h Handler) guestSession(c *gin.Context) {
	var req domain.GuestSessionRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Fail(c, apperrors.AppError{
			Code:       apperrors.ErrValidation.Code,
			Message:    apperrors.ErrValidation.Message,
			HTTPStatus: apperrors.ErrValidation.HTTPStatus,
			Details:    map[string]any{"reason": err.Error()},
		})
		return
	}

	session, err := h.service.GuestSession(c.Request.Context(), req)
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusCreated, session)
}

func (h Handler) refresh(c *gin.Context) {
	var req domain.RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Fail(c, apperrors.AppError{
			Code:       apperrors.ErrValidation.Code,
			Message:    apperrors.ErrValidation.Message,
			HTTPStatus: apperrors.ErrValidation.HTTPStatus,
			Details:    map[string]any{"reason": err.Error()},
		})
		return
	}
	session, err := h.service.Refresh(c.Request.Context(), req)
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, session)
}

func (h Handler) me(c *gin.Context) {
	data, err := h.service.Me(c.Request.Context(), sharedmiddleware.UserID(c))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}
