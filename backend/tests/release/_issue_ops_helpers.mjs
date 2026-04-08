import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import { ghExecutable, readJsonIfExists, releaseEvidenceDir, summarizeLifecycleStatus, writeJson, writeTicketArtifacts } from "./_blocker_ticket_helpers.mjs";

export function issueOpsEnv() {
  return {
    ...process.env,
    HTTP_PROXY: "",
    HTTPS_PROXY: "",
    ALL_PROXY: ""
  };
}

export function canUseGithub() {
  const gh = ghExecutable();
  const repo = process.env.GITHUB_REPOSITORY;
  try {
    execFileSync(gh, ["auth", "status"], { stdio: "ignore", env: issueOpsEnv() });
    return Boolean(repo);
  } catch {
    return false;
  }
}

export function execGh(args) {
  return execFileSync(ghExecutable(), args, { encoding: "utf8", env: issueOpsEnv() }).trim();
}

export function loadTicketMap() {
  return readJsonIfExists(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), { items: [] });
}

export function saveTicketMap(ticketMap) {
  ticketMap.generatedAt = new Date().toISOString();
  ticketMap.summary = summarizeLifecycleStatus(ticketMap.items);
  writeTicketArtifacts(ticketMap);
}

export function appendLog(fileName, entry) {
  const file = path.join(releaseEvidenceDir(), fileName);
  const current = readJsonIfExists(file, { generatedAt: new Date().toISOString(), items: [] });
  current.generatedAt = new Date().toISOString();
  current.items.push(entry);
  writeJson(file, current);
}

export function writeNamedEvidence(fileName, payload) {
  writeJson(path.join(releaseEvidenceDir(), fileName), payload);
}

export function buildReminderBody(item) {
  const impact = item.blocks_staging === "yes" ? "real staging execution" : "production readiness";
  return [
    `Release blocker reminder for \`${item.blocker_id}\`.`,
    "",
    `- required_input: \`${item.required_input}\``,
    `- provider_role: \`${item.provider_role}\``,
    `- due_bucket: \`${item.due_date_bucket}\``,
    `- upload_location: \`platform-intake/received/\``,
    `- verification_command: \`${item.verification_command}\``,
    `- impact_if_missing: \`${impact}\``,
    "",
    "Please upload the requested package and reply on this issue once the files are available."
  ].join("\n");
}

export function buildInvalidReplyBody(item, reason) {
  return [
    `The latest submission for \`${item.blocker_id}\` could not be accepted.`,
    "",
    `- rejection_reason: ${reason}`,
    `- required_input: \`${item.required_input}\``,
    `- verification_command: \`${item.verification_command}\``,
    "",
    "Please update the package in `platform-intake/received/` and reply again on this issue."
  ].join("\n");
}

export function buildVerifiedBody(item) {
  return [
    `The submission for \`${item.blocker_id}\` has been verified.`,
    "",
    `- evidence_output: \`${item.evidence_output}\``,
    `- verification_command: \`${item.verification_command}\``,
    "",
    "Engineering verification is complete. The blocker can move toward closeout."
  ].join("\n");
}

export function buildReopenedBody(item, reason) {
  return [
    `The blocker \`${item.blocker_id}\` has been reopened.`,
    "",
    `- reopen_reason: ${reason}`,
    `- close_condition: ${item.close_condition}`,
    `- reopen_condition: ${item.reopen_condition}`,
    "",
    "Please refresh the input package and continue the issue thread until verification succeeds."
  ].join("\n");
}
