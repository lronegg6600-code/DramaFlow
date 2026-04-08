FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /src
COPY backend/shared /src/shared
COPY backend/services/playback-service /src/services/playback-service
WORKDIR /src/services/playback-service
RUN go build -o /out/playback-service ./cmd/server

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /out/playback-service /app/playback-service
EXPOSE 8085
CMD ["/app/playback-service"]
