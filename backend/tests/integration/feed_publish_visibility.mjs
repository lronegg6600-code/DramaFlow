import { adminLogin, base, ensureRun, request } from "./_helpers.mjs";

ensureRun("feed_publish_visibility");

async function main() {
  const { cookie } = await adminLogin();
  const version = Date.now();

  await request(`${base}:8088/v1/admin/feed-config/home`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      cookie
    },
    body: JSON.stringify({
      region: "US",
      language: "en",
      draftPayload: {
        featured: [{ dramaId: "df-neon-vows", slotId: `release-${version}` }],
        trending: [],
        recommended: [],
        banners: [],
        status: "draft"
      }
    })
  });

  await request(`${base}:8088/v1/admin/feed-config/home/publish?region=US&language=en`, {
    method: "POST",
    headers: { cookie }
  });

  const feed = await request(`${base}:8083/v1/feed/home?region=US&language=en`);
  const text = JSON.stringify(feed);
  if (!text.includes("df-neon-vows")) {
    throw new Error("published feed payload not visible downstream");
  }

  console.log("[integration] feed publish -> downstream visibility flow passed");
}

main().catch((error) => {
  console.error(`[integration] feed_publish_visibility failed: ${error.message}`);
  process.exit(1);
});
