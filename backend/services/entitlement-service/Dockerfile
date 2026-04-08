FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /workspace
COPY backend /workspace/backend
WORKDIR /workspace/backend/services/entitlement-service
RUN go build -o /out/entitlement-service ./cmd/server

FROM alpine:3.20
RUN adduser -D -u 10001 dramaflow
USER dramaflow
COPY --from=builder /out/entitlement-service /usr/local/bin/entitlement-service
EXPOSE 8086
ENTRYPOINT ["/usr/local/bin/entitlement-service"]
