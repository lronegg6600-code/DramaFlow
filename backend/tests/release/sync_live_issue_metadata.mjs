import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { ghExecutable, readJsonIfExists, releaseEvidenceDir, summarizeLifecycleStatus, writeJson, writeTicketArtifacts } from "./_blocker_ticket_helpers.mjs";

function commandExists(name) {
  try {
    execFileSync("where.exe", [name], { stdio: "ignore" });
    return true;
  } catch {
    return false;
  }
}

function hasGhAuth(gh) {
  try {
    execFileSync(gh, ["auth", "status"], {
      stdio: "ignore",
      env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
    });
    return true;
  } catch {
    return false;
  }
}

const evidenceDir = releaseEvidenceDir();
const mapFile = path.join(evidenceDir, "blocker-ticket-map.json");
const launchSummaryFile = path.join(evidenceDir, "blocker-issue-launch-summary.json");
const ticketMap = readJsonIfExists(mapFile, { items: [] });
const gh = ghExecutable();
const hasGh = fs.existsSync(gh) || commandExists("gh");
const hasToken = Boolean(process.env.GITHUB_TOKEN || process.env.GH_TOKEN) || hasGhAuth(gh);
const hasRepo = Boolean(process.env.GITHUB_REPOSITORY);
const liveSync = {
  generatedAt: new Date().toISOString(),
  mode: hasGh && hasToken && hasRepo ? "live_sync" : "bundle_only",
  gh_available: hasGh,
  token_available: hasToken,
  repo_available: hasRepo,
  updated: []
};

if (hasGh && hasToken && hasRepo) {
  try {
    const output = execFileSync(
      gh,
      [
        "issue",
        "list",
        "--repo",
        process.env.GITHUB_REPOSITORY,
        "--limit",
        "100",
        "--state",
        "all",
        "--json",
        "number,url,state,assignees"
      ],
      {
        encoding: "utf8",
        env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
      }
    );
    const issues = JSON.parse(output);
    const issueMap = new Map(issues.map((issue) => [issue.number, issue]));

    for (const item of ticketMap.items.filter((entry) => entry.issue_number)) {
      const issue = issueMap.get(item.issue_number);
      if (!issue) {
        liveSync.updated.push({ blocker_id: item.blocker_id, issue_number: item.issue_number, error: "issue not found in list response" });
        continue;
      }
      item.issue_url = issue.url;
      if (issue.state === "CLOSED") {
        item.issue_lifecycle_status = "closed";
      } else if (["awaiting_reply", "replied_invalid", "verified", "escalated"].includes(item.issue_lifecycle_status)) {
        item.issue_lifecycle_status = item.issue_lifecycle_status;
      } else {
        item.issue_lifecycle_status = issue.assignees?.length ? "assigned" : "created_unassigned";
      }
      liveSync.updated.push({ blocker_id: item.blocker_id, issue_number: issue.number, issue_url: issue.url });
    }
  } catch (error) {
    liveSync.updated.push({ scope: "issue_list", error: String(error.message || error) });
  }
}

ticketMap.generatedAt = liveSync.generatedAt;
ticketMap.summary = summarizeLifecycleStatus(ticketMap.items);
writeTicketArtifacts(ticketMap);
writeJson(path.join(evidenceDir, "issue-status-sync.json"), liveSync);
writeJson(path.join(evidenceDir, "live-issue-metadata.json"), {
  generatedAt: liveSync.generatedAt,
  mode: liveSync.mode,
  items: ticketMap.items.map((item) => ({
    blocker_id: item.blocker_id,
    issue_number: item.issue_number,
    issue_url: item.issue_url,
    issue_lifecycle_status: item.issue_lifecycle_status,
    assignee_state: item.issue_lifecycle_status === "created_unassigned" ? "unassigned" : "assigned"
  }))
});

const existingSummary = readJsonIfExists(launchSummaryFile, {});
writeJson(launchSummaryFile, {
  ...existingSummary,
  generatedAt: liveSync.generatedAt,
  mode: liveSync.mode,
  launch_state: existingSummary.launch_state || "launch_not_started",
  summary: ticketMap.summary
});

console.log("[sync_live_issue_metadata] pass");
