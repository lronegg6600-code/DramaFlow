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
    http_req_duration: ["p(95)<500"]
  }
};

export default function () {
  http.get(`${__ENV.BASE_URL || "http://localhost:8088"}/v1/admin/dramas`, {
    headers: {
      Cookie: __ENV.ADMIN_COOKIE || ""
    }
  });
}
