FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /src
COPY backend/shared /src/shared
COPY backend/services/auth-service /src/services/auth-service
WORKDIR /src/services/auth-service
RUN go build -o /out/auth-service ./cmd/server

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /out/auth-service /app/auth-service
EXPOSE 8081
CMD ["/app/auth-service"]
