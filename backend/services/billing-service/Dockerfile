FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /workspace
COPY backend /workspace/backend
WORKDIR /workspace/backend/services/billing-service
RUN go build -o /out/billing-service ./cmd/server

FROM alpine:3.20
RUN adduser -D -u 10001 dramaflow
USER dramaflow
COPY --from=builder /out/billing-service /usr/local/bin/billing-service
EXPOSE 8087
ENTRYPOINT ["/usr/local/bin/billing-service"]
