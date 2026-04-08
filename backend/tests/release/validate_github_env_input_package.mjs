import path from "node:path";

import {
  boolStatus,
  intakeRoot,
  listCandidateFiles,
  parseLooseConfig,
  statusForValue,
  validationPayload,
  writeValidationFiles,
  printValidationResult
} from "./_intake_validation_helpers.mjs";

const category = "github_environment_input_package";
const dir = path.join(intakeRoot(), "received", "github-environments");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const items = [
  {
    blocker_id: "GH-01",
    blocker_title: "staging environment",
    current_status: sourceFile ? statusForValue(data.staging_environment) : "not_received",
    field: "staging_environment",
    notes: sourceFile ? "需要真实 environment 名称" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-02",
    blocker_title: "production environment",
    current_status: sourceFile ? statusForValue(data.production_environment) : "not_received",
    field: "production_environment",
    notes: sourceFile ? "需要真实 environment 名称" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-03",
    blocker_title: "required reviewers",
    current_status: sourceFile ? statusForValue(data.required_reviewers) : "not_received",
    field: "required_reviewers",
    notes: sourceFile ? "需要 reviewer 列表" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-04",
    blocker_title: "prevent self-review",
    current_status: sourceFile ? boolStatus(data.prevent_self_review) : "not_received",
    field: "prevent_self_review",
    notes: sourceFile ? "需要确认 self-review 保护" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-05",
    blocker_title: "branch/tag restrictions",
    current_status: sourceFile ? statusForValue(data.deployment_policy) : "not_received",
    field: "deployment_policy",
    notes: sourceFile ? "需要 deployment branches/tags policy" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-06",
    blocker_title: "environment secrets/vars",
    current_status: sourceFile ? statusForValue(data.environment_secrets_manifest) : "not_received",
    field: "environment_secrets_manifest",
    notes: sourceFile ? "需要 secrets/vars 清单或截图说明" : "未收到 GitHub environment 输入包"
  },
  {
    blocker_id: "GH-07",
    blocker_title: "artifact/evidence upload policy",
    current_status: sourceFile ? boolStatus(data.evidence_upload_enabled) : "not_received",
    field: "evidence_upload_enabled",
    notes: sourceFile ? "需要确认 workflow 能上传 evidence artifact" : "未收到 GitHub environment 输入包"
  }
];

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
