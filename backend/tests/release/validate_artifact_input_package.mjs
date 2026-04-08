import path from "node:path";

import {
  intakeRoot,
  listCandidateFiles,
  parseLooseConfig,
  statusForValue,
  validationPayload,
  writeValidationFiles,
  printValidationResult
} from "./_intake_validation_helpers.mjs";

const category = "artifact_identity_input_package";
const dir = path.join(intakeRoot(), "received", "artifact-identity");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const items = [
  {
    blocker_id: "AI-01",
    blocker_title: "docker image tag",
    current_status: sourceFile ? statusForValue(data.image_tag) : "not_received",
    field: "image_tag",
    notes: sourceFile ? "需要 RC 对应镜像 tag" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-02",
    blocker_title: "docker image digest",
    current_status: sourceFile ? statusForValue(data.image_digest) : "not_received",
    field: "image_digest",
    notes: sourceFile ? "需要不可变 digest" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-03",
    blocker_title: "admin web build artifact id",
    current_status: sourceFile ? statusForValue(data.admin_build_artifact_id) : "not_received",
    field: "admin_build_artifact_id",
    notes: sourceFile ? "需要能回查 admin build 产物" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-04",
    blocker_title: "workflow run id",
    current_status: sourceFile ? statusForValue(data.workflow_run_id) : "not_received",
    field: "workflow_run_id",
    notes: sourceFile ? "需要 GitHub Actions run id" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-05",
    blocker_title: "workflow sha",
    current_status: sourceFile ? statusForValue(data.workflow_sha) : "not_received",
    field: "workflow_sha",
    notes: sourceFile ? "需要工作流绑定的提交 SHA" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-06",
    blocker_title: "registry identity",
    current_status: sourceFile ? statusForValue(data.registry) : "not_received",
    field: "registry",
    notes: sourceFile ? "需要镜像所在 registry" : "未收到 artifact identity 输入包"
  },
  {
    blocker_id: "AI-07",
    blocker_title: "artifact provenance 链接",
    current_status: sourceFile ? statusForValue(data.provenance_url) : "not_received",
    field: "provenance_url",
    notes: sourceFile ? "需要 provenance 或 SBOM 入口" : "未收到 artifact identity 输入包"
  }
];

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
