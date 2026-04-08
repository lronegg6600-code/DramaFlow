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

const category = "repo_identity_input_package";
const dir = path.join(intakeRoot(), "received", "repo-identity");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const items = [
  {
    blocker_id: "RI-01",
    blocker_title: "真实 .git checkout",
    current_status: sourceFile ? statusForValue(data.repo_root) : "not_received",
    field: "repo_root",
    notes: sourceFile ? "需要指向真实 git 根目录" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-02",
    blocker_title: "commit SHA",
    current_status: sourceFile ? statusForValue(data.commit_sha) : "not_received",
    field: "commit_sha",
    notes: sourceFile ? "需要 40 位或可解析 SHA" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-03",
    blocker_title: "branch",
    current_status: sourceFile ? statusForValue(data.branch) : "not_received",
    field: "branch",
    notes: sourceFile ? "需要 release 所在分支名" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-04",
    blocker_title: "tag 或 RC version",
    current_status: sourceFile ? statusForValue(data.tag_or_version) : "not_received",
    field: "tag_or_version",
    notes: sourceFile ? "需要发布 tag 或 RC 版本号" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-05",
    blocker_title: "changed files summary",
    current_status: sourceFile ? statusForValue(data.changed_files_summary) : "not_received",
    field: "changed_files_summary",
    notes: sourceFile ? "需要能追溯本次 RC 影响范围" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-06",
    blocker_title: "release notes 链接",
    current_status: sourceFile ? statusForValue(data.release_notes_url) : "not_received",
    field: "release_notes_url",
    notes: sourceFile ? "建议直接给 PR/Release Notes URL" : "未收到 repo identity 输入包"
  },
  {
    blocker_id: "RI-07",
    blocker_title: "仓库管理员确认",
    current_status: sourceFile ? boolStatus(data.repo_admin_confirmed) : "not_received",
    field: "repo_admin_confirmed",
    notes: sourceFile ? "需要 repo admin 确认这是可发版 checkout" : "未收到 repo identity 输入包"
  }
];

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
