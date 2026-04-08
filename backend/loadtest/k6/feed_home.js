import http from "k6/http";

export const options = {
  scenarios: {
    baseline: {
      executor: "constant-vus",
      vus: 10,
      duration: "1m"
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<300"]
  }
};

export default function () {
  http.get(`${__ENV.BASE_URL || "http://localhost:8083"}/v1/feed/home`);
}
