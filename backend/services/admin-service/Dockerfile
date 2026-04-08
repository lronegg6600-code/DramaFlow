FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /workspace
COPY backend/shared ./backend/shared
COPY backend/services/admin-service ./backend/services/admin-service
WORKDIR /workspace/backend/services/admin-service
RUN go build -o /out/admin-service ./cmd/server

FROM alpine:3.20
RUN adduser -D -u 10001 dramaflow
USER dramaflow
WORKDIR /app
COPY --from=builder /out/admin-service /app/admin-service
EXPOSE 8088
ENTRYPOINT ["/app/admin-service"]
