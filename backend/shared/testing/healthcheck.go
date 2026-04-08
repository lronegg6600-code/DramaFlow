package testing

import (
	"net/http"
	"net/http/httptest"

	"github.com/gin-gonic/gin"
)

func PerformRequest(router *gin.Engine, method string, path string) *httptest.ResponseRecorder {
	req, _ := http.NewRequest(method, path, nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}
