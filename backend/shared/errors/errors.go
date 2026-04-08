package errors

import "net/http"

type AppError struct {
	Code       string         `json:"code"`
	Message    string         `json:"message"`
	Details    map[string]any `json:"details,omitempty"`
	HTTPStatus int            `json:"-"`
}

func (e AppError) Error() string {
	return e.Message
}

func New(httpStatus int, code string, message string) AppError {
	return AppError{
		Code:       code,
		Message:    message,
		HTTPStatus: httpStatus,
	}
}

var (
	ErrUnauthorized = New(http.StatusUnauthorized, "auth.unauthorized", "Authorization is required.")
	ErrForbidden    = New(http.StatusForbidden, "auth.forbidden", "You are not allowed to access this resource.")
	ErrNotFound     = New(http.StatusNotFound, "resource.not_found", "The requested resource was not found.")
	ErrValidation   = New(http.StatusBadRequest, "request.invalid", "The request body or parameters are invalid.")
	ErrInternal     = New(http.StatusInternalServerError, "server.internal", "The server could not complete this request.")
)
