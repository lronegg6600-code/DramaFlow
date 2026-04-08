FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /src
COPY backend/shared /src/shared
COPY backend/services/progress-service /src/services/progress-service
WORKDIR /src/services/progress-service
RUN go build -o /out/progress-service ./cmd/server

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /out/progress-service /app/progress-service
EXPOSE 8084
CMD ["/app/progress-service"]
