import fs from "node:fs";
import path from "node:path";

import { ensureDir, evidenceDir, rootDir, writeJson } from "./_intake_validation_helpers.mjs";

export { writeJson };

export function releaseEvidenceDir() {
  return evidenceDir();
}

export function ghExecutable() {
  const explicit = process.env.GH_EXE;
  if (explicit && fs.existsSync(explicit)) return explicit;
  const bundled = "Z:\\Tools\\GitHubCLI\\bin\\gh.exe";
  if (fs.existsSync(bundled)) return bundled;
  return "gh";
}

export function docsDir() {
  return path.join(rootDir(), "docs");
}

export function templatesDir() {
  return path.join(docsDir(), "templates");
}

export function loadOwnerMatrix() {
  const file = path.join(releaseEvidenceDir(), "platform-blocker-owner-matrix.json");
  return JSON.parse(fs.readFileSync(file, "utf8")).items;
}

export function readJsonIfExists(file, fallback) {
  if (!fs.existsSync(file)) return fallback;
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

export function writeMarkdown(file, content) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, content, "utf8");
}

export function ownerTemplate(owner) {
  if (owner === "repo admin") return "repo-admin-blocker-request";
  if (owner === "release manager") return "release-manager-request";
  if (owner === "ops") return "ops-blocker-request";
  return "platform-blocker-request";
}

