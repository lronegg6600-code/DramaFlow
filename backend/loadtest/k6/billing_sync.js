import http from "k6/http";

export const options = {
  scenarios: {
    baseline: {
      executor: "constant-vus",
      vus: 5,
      duration: "1m"
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"]
  }
};

export default function () {
  http.post(
    `${__ENV.BASE_URL || "http://localhost:8087"}/v1/billing/google-play/purchases:sync`,
    JSON.stringify({
      purchaseToken: __ENV.PURCHASE_TOKEN || "loadtest-user:purchase-token",
      productId: "premium_access",
      packageName: "com.dramaflow.app",
      source: "loadtest"
    }),
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${__ENV.ACCESS_TOKEN || ""}`
      }
    }
  );
}
