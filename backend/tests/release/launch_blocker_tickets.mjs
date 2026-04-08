import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import {
  buildIssueBody,
  docsDir,
  ghExecutable,
  readJsonIfExists,
  releaseEvidenceDir,
  summarizeLifecycleStatus,
  templatesDir,
  writeJson,
  writeMarkdown,
  writeTicketArtifacts
} from "./_blocker_ticket_helpers.mjs";

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

function loadTemplate(name, fallback) {
  const file = path.join(templatesDir(), name);
  if (!fs.existsSync(file)) return fallback;
  return fs.readFileSync(file, "utf8");
}

function bodyFromTemplate(ticket) {
  const templateByType = {
    platform: "platform-ticket-body.md",
    repo_admin: "repo-admin-ticket-body.md",
    ops: "ops-ticket-body.md",
    release_manager: "release-manager-ticket-body.md"
  };
  const fallback = buildIssueBody(ticket);
  const template = loadTemplate(templateByType[ticket.issue_type], fallback);
  const rendered = template
    .replaceAll("{{BLOCKER_ID}}", ticket.blocker_id)
    .replaceAll("{{BLOCKER_TITLE}}", ticket.blocker_title)
    .replaceAll("{{BLOCKER_CATEGORY}}", ticket.blocker_category)
    .replaceAll("{{REQUIRED_INPUT}}", ticket.required_input ?? "")
    .replaceAll("{{INPUT_FORMAT}}", ticket.input_format ?? "")
    .replaceAll("{{DUE_BUCKET}}", ticket.due_date_bucket ?? "")
    .replaceAll("{{PROVIDER_ROLE}}", ticket.provider_role ?? "")
    .replaceAll("{{ASSIGNEE_ROLE}}", ticket.suggested_assignee_role ?? "")
    .replaceAll("{{VERIFICATION_COMMAND}}", ticket.verification_command ?? "")
    .replaceAll("{{EVIDENCE_OUTPUT}}", ticket.evidence_output ?? "")
    .replaceAll("{{ACCEPTANCE_CRITERIA}}", ticket.acceptance_criteria ?? "")
    .replaceAll("{{CLOSE_CONDITION}}", ticket.close_condition ?? "")
    .replaceAll("{{REOPEN_CONDITION}}", ticket.reopen_condition ?? "")
    .replaceAll("{{NOTES}}", ticket.notes ?? "");

  if (rendered.includes("blocker_id:")) {
    return rendered;
  }

  return `${rendered}\n\n---\n\n${fallback}`;
}

const evidenceDir = releaseEvidenceDir();
const ticketMapFile = path.join(evidenceDir, "blocker-ticket-map.json");
const ticketMap = readJsonIfExists(ticketMapFile, { items: [] });
const launchMode = String(process.env.DRAMAFLOW_LAUNCH_MODE || "dry-run").toLowerCase();
const gh = ghExecutable();
const ghAvailable = fs.existsSync(gh) || commandExists("gh");
const tokenAvailable = Boolean(process.env.GITHUB_TOKEN || process.env.GH_TOKEN) || hasGhAuth(gh);
const repoAvailable = Boolean(process.env.GITHUB_REPOSITORY);
const launchDir = path.join(evidenceDir, "blocker-issue-launch-markdown");
fs.mkdirSync(launchDir, { recursive: true });

const items = ticketMap.items.map((ticket) => {
  const body = bodyFromTemplate(ticket);
  const launchRecord = {
    blocker_id: ticket.blocker_id,
    title: ticket.issue_title_template,
    body,
    labels: ticket.labels,
    milestone: ticket.milestone,
    assignee_role: ticket.suggested_assignee_role,
    provider_role: ticket.provider_role,
    due_bucket: ticket.due_date_bucket,
    close_condition: ticket.close_condition,
    reopen_condition: ticket.reopen_condition,
    issue_type: ticket.issue_type,
    issue_form: ticket.issue_form,
    launch_state: "launch_bundle_ready",
    issue_number: ticket.issue_number,
    issue_url: ticket.issue_url
  };
  writeMarkdown(path.join(launchDir, `${ticket.blocker_id}.md`), body);
  return launchRecord;
});

let createdCount = 0;
const failures = [];

