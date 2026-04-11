import path from "node:path";
import { EVIDENCE_DIR, getNowIso, readJsonIfExists, writeJson } from "./real_external_url_common.mjs";

const closeout = (await readJsonIfExists(path.join(EVIDENCE_DIR, "staging-candidate-invalid-closeout.json"))) ?? { closeoutCompleted: false, items: [] };
const payload = {
  generatedAt: getNowIso(),
  cycleClosed: closeout.closeoutCompleted,
  previousState: "candidate_received_but_invalid",
  nextState: "real_url_not_received",
  carryForwardBlocker: "Platform must provide resolvable Android-accessible external gateway/domain mapping.",
  candidateCount: closeout.items.length,
};
await writeJson(path.join(EVIDENCE_DIR, "staging-candidate-invalid-closeout.json"), {
  ...closeout,
  cycleClosed: payload.cycleClosed,
  nextState: payload.nextState,
  carryForwardBlocker: payload.carryForwardBlocker,
});
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.cycleClosed ? 0 : 1);
