import http from "k6/http";

export const options = {
  scenarios: {
    spike: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "30s", target: 5 },
        { duration: "30s", target: 15 },
        { duration: "30s", target: 1 }
      ]
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"]
  }
};

export default function () {
  http.post(
    `${__ENV.BASE_URL || "http://localhost:8088"}/v1/admin/feed-config/home/publish?region=US&language=zh-CN`,
    null,
    {
      headers: {
        Cookie: __ENV.ADMIN_COOKIE || ""
      }
    }
  );
}
