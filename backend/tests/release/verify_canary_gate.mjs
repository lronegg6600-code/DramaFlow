const promUrl = process.env.PROM_URL ?? "http://localhost:9090";

async function queryPrometheus(query) {
  const url = new URL("/api/v1/query", promUrl);
  url.searchParams.set("query", query);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`prometheus query failed ${response.status}: ${query}`);
  }
  const payload = await response.json();
  if (payload.status !== "success") {
    throw new Error(`prometheus query unsuccessful: ${query}`);
  }
  return payload.data.result;
}

function firstNumeric(result) {
  if (!Array.isArray(result) || result.length === 0) {
    return 0;
  }
  const value = Number(result[0]?.value?.[1] ?? "0");
  return Number.isFinite(value) ? value : 0;
}

async function main() {
  const firingAlerts = await queryPrometheus('ALERTS{alertstate="firing"}');
  if (firingAlerts.length > 0) {
    throw new Error(`firing alerts present: ${firingAlerts.map((item) => item.metric.alertname).join(", ")}`);
  }

  const serviceDown = await queryPrometheus('sum(up{job=~"admin-service|auth-service|billing-service|content-service|entitlement-service|feed-service|playback-service|progress-service"} == 0)');
  if (firstNumeric(serviceDown) > 0) {
    throw new Error("one or more critical services are down");
  }

  const httpErrors = await queryPrometheus("sum(increase(dramaflow_http_errors_total[1m]))");
  const billingSyncErrors = await queryPrometheus("sum(increase(dramaflow_billing_purchase_sync_error_total[1m]))");
  const playbackCreateErrors = await queryPrometheus("sum(increase(dramaflow_playback_session_create_error_total[1m]))");

  const summaries = {
    httpErrors1m: firstNumeric(httpErrors),
    billingSyncErrors1m: firstNumeric(billingSyncErrors),
    playbackCreateErrors1m: firstNumeric(playbackCreateErrors)
  };

  if (summaries.httpErrors1m > 0 || summaries.billingSyncErrors1m > 0 || summaries.playbackCreateErrors1m > 0) {
    throw new Error(`canary stop condition reached: ${JSON.stringify(summaries)}`);
  }

  console.log(`[canary-gate] pass ${JSON.stringify(summaries)}`);
}

main().catch((error) => {
  console.error(`[canary-gate] fail: ${error.message}`);
  process.exit(1);
});
