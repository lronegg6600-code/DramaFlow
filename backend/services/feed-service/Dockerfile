FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /src
COPY backend/shared /src/shared
COPY backend/services/feed-service /src/services/feed-service
WORKDIR /src/services/feed-service
RUN go build -o /out/feed-service ./cmd/server

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /out/feed-service /app/feed-service
EXPOSE 8083
CMD ["/app/feed-service"]
