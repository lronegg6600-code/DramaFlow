FROM golang:1.25.3-alpine AS builder

ENV GOPROXY=https://goproxy.cn,direct
RUN apk add --no-cache git

WORKDIR /src
COPY backend/shared /src/shared
COPY backend/services/content-service /src/services/content-service
WORKDIR /src/services/content-service
RUN go build -o /out/content-service ./cmd/server

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /out/content-service /app/content-service
EXPOSE 8082
CMD ["/app/content-service"]
