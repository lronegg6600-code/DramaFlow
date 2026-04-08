package http

import (
	"net/http"

	"dramaflow/backend/shared/response"
	"dramaflow/backend/services/content-service/internal/service"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service service.Service
}

func New(service service.Service) Handler {
	return Handler{service: service}
}

func (h Handler) RegisterRoutes(router *gin.RouterGroup) {
	router.GET("/dramas", h.listDramas)
	router.GET("/dramas/:dramaId", h.getDrama)
	router.GET("/dramas/:dramaId/episodes", h.listEpisodes)
	router.GET("/episodes/:episodeId", h.getEpisode)
}

func (h Handler) listDramas(c *gin.Context) {
	data, err := h.service.ListDramas(c.Request.Context())
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) getDrama(c *gin.Context) {
	data, err := h.service.GetDrama(c.Request.Context(), c.Param("dramaId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) listEpisodes(c *gin.Context) {
	data, err := h.service.ListEpisodes(c.Request.Context(), c.Param("dramaId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}

func (h Handler) getEpisode(c *gin.Context) {
	data, err := h.service.GetEpisode(c.Request.Context(), c.Param("episodeId"))
	if err != nil {
		response.Fail(c, err)
		return
	}
	response.Success(c, http.StatusOK, data)
}
