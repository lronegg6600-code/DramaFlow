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
    http_req_duration: ["p(95)<400"]
  }
};

export default function () {
  http.post(
    `${__ENV.BASE_URL || "http://localhost:8085"}/v1/playback/sessions`,
    JSON.stringify({
      episodeId: __ENV.EPISODE_ID || "episode-seed-1",
      sourcePage: "loadtest",
      deviceContext: { platform: "android", appVersion: "1.0.0", networkType: "wifi" }
    }),
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${__ENV.ACCESS_TOKEN || ""}`
      }
    }
  );
}
