package response

import (
	"net/http"

	apperrors "dramaflow/backend/shared/errors"
	"github.com/gin-gonic/gin"
)

type Envelope struct {
	Data  any           `json:"data,omitempty"`
	Error *ErrorPayload `json:"error,omitempty"`
	Meta  any           `json:"meta,omitempty"`
}

type ErrorPayload struct {
	Code    string         `json:"code"`
	Message string         `json:"message"`
	Details map[string]any `json:"details,omitempty"`
}

func Success(c *gin.Context, status int, data any) {
	c.JSON(status, Envelope{Data: data})
}

func SuccessWithMeta(c *gin.Context, status int, data any, meta any) {
	c.JSON(status, Envelope{Data: data, Meta: meta})
}

func Fail(c *gin.Context, err error) {
	switch typed := err.(type) {
	case apperrors.AppError:
		c.Set("error_code", typed.Code)
		c.JSON(typed.HTTPStatus, Envelope{
			Error: &ErrorPayload{Code: typed.Code, Message: typed.Message, Details: typed.Details},
		})
	default:
		c.Set("error_code", apperrors.ErrInternal.Code)
		c.JSON(http.StatusInternalServerError, Envelope{
			Error: &ErrorPayload{Code: apperrors.ErrInternal.Code, Message: apperrors.ErrInternal.Message},
		})
	}
}