if (launchMode === "real" && ghAvailable && tokenAvailable && repoAvailable) {
  for (const launchItem of items) {
    try {
      const output = execFileSync(
        gh,
        [
          "issue",
          "create",
          "--repo",
          process.env.GITHUB_REPOSITORY,
          "--title",
          launchItem.title,
          "--body",
          launchItem.body,
          "--milestone",
          launchItem.milestone,
          "--label",
          launchItem.labels.join(",")
        ],
        {
          encoding: "utf8",
          env: { ...process.env, HTTP_PROXY: "", HTTPS_PROXY: "", ALL_PROXY: "" }
        }
      ).trim();
      const matchedIssue = output.match(/\/issues\/(\d+)/);
      if (matchedIssue) {
        launchItem.issue_number = Number(matchedIssue[1]);
        launchItem.issue_url = output;
        launchItem.launch_state = "launch_completed";
        createdCount += 1;
        const ticket = ticketMap.items.find((item) => item.blocker_id === launchItem.blocker_id);
        if (ticket) {
          ticket.issue_number = launchItem.issue_number;
          ticket.issue_url = launchItem.issue_url;
          ticket.issue_lifecycle_status = "created_unassigned";
        }
      } else {
        launchItem.launch_state = "launch_failed";
        failures.push({ blocker_id: launchItem.blocker_id, reason: "gh output missing issue number", output });
      }
    } catch (error) {
      launchItem.launch_state = "launch_failed";
      failures.push({ blocker_id: launchItem.blocker_id, reason: String(error.message || error) });
    }
  }
}

const providerDistribution = {};
const milestoneDistribution = {};
for (const item of items) {
  providerDistribution[item.provider_role] = (providerDistribution[item.provider_role] || 0) + 1;
  milestoneDistribution[item.milestone] = (milestoneDistribution[item.milestone] || 0) + 1;
}

const launchState =
  createdCount === 0
    ? "launch_bundle_ready"
    : createdCount === items.length
      ? "launch_completed"
      : "launch_partially_completed";

const summary = {
  generatedAt: new Date().toISOString(),
  mode: launchMode,
  gh_available: ghAvailable,
  gh_executable: gh,
  token_available: tokenAvailable,
  repo_available: repoAvailable,
  total_blockers: items.length,
  staging_blockers: ticketMap.items.filter((item) => item.blocks_staging === "yes").length,
  production_only_blockers: ticketMap.items.filter((item) => item.blocks_staging !== "yes" && item.blocks_production === "yes").length,
  provider_role_distribution: providerDistribution,
  milestone_distribution: milestoneDistribution,
  created_count: createdCount,
  failed_count: failures.length,
  launch_state: launchState,
  failures
};

ticketMap.generatedAt = summary.generatedAt;
ticketMap.summary = summarizeLifecycleStatus(ticketMap.items);
writeTicketArtifacts(ticketMap);

writeJson(path.join(evidenceDir, "blocker-issue-launch-payloads.json"), {
  generatedAt: summary.generatedAt,
  items
});
writeJson(path.join(evidenceDir, "blocker-issue-launch-summary.json"), summary);

writeMarkdown(
  path.join(evidenceDir, "blocker-launch-report.md"),
  `# Blocker Launch Report

- generated_at: ${summary.generatedAt}
- mode: ${summary.mode}
- launch_state: ${summary.launch_state}
- gh_available: ${summary.gh_available}
- token_available: ${summary.token_available}
- repo_available: ${summary.repo_available}
- total_blockers: ${summary.total_blockers}
- staging_blockers: ${summary.staging_blockers}
- production_only_blockers: ${summary.production_only_blockers}
- created_count: ${summary.created_count}
- failed_count: ${summary.failed_count}

## Provider Distribution

${Object.entries(providerDistribution)
  .map(([key, value]) => `- ${key}: ${value}`)
  .join("\n")}

## Milestone Distribution

${Object.entries(milestoneDistribution)
  .map(([key, value]) => `- ${key}: ${value}`)
  .join("\n")}

## Failure Summary

${failures.length === 0 ? "- none" : failures.map((item) => `- ${item.blocker_id}: ${item.reason}`).join("\n")}
`
);

console.log("[launch_blocker_tickets] pass");
