import path from "node:path";
import { EVIDENCE_DIR, buildExternalUrlStatuses, getNowIso, rerunMobileGateWithExternalUrls, writeJson } from "./external_staging_url_common.mjs";

const state = await buildExternalUrlStatuses();
const rerun = await rerunMobileGateWithExternalUrls(state.items);
const payload = {
  generatedAt: getNowIso(),
  ...rerun,
};
await writeJson(path.join(EVIDENCE_DIR, "mobile-rerun-after-external-urls.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(rerun.rerunExecuted ? 0 : 1);
