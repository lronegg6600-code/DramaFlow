import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const evidenceDir = path.join(root, "release-evidence");

const stagingStatus = JSON.parse(readFileSync(path.join(evidenceDir, "staging-real-external-url-status-summary.json"), "utf8"));
const replyLog = JSON.parse(readFileSync(path.join(evidenceDir, "staging-real-external-url-reply-log.json"), "utf8"));

const payload = {
  generatedAt: new Date().toISOString(),
  localLine: {
    closed: true,
    completed: [
      "local backend 7 services healthy",
      "Android physical-device local smoke passed",
      "entitlements=null dual-sided fix landed",
      "live entitlement-service runtime replaced",
      "live contract probe now returns entitlements=[]",
      "post-cutover minimal device regression passed without crash or decode error",
    ],
  },
  stagingLine: {
    readyToResume: true,
    completed: [],
    remaining: [
      "obtain 7 real resolvable Android-accessible external staging URLs",
      "validate DNS / format / reachability / path on all 7 URLs",
      "export verified Android staging env",
      "rerun Android x backend staging integration gate",
    ],
    currentCoreBlocker: "platform has not provided the 7 real external staging URLs",
    issueNumber: replyLog.issueNumber,
    platformReplyCount: replyLog.platformReplyCount,
    currentState: stagingStatus.currentState,
    urlStatusSummary: stagingStatus.statuses,
  },
  nextShortestPath: "resume issue #42 external staging URL acceptance and verification workflow",
};

writeFileSync(path.join(evidenceDir, "staging-return-handoff.json"), `${JSON.stringify(payload, null, 2)}\n`);
console.log(JSON.stringify(payload, null, 2));