export function slugify(value) {
  return String(value)
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function blockerCategoryLabel(blockerCategory) {
  const map = {
    "Repository identity": "blocker:repo-identity",
    "Artifact identity": "blocker:artifact-identity",
    "Cluster access": "blocker:cluster-access",
    "Deployment tooling": "blocker:deploy-tooling",
    "GitHub release control": "blocker:github-env",
    "Secrets / config": "blocker:secrets"
  };
  return map[blockerCategory] || `blocker:${slugify(blockerCategory)}`;
}

export function milestoneForBlocker(blocker) {
  return blocker.blocks_staging === "yes" ? "staging-unblock" : "production-readiness";
}

export function issueTypeForBlocker(blocker) {
  if (blocker.primary_owner === "repo admin") return "repo_admin";
  if (blocker.primary_owner === "release manager") return "release_manager";
  if (blocker.primary_owner === "ops") return "ops";
  return "platform";
}

export function buildIssueTitle(blocker) {
  return `[Release Blocker][${blocker.blocker_id}] ${blocker.blocker_category} :: ${blocker.required_input}`;
}

export function buildLabels(blocker) {
  const labels = [
    "blocker",
    "blocker:external",
    "blocker:waiting",
    `severity:${String(blocker.severity).toLowerCase()}`,
    blockerCategoryLabel(blocker.blocker_category),
    `owner:${slugify(blocker.primary_owner)}`,
    `provider:${slugify(blocker.provider_role)}`,
    `sla:${slugify(blocker.sla_level)}`
  ];
  if (blocker.blocks_staging === "yes") labels.push("blocker:staging");
  if (blocker.blocks_production === "yes") labels.push("blocker:production");
  return labels;
}

export function buildTicketRecord(blocker) {
  return {
    blocker_id: blocker.blocker_id,
    blocker_category: blocker.blocker_category,
    blocker_title: blocker.blocker_title,
    current_status: blocker.current_status,
    issue_type: issueTypeForBlocker(blocker),
    issue_form: ownerTemplate(blocker.primary_owner),
    issue_title_template: buildIssueTitle(blocker),
    labels: buildLabels(blocker),
    suggested_assignee_role: blocker.primary_owner,
    provider_role: blocker.provider_role,
    due_date_bucket: blocker.due_date_placeholder,
    milestone: milestoneForBlocker(blocker),
    close_condition: "validator returns received_and_verified",
    reopen_condition: "validator returns received_but_invalid after close or verification regresses",
    issue_number: null,
    issue_url: null,
    issue_lifecycle_status: "not_created",
    reminder_state: "pending",
    escalation_state: "not_queued",
    verification_command: blocker.verification_command,
    evidence_output: blocker.evidence_output,
    acceptance_criteria: blocker.acceptance_criteria,
    dependency: blocker.dependency || "",
    notes: blocker.notes,
    required_input: blocker.required_input,
    blocks_staging: blocker.blocks_staging,
    blocks_production: blocker.blocks_production,
    severity: blocker.severity,
    escalation_path: blocker.escalation_path,
    engineering_receiver: blocker.engineering_receiver,
    input_format: blocker.input_format,
    sla_level: blocker.sla_level
  };
}

export function buildIssueBody(ticket) {
  return [
    `# ${ticket.issue_title_template}`,
    "",
    `- blocker_id: \`${ticket.blocker_id}\``,
    `- blocker_category: \`${ticket.blocker_category}\``,
    `- current_status: \`${ticket.current_status}\``,
    `- required_input: \`${ticket.required_input ?? ""}\``,
    `- input_format: \`${ticket.input_format ?? ""}\``,
    `- provider_role: \`${ticket.provider_role ?? ""}\``,
    `- issue_form: \`${ticket.issue_form}\``,
    `- suggested_assignee_role: \`${ticket.suggested_assignee_role}\``,
    `- due_bucket: \`${ticket.due_date_bucket}\``,
    `- milestone: \`${ticket.milestone ?? ""}\``,
    `- verification_command: \`${ticket.verification_command}\``,
    `- evidence_output: \`${ticket.evidence_output}\``,
    "",
    "## Acceptance Criteria",
    "",
    ticket.acceptance_criteria,
    "",
    "## Close Condition",
    "",
    ticket.close_condition,
    "",
    "## Reopen Condition",
    "",
    ticket.reopen_condition,
    "",
    "## Notes",
    "",
    ticket.notes || "None"
  ].join("\n");
}

export function summarizeLifecycleStatus(items) {
  return {
    total: items.length,
    not_created: items.filter((item) => item.issue_lifecycle_status === "not_created").length,
    created_unassigned: items.filter((item) => item.issue_lifecycle_status === "created_unassigned").length,
    assigned: items.filter((item) => item.issue_lifecycle_status === "assigned").length,
    awaiting_reply: items.filter((item) => item.issue_lifecycle_status === "awaiting_reply").length,
    replied_invalid: items.filter((item) => item.issue_lifecycle_status === "replied_invalid").length,
    verified: items.filter((item) => item.issue_lifecycle_status === "verified").length,
    closed: items.filter((item) => item.issue_lifecycle_status === "closed").length,
    escalated: items.filter((item) => item.issue_lifecycle_status === "escalated").length
  };
}

export function writeTicketArtifacts(payload) {
  const summary = payload.summary || summarizeLifecycleStatus(payload.items || []);
  payload.summary = summary;
  writeJson(path.join(releaseEvidenceDir(), "blocker-ticket-map.json"), payload);

  const tableHeader =
    "| blocker_id | issue_type | issue_form | issue_lifecycle_status | owner | milestone | due_bucket |\n| --- | --- | --- | --- | --- | --- | --- |\n";
  const tableBody = payload.items
    .map(
      (item) =>
        `| ${item.blocker_id} | ${item.issue_type} | ${item.issue_form} | ${item.issue_lifecycle_status} | ${item.suggested_assignee_role} | ${item.milestone} | ${item.due_date_bucket} |`
    )
    .join("\n");

  writeMarkdown(
    path.join(releaseEvidenceDir(), "blocker-ticket-map.md"),
    `# Blocker Ticket Map\n\nGenerated at: ${payload.generatedAt}\n\n- total: ${summary.total}\n- not_created: ${summary.not_created}\n- created_unassigned: ${summary.created_unassigned}\n- assigned: ${summary.assigned}\n- awaiting_reply: ${summary.awaiting_reply}\n- replied_invalid: ${summary.replied_invalid}\n- verified: ${summary.verified}\n- closed: ${summary.closed}\n- escalated: ${summary.escalated}\n\n${tableHeader}${tableBody}\n`
  );
}
